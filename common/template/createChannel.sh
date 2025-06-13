#!/usr/bin/env bash
#
# Copyright IBM Corp All Rights Reserved
#
# SPDX-License-Identifier: Apache-2.0
#

# Dieses Skript ist für die Erstellung eines Anwendungs-Kanals und das Hinzufügen
# der Peers zu diesem Kanal verantwortlich. Es wird von network.sh aufgerufen.

# imports
# Wichtig: envVar.sh muss die aktualisierte setGlobals-Funktion enthalten!
. scripts/envVar.sh

# Standard-Parameter für den Kanal
CHANNEL_NAME="$1"
DELAY="$2"
MAX_RETRY="$3"
VERBOSE="$4"
BFT="$5"
: ${CHANNEL_NAME:="mychannel"}
: ${DELAY:="3"}
: ${MAX_RETRY:="5"}
: ${VERBOSE:="false"}
: ${BFT:=0}

: ${CONTAINER_CLI:="docker"}
if command -v "${CONTAINER_CLI}"-compose > /dev/null 2>&1; then
    : "${CONTAINER_CLI_COMPOSE:="${CONTAINER_CLI}-compose"}"
else
    : "${CONTAINER_CLI_COMPOSE:="${CONTAINER_CLI} compose"}"
fi
infoln "Using ${CONTAINER_CLI} and ${CONTAINER_CLI_COMPOSE}"

# Sicherstellen, dass das Verzeichnis für Kanal-Artefakte existiert
if [ ! -d "channel-artifacts" ]; then
  mkdir channel-artifacts
fi

# Erstellt die Kanal-Erstellungs-Transaktionsdatei (.tx)
# Diese Datei wird vom Orderer verwendet, um einen neuen Kanal zu initialisieren.
createChannelTransaction() {
  # FABRIC_CFG_PATH ist bereits in network.sh auf ${ROOTDIR}/configtx gesetzt.
  # Dies stellt sicher, dass configtxgen die richtige configtx.yaml findet.

  which configtxgen
  if [ "$?" -ne 0 ]; then
   fatalln "configtxgen tool not found. Please ensure Fabric binaries are in your PATH."
  fi

  local bft_true=$1
  set -x

  if [ "${bft_true}" -eq 1 ]; then
   configtxgen -profile ChannelUsingBFT -outputCreateChannelTx ./channel-artifacts/"${CHANNEL_NAME}".tx -channelID "${CHANNEL_NAME}"
  else
   configtxgen -profile ChannelUsingRaft -outputCreateChannelTx ./channel-artifacts/"${CHANNEL_NAME}".tx -channelID "${CHANNEL_NAME}"
  fi
  res=$?
  { set +x; } 2>/dev/null
  verifyResult $res "Failed to generate channel configuration transaction '${CHANNEL_NAME}.tx'"
}

# Erstellt den Kanal auf dem Orderer-Dienst mithilfe von osnadmin
createChannel() {
  # Poll in case the raft leader is not set yet
  local rc=1
  local COUNTER=1
  local bft_true=$1
  infoln "Attempting to create channel '${CHANNEL_NAME}' on Orderer service..."

  # Pfade und Umgebungsvariablen für Orderer-Admin für osnadmin
  # TEST_NETWORK_HOME wird von network.sh gesetzt und zeigt auf den test-network/ Ordner
  local ORDERER_ADMIN_TLS_CLIENT_CERT="${TEST_NETWORK_HOME}/organizations/ordererOrganizations/navine.tech/users/Admin@navine.tech/tls/client.crt"
  local ORDERER_ADMIN_TLS_PRIVATE_KEY="${TEST_NETWORK_HOME}/organizations/ordererOrganizations/navine.tech/users/Admin@navine.tech/tls/client.key"

  # Hier den Pfad zur CA.crt des Orderers abrufen (globale Variable aus envVar.sh)
  local ORDERER_CA_ROOT_CERT="${ORDERER_CA}"

  while [ ${rc} -ne 0 -a ${COUNTER} -lt ${MAX_RETRY} ] ; do
   sleep "${DELAY}"
   set -x
   # Aufruf von orderer.sh mit allen notwendigen Parametern
   # orderer.sh ist dafür zuständig, den osnadmin-Befehl auszuführen.
   # Parameter: CHANNEL_NAME, CHANNEL_TX_FILE, ORDERER_HOSTNAME, ORDERER_PORT,
   #            ORDERER_ADMIN_TLS_CLIENT_CERT, ORDERER_ADMIN_TLS_PRIVATE_KEY, ORDERER_TLS_ROOTCERT_FILE
   . scripts/orderer.sh "${CHANNEL_NAME}" \
                        "./channel-artifacts/${CHANNEL_NAME}.tx" \
                        "orderer.navine.tech" \
                        "7050" \
                        "${ORDERER_ADMIN_TLS_CLIENT_CERT}" \
                        "${ORDERER_ADMIN_TLS_PRIVATE_KEY}" \
                        "${ORDERER_CA_ROOT_CERT}" > /dev/null 2>&1
   res=$?
   { set +x; } 2>/dev/null
   let rc=$res
   COUNTER=$(expr ${COUNTER} + 1)
  done
  cat log.txt
  verifyResult ${res} "Channel creation failed"
}

# joinChannel ORG_SHORT_NAME
# Verbindet einen Peer einer Organisation mit dem Kanal
joinChannel() {
  local ORG_SHORT_NAME="$1" # Erwartet Kurznamen wie "de", "fr", "be", "es"

  # Setzt die Umgebungsvariablen für den Peer (aus scripts/envVar.sh)
  # Dazu gehören CORE_PEER_LOCALMSPID, CORE_PEER_TLS_ROOTCERT_FILE, CORE_PEER_MSPCONFIGPATH, CORE_PEER_ADDRESS
  setGlobals "${ORG_SHORT_NAME}"

  local rc=1
  local COUNTER=1
  ## Sometimes Join takes time, hence retry
  while [ ${rc} -ne 0 -a ${COUNTER} -lt ${MAX_RETRY} ] ; do
    sleep "${DELAY}"
    set -x
    # Hier wird der Blockfile-Pfad verwendet, der am Anfang von createChannelTransaction erstellt wurde
    peer channel join -b "${BLOCKFILE}" >&log.txt
    res=$?
    { set +x; } 2>/dev/null
   let rc=$res
   COUNTER=$(expr ${COUNTER} + 1)
  done
  cat log.txt
  verifyResult ${res} "After ${MAX_RETRY} attempts, peer0.${ORG_DOMAIN} has failed to join channel '${CHANNEL_NAME}' "
}

# setAnchorPeer ORG_SHORT_NAME
# Setzt den Anker-Peer für eine Organisation auf dem Kanal
setAnchorPeer() {
  local ORG_SHORT_NAME="$1" # Erwartet Kurznamen wie "de", "fr", "be", "es"
  # Das Skript 'scripts/setAnchorPeer.sh' muss ebenfalls an 4 Orgs angepasst werden
  . scripts/setAnchorPeer.sh "${ORG_SHORT_NAME}" "${CHANNEL_NAME}"
}

## --- Hauptausführung des Skripts ---

# Initialisiere FABRIC_CFG_PATH für configtxgen
# ROOTDIR wird von network.sh gesetzt. configtx liegt direkt unter test-network/
export FABRIC_CFG_PATH=${TEST_NETWORK_HOME}/configtx
if [ ${BFT} -eq 1 ]; then
  # Falls BFT aktiviert ist, ändere den Pfad zur BFT-Konfiguration
  export FABRIC_CFG_PATH=${TEST_NETWORK_HOME}/bft-config
fi

# Dateipfad für den Kanal-Block
BLOCKFILE="./channel-artifacts/${CHANNEL_NAME}.block"

infoln "Generating channel configuration transaction '${CHANNEL_NAME}.tx'"
# Diese Funktion wurde angepasst, um .tx zu generieren
createChannelTransaction "${BFT}"


## Erstelle den Kanal auf dem Orderer
infoln "Creating channel ${CHANNEL_NAME}"
createChannel "${BFT}"
successln "Channel '${CHANNEL_NAME}' created"

## Füge alle die Peers zum Kanal hinzu
infoln "Joining peers to the channel..."
# Schleife durch alle 4 Regulatory Orgs
for ORG_SHORT_NAME in de fr be es; do
  joinChannel "${ORG_SHORT_NAME}"
done

## Setze die Anker-Peers für jede Org im Kanal
infoln "Setting anchor peers for all organizations..."
# Schleife durch alle 4 Regulatory Orgs
for ORG_SHORT_NAME in de fr be es; do
  setAnchorPeer "${ORG_SHORT_NAME}"
done

successln "All peers joined channel '${CHANNEL_NAME}' and anchor peers set."
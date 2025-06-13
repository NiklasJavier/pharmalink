#!/usr/bin/env bash
#
# Copyright IBM Corp. All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#

# Dieses Skript setzt die Anker-Peers für eine Organisation auf einem Kanal.
# Es wird von 'scripts/createChannel.sh' aufgerufen.

# Imports
# Wichtig: 'envVar.sh' muss die aktualisierte setGlobals-Funktion enthalten.
# 'configUpdate.sh' muss die Funktionen 'fetchChannelConfig' und 'createConfigUpdate' bereitstellen.
. scripts/envVar.sh
. scripts/configUpdate.sh # Annahme: configUpdate.sh liegt im selben 'scripts' Ordner

# Die Variable TEST_NETWORK_HOME wird vom aufrufenden Skript (network.sh) gesetzt.
# Sie sollte auf das Wurzelverzeichnis des 'test-network'-Ordners zeigen.
# Beispiel: /home/xn31699/pharmalink/fabric-samples/test-network/

# createAnchorPeerUpdate - Generiert eine Transaktion zum Aktualisieren der Anker-Peers
# Parameter: $1 - Der Kurzname der Organisation (z.B. "de", "fr", "be", "es")
#            $2 - Der Kanalname
createAnchorPeerUpdate() {
  local ORG_SHORT_NAME="$1"
  local CHANNEL_NAME="$2"

  # Setzt die Umgebungsvariablen für die angegebene Organisation
  # (CORE_PEER_LOCALMSPID, CORE_PEER_MSPCONFIGPATH, CORE_PEER_ADDRESS etc.)
  setGlobals "${ORG_SHORT_NAME}"

  infoln "Fetching channel config for channel ${CHANNEL_NAME} for organization ${ORG_SHORT_NAME}"
  # Dateinamen für die Konfigurations-JSONs anpassen, um die MSPID zu verwenden
  local CURRENT_CONFIG_JSON="${TEST_NETWORK_HOME}/channel-artifacts/${CORE_PEER_LOCALMSPID}config.json"
  local MODIFIED_CONFIG_JSON="${TEST_NETWORK_HOME}/channel-artifacts/${CORE_PEER_LOCALMSPID}modified_config.json"
  local ANCHOR_TX="${TEST_NETWORK_HOME}/channel-artifacts/${CORE_PEER_LOCALMSPID}anchors.tx"

  fetchChannelConfig "${ORG_SHORT_NAME}" "${CHANNEL_NAME}" "${CURRENT_CONFIG_JSON}" # Org-Parameter hinzugefügt, falls fetchChannelConfig ihn erwartet.

  infoln "Generating anchor peer update transaction for ${ORG_SHORT_NAME} on channel ${CHANNEL_NAME}"

  # Ermittle Host und Port für den Anker-Peer der jeweiligen Organisation
  local HOST=""
  local PORT=""

  if [ "${ORG_SHORT_NAME}" == "de" ]; then
    HOST="peer0.de.navine.tech"
    PORT=7051 # Muss dem Peer-Port für Deutschland entsprechen
  elif [ "${ORG_SHORT_NAME}" == "fr" ]; then
    HOST="peer0.fr.navine.tech"
    PORT=8051 # Muss dem Peer-Port für Frankreich entsprechen
  elif [ "${ORG_SHORT_NAME}" == "be" ]; then
    HOST="peer0.be.navine.tech"
    PORT=9051 # Muss dem Peer-Port für Belgien entsprechen
  elif [ "${ORG_SHORT_NAME}" == "es" ]; then
    HOST="peer0.es.navine.tech"
    PORT=10051 # Muss dem Peer-Port für Spanien entsprechen
  else
    errorln "Organization '${ORG_SHORT_NAME}' unknown in setAnchorPeer. Exiting."
    exit 1
  fi

  set -x
  # Ändere die Konfiguration, um den Anker-Peer hinzuzufügen
  # ${CORE_PEER_LOCALMSPID} wird von setGlobals gesetzt und ist z.B. RegDeMSP
  jq '.channel_group.groups.Application.groups.'${CORE_PEER_LOCALMSPID}'.values += {"AnchorPeers":{"mod_policy": "Admins","value":{"anchor_peers": [{"host": "'$HOST'","port": '$PORT'}]},"version": "0"}}' "${CURRENT_CONFIG_JSON}" > "${MODIFIED_CONFIG_JSON}"
  res=$?
  { set +x; } 2>/dev/null
  verifyResult $res "Channel configuration update for anchor peer failed, make sure you have jq installed"

  # Berechne ein Konfigurations-Update, basierend auf den Unterschieden
  # zwischen config.json und modified_config.json, schreibe es als Transaktion
  createConfigUpdate "${CHANNEL_NAME}" "${CURRENT_CONFIG_JSON}" "${MODIFIED_CONFIG_JSON}" "${ANCHOR_TX}"
}

# updateAnchorPeer - Sendet die Anker-Peer-Update-Transaktion an den Orderer
# Parameter: $1 - Der Kurzname der Organisation (z.B. "de", "fr", "be", "es")
#            $2 - Der Kanalname
updateAnchorPeer() {
  local ORG_SHORT_NAME="$1"
  local CHANNEL_NAME="$2"

  # Umgebungsvariablen für die Organisation setzen (aus setGlobals)
  setGlobals "${ORG_SHORT_NAME}"

  # Pfad zur generierten Anker-Peer-Transaktionsdatei
  local ANCHOR_TX="${TEST_NETWORK_HOME}/channel-artifacts/${CORE_PEER_LOCALMSPID}anchors.tx"

  set -x
  # Orderer-Endpunkt und TLS-Root-CA-Zertifikat aus globalen Variablen (envVar.sh)
  peer channel update -o "orderer.navine.tech:7050" \
    --ordererTLSHostnameOverride orderer.navine.tech \
    -c "${CHANNEL_NAME}" -f "${ANCHOR_TX}" \
    --tls --cafile "${ORDERER_CA}" >&log.txt
  res=$?
  cat log.txt
  { set +x; } 2>/dev/null
  verifyResult $res "Anchor peer update failed for organization '${ORG_SHORT_NAME}' on channel '${CHANNEL_NAME}'"
  successln "Anchor peer set for org '${CORE_PEER_LOCALMSPID}' on channel '${CHANNEL_NAME}'"
}

# --- Hauptausführung des Skripts ---
# Dieses Skript wird von scripts/createChannel.sh aufgerufen

# Parameter: $1=ORG_SHORT_NAME, $2=CHANNEL_NAME
if [ "$#" -ne 2 ]; then
  errorln "Usage: scripts/setAnchorPeer.sh <ORG_SHORT_NAME> <CHANNEL_NAME>"
  exit 1
fi

# Rufe die Funktion zum Erstellen des Anker-Peer-Updates auf
createAnchorPeerUpdate "$1" "$2"

# Rufe die Funktion zum Senden des Anker-Peer-Updates auf
updateAnchorPeer "$1" "$2"
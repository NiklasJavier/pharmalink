#!/usr/bin/env bash
#
# Copyright IBM Corp All Rights Reserved
#
# SPDX-License-Identifier: Apache-2.0
#

# This is a collection of bash functions used by different scripts

# imports
# test network home var targets to test-network folder
# The ROOTDIR in network.sh typically sets the base.
# Assuming this script is sourced from network.sh, and network.sh sets PWD to ROOTDIR,
# we can rely on PWD for relative paths from test-network/ folder.

# Hier wird der Test-Netzwerk-Home-Pfad definiert. In unserem Fall ist das der Ort,
# an dem das 'network.sh' Skript liegt (typischerweise der 'test-network/' Ordner).
TEST_NETWORK_HOME=${PWD} # Setzt TEST_NETWORK_HOME auf das aktuelle Arbeitsverzeichnis (wo network.sh läuft)
. ${TEST_NETWORK_HOME}/scripts/utils.sh # utils.sh liegt in scripts/

export CORE_PEER_TLS_ENABLED=true

# --- ANGEPASSTE CA-ZERTIFIKATSPFADE FÜR ORDERER UND ALLE 4 PEER-ORGS ---
# Diese Pfade sind relativ zum TEST_NETWORK_HOME (also dem 'test-network/' Ordner)

export ORDERER_CA=${TEST_NETWORK_HOME}/organizations/ordererOrganizations/navine.tech/orderers/orderer.navine.tech/tls/ca.crt # Orderer TLS CA Cert
export PEER0_DE_CA=${TEST_NETWORK_HOME}/organizations/peerOrganizations/de.navine.tech/peers/peer0.de.navine.tech/tls/ca.crt # Deutschland Peer TLS CA Cert
export PEER0_FR_CA=${TEST_NETWORK_HOME}/organizations/peerOrganizations/fr.navine.tech/peers/peer0.fr.navine.tech/tls/ca.crt # Frankreich Peer TLS CA Cert
export PEER0_BE_CA=${TEST_NETWORK_HOME}/organizations/peerOrganizations/be.navine.tech/peers/peer0.be.navine.tech/tls/ca.crt # Belgien Peer TLS CA Cert
export PEER0_ES_CA=${TEST_NETWORK_HOME}/organizations/peerOrganizations/es.navine.tech/peers/peer0.es.navine.tech/tls/ca.crt # Spanien Peer TLS CA Cert

# Set environment variables for the peer org
# Parameter: $1 - Der Kurzname der Organisation (z.B. "de", "fr", "be", "es")
setGlobals() {
  local ORG_NAME_SHORT="$1" # Verwende einen aussagekräftigen Namen für den Parameter

  # Die folgenden Variablen werden in der if/elif-Struktur gesetzt
  local ORG_DOMAIN
  local ORG_MSPID
  local PEER_ADDRESS
  local PEER_TLS_ROOTCERT_FILE
  local ORG_ADMIN_MSPCONFIGPATH # Pfad zum Admin-MSP für diese Org

  infoln "Using organization ${ORG_NAME_SHORT}"

  if [ "${ORG_NAME_SHORT}" == "de" ]; then
    ORG_DOMAIN="de.navine.tech"
    ORG_MSPID="RegDeMSP"
    PEER_ADDRESS="localhost:7051" # Externe Port, wie in docker-compose-peers.yaml
    PEER_TLS_ROOTCERT_FILE=${PEER0_DE_CA}
    ORG_ADMIN_MSPCONFIGPATH=${TEST_NETWORK_HOME}/organizations/peerOrganizations/de.navine.tech/users/Admin@de.navine.tech/msp

  elif [ "${ORG_NAME_SHORT}" == "fr" ]; then
    ORG_DOMAIN="fr.navine.tech"
    ORG_MSPID="RegFrMSP"
    PEER_ADDRESS="localhost:8051" # Eindeutiger Port für FR
    PEER_TLS_ROOTCERT_FILE=${PEER0_FR_CA}
    ORG_ADMIN_MSPCONFIGPATH=${TEST_NETWORK_HOME}/organizations/peerOrganizations/fr.navine.tech/users/Admin@fr.navine.tech/msp

  elif [ "${ORG_NAME_SHORT}" == "be" ]; then
    ORG_DOMAIN="be.navine.tech"
    ORG_MSPID="RegBeMSP"
    PEER_ADDRESS="localhost:9051" # Eindeutiger Port für BE
    PEER_TLS_ROOTCERT_FILE=${PEER0_BE_CA}
    ORG_ADMIN_MSPCONFIGPATH=${TEST_NETWORK_HOME}/organizations/peerOrganizations/be.navine.tech/users/Admin@be.navine.tech/msp

  elif [ "${ORG_NAME_SHORT}" == "es" ]; then
    ORG_DOMAIN="es.navine.tech"
    ORG_MSPID="RegEsMSP"
    PEER_ADDRESS="localhost:10051" # Eindeutiger Port für ES
    PEER_TLS_ROOTCERT_FILE=${PEER0_ES_CA}
    ORG_ADMIN_MSPCONFIGPATH=${TEST_NETWORK_HOME}/organizations/peerOrganizations/es.navine.tech/users/Admin@es.navine.tech/msp

  else
    errorln "Organization '${ORG_NAME_SHORT}' unknown or not supported."
    # Optional: exit 1, wenn ein unbekannter Org-Name kritisch ist
  fi

  # --- Export der Umgebungsvariablen ---
  # Diese werden von 'peer' und 'osnadmin' CLI-Befehlen gelesen
  export CORE_PEER_LOCALMSPID="${ORG_MSPID}"
  export CORE_PEER_TLS_ROOTCERT_FILE="${PEER_TLS_ROOTCERT_FILE}"
  export CORE_PEER_MSPCONFIGPATH="${ORG_ADMIN_MSPCONFIGPATH}" # Pfad zum Admin-MSP
  export CORE_PEER_ADDRESS="${PEER_ADDRESS}"
  export CORE_PEER_ID="peer0.${ORG_DOMAIN}" # Sicherstellen, dass die ID korrekt ist
  # ACHTUNG: CORE_PEER_GOSSIP_BOOTSTRAP und CORE_PEER_GOSSIP_EXTERNALENDPOINT
  # sollten auch gesetzt werden, wenn der Peer sich mit anderen Peers verbinden muss.
  # Diese könnten aus der docker-compose-peers.yaml abgeleitet werden.
  # export CORE_PEER_GOSSIP_BOOTSTRAP="peer0.${ORG_DOMAIN}:${PEER_PORT_INNER_CONTAINER}"
  # export CORE_PEER_GOSSIP_EXTERNALENDPOINT="peer0.${ORG_DOMAIN}:${PEER_PORT_EXTERNAL}"


  if [ "${VERBOSE}" = "true" ]; then
    infoln "--- Exported CORE variables for ${ORG_NAME_SHORT} ---"
    env | grep CORE_PEER
    infoln "----------------------------------------------------"
  fi
}

# parsePeerConnectionParameters $@
# Helper function that sets the peer connection parameters for a chaincode
# operation. This function receives a list of short org names (e.g., "de", "fr")
# and sets up --peerAddresses and --tlsRootCertFiles for each.
parsePeerConnectionParameters() {
  PEER_CONN_PARMS=() # Array für --peerAddresses
  PEERS=""           # String für Peer-Namen
  local orgs=("$@")  # Liste der Organisationen (z.B. "de", "fr")

  ## Setzt die Peer-Adressen und TLS-Zertifikate für jede angegebene Organisation
  for ORG_SHORT_NAME in "${orgs[@]}"; do
    setGlobals "${ORG_SHORT_NAME}" # Setzt die Umgebungsvariablen für die aktuelle Org
    # Annahme: der Peer-Dienst-Name im Docker Compose ist peer0.ORG_DOMAIN
    PEER="peer0.${ORG_DOMAIN}" # ORG_DOMAIN wird von setGlobals gesetzt

    if [ -z "${PEERS}" ]; then
      PEERS="${PEER}"
    else
      PEERS="${PEERS} ${PEER}"
    fi

    # Fügt --peerAddresses hinzu
    PEER_CONN_PARMS+=("--peerAddresses" "${CORE_PEER_ADDRESS}")

    # Fügt --tlsRootCertFiles hinzu
    PEER_CONN_PARMS+=("--tlsRootCertFiles" "${CORE_PEER_TLS_ROOTCERT_FILE}")
  done
}

# verifyResult $? "some message"
verifyResult() {
  if [ $1 -ne 0 ]; then
    fatalln "$2"
  fi
}
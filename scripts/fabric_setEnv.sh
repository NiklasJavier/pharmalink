#!/bin/bash

# =================================================================
# Setzt die Umgebungsvariablen, um als Admin von Org1 zu agieren.
#
# WICHTIG: Dieses Skript muss mit 'source' ausgeführt werden,
# damit die Variablen in der aktuellen Shell gesetzt werden.
#
# Verwendung:
#   source ./fabric_setEnv.sh
#
# =================================================================

# --- Globale Projekt-Einstellungen ---
export CHANNEL_NAME="pharmalink"
export CHAINCODE="pharmalink_chaincode_main"


# --- Basis-Pfade und Endpunkte ---
# Annahme: Das 'pharmalink' Projekt ist in Ihrem Home-Verzeichnis
export BASE_DIR="$HOME/pharmalink"
export ORG_DIR="$BASE_DIR/fabric-samples/test-network/organizations"
export FABRIC_CFG_PATH="$BASE_DIR/fabric-cli/config" # Pfad zur core.yaml des Peer-Clients

# Verwenden Sie 'localhost' für lokale Tests oder Ihre Domain für Remote-Zugriff
ENDPOINT="localhost"
# ENDPOINT="node.d1.navine.tech" # Alternative für Remote-Zugriff


# --- Netzwerk-Adressen ---
export ORDERER_ADDRESS="$ENDPOINT:7050"
export PEER0_ORG1_ADDRESS="$ENDPOINT:7051"
export PEER0_ORG2_ADDRESS="$ENDPOINT:9051"
export CA_ORG1_ADDRESS="$ENDPOINT:7054"


# --- Identitäts-spezifische Einstellungen für Org1 Admin ---
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_ADDRESS="$PEER0_ORG1_ADDRESS"
export CORE_PEER_MSPCONFIGPATH="$ORG_DIR/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp"


# --- TLS Zertifikatspfade ---
export CORE_PEER_TLS_ENABLED=true
export ORDERER_CA="$ORG_DIR/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem"
export PEER0_ORG1_CA="$ORG_DIR/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt"
export PEER0_ORG2_CA="$ORG_DIR/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt"

# Setzt das Standard-TLS-Zertifikat für Peer-Befehle
export CORE_PEER_TLS_ROOTCERT_FILE="$PEER0_ORG1_CA"


# --- Bestätigung für den Benutzer ---
echo "✅ Umgebungsvariablen für den Admin von Org1MSP gesetzt."
echo "   CORE_PEER_ADDRESS = $CORE_PEER_ADDRESS"
#!/bin/bash

# =================================================================
# Zentrale Umgebungsvariablen für das Pharmalink-Netzwerk
# =================================================================

# --- Globale Projekt-Einstellungen ---
export CHANNEL_NAME="pharmalink"
export CHAINCODE="pharmalink_chaincode_main"

# --- Basis-Pfade und Endpunkte ---
export BASE_DIR="$HOME/pharmalink"
export ORG_DIR="$BASE_DIR/fabric-samples/test-network/organizations"
export FABRIC_CFG_PATH="$BASE_DIR/../fabric-cli/config"

ENDPOINT="localhost"

# --- Netzwerk-Adressen ---
export ORDERER_ADDRESS="$ENDPOINT:7050"
export PEER0_ORG1_ADDRESS="$ENDPOINT:7051"
export PEER0_ORG2_ADDRESS="$ENDPOINT:9051"

# --- CA-Server-Adressen ---
export CA_ORG1_ADDRESS="$ENDPOINT:7054"
export CA_ORG2_ADDRESS="$ENDPOINT:8054"

# --- NEU: Vollständige URLs für die CA-Server ---
export CA_ORG1_URL="https://localhost:7054"
export CA_ORG2_URL="https://localhost:8054"


# --- Identitäts-spezifische Einstellungen (Standard: Org1 Admin) ---
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_ADDRESS="$PEER0_ORG1_ADDRESS"
export CORE_PEER_MSPCONFIGPATH="$ORG_DIR/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp"
export CORE_PEER_TLS_ENABLED=true

# --- TLS Zertifikatspfade ---
export ORDERER_CA="$ORG_DIR/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem"
export PEER0_ORG1_CA="$ORG_DIR/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt"
export PEER0_ORG2_CA="$ORG_DIR/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt"
export CORE_PEER_TLS_ROOTCERT_FILE="$PEER0_ORG1_CA"

# --- NEU: Spezifische Pfade für den fabric-ca-client ---
export FABRIC_CA_CLIENT_HOME_ORG1="$BASE_DIR/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/"
export FABRIC_CA_CLIENT_HOME_ORG2="$BASE_DIR/fabric-samples/test-network/organizations/peerOrganizations/org2.example.com/"
export CA_ORG1_TLS_CERTFILE="$ORG_DIR/peerOrganizations/org1.example.com/ca/ca.org1.example.com-cert.pem"
export CA_ORG2_TLS_CERTFILE="$ORG_DIR/peerOrganizations/org2.example.com/ca/ca.org2.example.com-cert.pem"

echo "Basis-Umgebungsvariablen für Pharmalink gesetzt."
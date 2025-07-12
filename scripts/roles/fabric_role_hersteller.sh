#!/bin/bash

source "$HOME/pharmalink/scripts/fabric_setEnv.sh"
export BASE_DIR="$HOME/pharmalink/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com"
export USER_DIR="$BASE_DIR/users/hersteller-user1@org1.example.com"
export CORE_PEER_MSPCONFIGPATH="$USER_DIR/msp"
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_TLS_ENABLED=true

echo "Prüfe Umgebungsvariablen:"
echo "CORE_PEER_MSPCONFIGPATH: $CORE_PEER_MSPCONFIGPATH"
echo "CORE_PEER_LOCALMSPID: $CORE_PEER_LOCALMSPID"
echo "CORE_PEER_TLS_ENABLED: $CORE_PEER_TLS_ENABLED"
echo "CORE_PEER_ADDRESS: $CORE_PEER_ADDRESS"
peer chaincode invoke -o $ORDERER_ADDRESS --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n $CHAINCODE \
--peerAddresses $PEER0_ORG1_ADDRESS --tlsRootCertFiles $PEER0_ORG1_CA \
--peerAddresses $PEER0_ORG2_ADDRESS --tlsRootCertFiles $PEER0_ORG2_CA \
-c '{"function":"initCall","Args":[""]}'
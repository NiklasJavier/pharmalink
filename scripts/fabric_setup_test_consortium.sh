#!/bin/bash

echo "--> Lade Basis-Konfiguration aus fabric_setEnv.sh..."
source "$(dirname "$0")/fabric_setEnv.sh"

FABRIC_CA_CLIENT_PATH="$BASE_DIR/fabric-samples/bin/fabric-ca-client"
NUM_USERS_PER_ROLE=3
AFFILIATIONS=("hersteller" "grosshaendler" "apotheke" "behoerde")


if [ ! -f "$FABRIC_CA_CLIENT_PATH" ]; then
    echo "FEHLER: fabric-ca-client nicht gefunden unter: $FABRIC_CA_CLIENT_PATH"
    exit 1
fi

export FABRIC_CA_CLIENT_HOME=$FABRIC_CA_CLIENT_HOME_ORG1
CA_TLS_CERTFILE=$CA_ORG1_TLS_CERTFILE
CA_SERVER_URL=$CA_ORG1_URL

echo "INFO: Alle Identitäten werden bei der zentralen CA unter $CA_SERVER_URL erstellt."

echo -e "\n1. Einloggen als CA-Admin für Org1..."
$FABRIC_CA_CLIENT_PATH enroll -u ${CA_SERVER_URL/https:\/\//https:\/\/admin:adminpw@} --tls.certfiles $CA_TLS_CERTFILE

ADMIN_CERT_PATH="$ORG_DIR/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/signcerts/cert.pem"

if [ ! -f "$ADMIN_CERT_PATH" ]; then
    echo "FEHLER: Das Admin-Zertifikat für Org1 wurde nicht gefunden unter: $ADMIN_CERT_PATH"
    echo "Stellen Sie sicher, dass die Admin-Identität für Org1 bereits erstellt wurde (z.B. durch registerEnroll.sh)."
    exit 1
fi

for aff in "${AFFILIATIONS[@]}"; do
  echo -e "\n========================================================"
  echo "Verarbeite: '$aff' in Org1"
  echo "========================================================"

  AFFILIATION_PATH="org1.$aff"
  echo "--> Erstelle Affiliation: '$AFFILIATION_PATH'..."
  $FABRIC_CA_CLIENT_PATH affiliation add $AFFILIATION_PATH --force --tls.certfiles $CA_TLS_CERTFILE

  echo "--> Registriere und enrolle $NUM_USERS_PER_ROLE Benutzer für die Rolle '$aff'..."
  for i in $(seq 1 $NUM_USERS_PER_ROLE); do
    USERNAME="$aff-user$i"
    PASSWORD="$aff-pw$i"

    echo -e "\n    -> Verarbeite '$USERNAME'..."

    echo "       -> Registriere '$USERNAME' bei der CA..."
    $FABRIC_CA_CLIENT_PATH register \
      --id.name $USERNAME \
      --id.secret $PASSWORD \
      --id.type client \
      --id.affiliation $AFFILIATION_PATH \
      --id.attrs "role=$aff:ecert" \
      --tls.certfiles $CA_TLS_CERTFILE

    MSP_DIR="$ORG_DIR/peerOrganizations/org1.example.com/users/${USERNAME}@org1.example.com/msp"
    echo "       -> Schließe '$USERNAME' ein (enroll) und speichere Zertifikate nach:"
    echo "          $MSP_DIR"

    $FABRIC_CA_CLIENT_PATH enroll \
      -u https://${USERNAME}:${PASSWORD}@${CA_SERVER_URL#https://} \
      --mspdir "$MSP_DIR" \
      --tls.certfiles $CA_TLS_CERTFILE

    echo "       -> Erstelle 'admincerts' Ordner und kopiere Admin-Zertifikat..."
    mkdir -p "$MSP_DIR/admincerts"

    cp "$ADMIN_CERT_PATH" "$MSP_DIR/admincerts/org1-admin-cert.pem"

  done
done

for file in "$BASE_DIR/scripts/roles/"*.sh; do
  echo "Führe Skript aus: $file"
  bash "$file"
done

echo "Alle Identitäten für das Pharmalink-Konsortium erfolgreich erstellt und eingeschrieben."
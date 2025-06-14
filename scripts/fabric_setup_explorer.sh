#!/bin/bash

# Konfiguration
KEYSTORE_DIR="./fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore"
BASE_PATH="/tmp/crypto/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore"
JSON_FILE="./docker/connection-profile/test-network.json"

# Prüfe, ob jq installiert ist
if ! command -v jq >/dev/null 2>&1; then
    echo "Fehler: 'jq' ist nicht installiert. Installieren Sie es mit 'sudo apt install jq' (Ubuntu) oder 'brew install jq' (macOS)."
    exit 1
fi

# 1. Prüfe, ob das Keystore-Verzeichnis existiert
if [ ! -d "${KEYSTORE_DIR}" ]; then
    echo "Fehler: Keystore-Verzeichnis '${KEYSTORE_DIR}' existiert nicht."
    exit 1
fi

# 2. Prüfe, ob die JSON-Datei existiert
if [ ! -f "${JSON_FILE}" ]; then
    echo "Fehler: JSON-Datei '${JSON_FILE}' existiert nicht."
    exit 1
fi

# 3. Finde die Schlüsseldatei (erwartet genau eine Datei)
KEY_FILE=$(ls "${KEYSTORE_DIR}")
if [ -z "${KEY_FILE}" ]; then
    echo "Fehler: Keine Datei im Keystore-Verzeichnis gefunden."
    exit 1
fi
if [ $(ls "${KEYSTORE_DIR}" | wc -l) -ne 1 ]; then
    echo "Fehler: Mehr als eine Datei im Keystore-Verzeichnis gefunden."
    exit 1
fi

# 4. Erstelle den vollständigen Pfad
KEY_PATH="${BASE_PATH}/${KEY_FILE}"

# 5. Aktualisiere die JSON-Datei
jq ".organizations.Org1MSP.adminPrivateKey.path = \"${KEY_PATH}\"" "${JSON_FILE}" > "${JSON_FILE}.tmp"
if [ $? -ne 0 ]; then
    echo "Fehler beim Aktualisieren der JSON-Datei '${JSON_FILE}'."
    rm -f "${JSON_FILE}.tmp"
    exit 1
fi
mv "${JSON_FILE}.tmp" "${JSON_FILE}"
echo "JSON-Datei '${JSON_FILE}' wurde erfolgreich aktualisiert."

# 6. Ausgabe zur Bestätigung
echo "Neuer Pfad für adminPrivateKey.path:"
jq ".organizations.Org1MSP.adminPrivateKey.path" "${JSON_FILE}"
echo ""
echo "Der Pfad zum privaten Schlüssel wurde auf '${KEY_PATH}' gesetzt."
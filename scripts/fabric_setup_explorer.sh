#!/bin/bash

# Beschreibung: Skript zum Starten/Beenden des Hyperledger Fabric Explorers und Aktualisieren des privaten Schlüssels in der Verbindungskonfiguration.

# Globale Variablen
declare -r SRC_DIR
SRC_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
declare -r KEYSTORE_DIR=".${SRC_DIR}/../fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore"
declare -r BASE_PATH="/tmp/crypto/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore"
declare -r JSON_FILE=".${SRC_DIR}/../docker/explorer/connection-profile/test-network.json"
declare -r DOCKER_COMPOSE_FILE=".${SRC_DIR}/../docker/docker-compose-explorer.yaml"

explorerUp() {
    if ! command -v jq >/dev/null 2>&1; then
        echo "Fehler: 'jq' ist nicht installiert. Installieren Sie es mit:"
        echo "  Ubuntu: sudo apt install jq"
        echo "  macOS: brew install jq"
        exit 1
    fi

    if [[ ! -d "${KEYSTORE_DIR}" ]]; then
        echo "Fehler: Keystore-Verzeichnis '${KEYSTORE_DIR}' existiert nicht."
        exit 1
    fi

    if [[ ! -f "${JSON_FILE}" ]]; then
        echo "Fehler: JSON-Datei '${JSON_FILE}' existiert nicht."
        exit 1
    fi

    local key_file
    key_file=$(ls "${KEYSTORE_DIR}" 2>/dev/null)
    if [[ -z "${key_file}" ]]; then
        echo "Fehler: Keine Datei im Keystore-Verzeichnis '${KEYSTORE_DIR}' gefunden."
        exit 1
    fi
    if [[ $(ls "${KEYSTORE_DIR}" | wc -l) -ne 1 ]]; then
        echo "Fehler: Mehr als eine Datei im Keystore-Verzeichnis gefunden."
        exit 1
    fi

    local key_path="${BASE_PATH}/${key_file}"

    jq ".organizations.Org1MSP.adminPrivateKey.path = \"${key_path}\"" "${JSON_FILE}" > "${JSON_FILE}.tmp"
    if [[ $? -ne 0 ]]; then
        echo "Fehler: Konnte die JSON-Datei '${JSON_FILE}' nicht aktualisieren."
        rm -f "${JSON_FILE}.tmp"
        exit 1
    fi
    mv "${JSON_FILE}.tmp" "${JSON_FILE}"
    echo "JSON-Datei '${JSON_FILE}' wurde erfolgreich aktualisiert."

    echo "Neuer Pfad für adminPrivateKey.path:"
    jq -r ".organizations.Org1MSP.adminPrivateKey.path" "${JSON_FILE}"
    echo ""
    echo "Privater Schlüsselpfad wurde auf '${key_path}' gesetzt."

    echo "Starte Fabric Explorer..."
    docker compose -f "${DOCKER_COMPOSE_FILE}" up -d
    if [[ $? -ne 0 ]]; then
        echo "Fehler: Konnte den Fabric Explorer nicht starten."
        exit 1
    fi
    echo "Fabric Explorer wurde erfolgreich gestartet."
}

explorerDown() {
    echo "Beende Fabric Explorer..."
    docker compose -f "${DOCKER_COMPOSE_FILE}" down -v
    if [[ $? -ne 0 ]]; then
        echo "Fehler: Konnte den Fabric Explorer nicht beenden."
        exit 1
    fi
    echo "Fabric Explorer wurde erfolgreich beendet."
}

printHelp() {
    echo "Verwendung: $0 <Modus>"
    echo "Modi:"
    echo "  up   - Startet den Fabric Explorer und aktualisiert die Verbindungskonfiguration."
    echo "  down - Beendet den Fabric Explorer."
    echo "Beispiel:"
    echo "  $0 up"
    echo "  $0 down"
}

# Argumente parsen
if [[ $# -lt 1 ]]; then
    printHelp
    exit 0
fi

MODE="$1"
shift

# Modus auswerten
case "${MODE}" in
    up)
        explorerUp
        ;;
    down)
        explorerDown
        ;;
    *)
        echo "Fehler: Unbekannter Modus '${MODE}'."
        printHelp
        exit 1
        ;;
esac
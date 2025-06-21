#!/bin/bash

COMPOSE_FILE="../docker/docker-compose-pharmalink.yaml"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

usage() {
    echo "Nutzung: $0 {up|down}"
    echo "  up:   Startet die Pharmalink-Umgebung mit docker-compose."
    echo "  down: Stoppt und bereinigt die Pharmalink-Umgebung."
}

start_services() {
    echo -e "${GREEN}Starte Pharmalink-Dienste...${NC}"
    if [ ! -f "$COMPOSE_FILE" ]; then
        echo -e "${RED}Fehler: Docker-Compose-Datei nicht unter ${COMPOSE_FILE} gefunden.${NC}"
        exit 1
    fi
    docker-compose -f "${COMPOSE_FILE}" up -d
    echo -e "${GREEN}Dienste gestartet. Mit 'docker ps' können Sie den Status prüfen.${NC}"
}

stop_services() {
    echo -e "${GREEN}Stoppe und bereinige Pharmalink-Dienste...${NC}"
    if [ ! -f "$COMPOSE_FILE" ]; then
        echo -e "${RED}Fehler: Docker-Compose-Datei nicht unter ${COMPOSE_FILE} gefunden.${NC}"
        exit 1
    fi
    docker-compose -f "${COMPOSE_FILE}" down
    echo -e "${GREEN}Umgebung wurde bereinigt.${NC}"
}

if [ "$#" -ne 1 ]; then
    usage
    exit 1
fi

ACTION=$1

case "$ACTION" in
    up)
        start_services
        ;;
    down)
        stop_services
        ;;
    *)
        echo -e "${RED}Fehler: Ungültiges Argument '$ACTION'${NC}"
        usage
        exit 1
        ;;
esac

exit 0
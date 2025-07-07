#!/bin/bash

echo "Dieses Skript wird ALLE Docker-Container, -Images, -Volumes und -Netzwerke entfernen."
echo "Dies ist eine destruktive Aktion und kann NICHT rückgängig gemacht werden."
read -p "Bist du dir sicher, dass du fortfahren möchtest? (ja/nein) " confirm

if [[ "$confirm" != "ja" ]]; then
    echo "Abbruch. Es wurden keine Docker-Ressourcen entfernt."
    exit 0
fi

echo "Stoppe alle laufenden Docker-Container..."
docker stop $(docker ps -aq) 2>/dev/null || echo "Keine Container zum Stoppen gefunden."

echo "Entferne alle Docker-Container..."
docker rm $(docker ps -aq) 2>/dev/null || echo "Keine Container zum Entfernen gefunden."

echo "Entferne alle Docker-Images..."
docker rmi $(docker images -aq) 2>/dev/null || echo "Keine Images zum Entfernen gefunden."

echo "Entferne alle Docker-Volumes..."
docker volume rm $(docker volume ls -q) 2>/dev/null || echo "Keine Volumes zum Entfernen gefunden."

echo "Entferne alle Docker-Netzwerke (außer den Standardnetzwerken)..."
# Exclude default networks (bridge, host, none)
docker network rm $(docker network ls -q | grep -v 'bridge\|host\|none') 2>/dev/null || echo "Keine benutzerdefinierten Netzwerke zum Entfernen gefunden."

echo "Bereinige verwaiste Docker-Ressourcen (system prune)..."
docker state prune -f

echo "Alle Docker-Ressourcen wurden erfolgreich entfernt."

echo "Repo zurücksetzen"
git reset --hard HEAD && git clean -fd
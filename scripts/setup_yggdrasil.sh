#!/bin/bash

echo "Starte Yggdrasil-Installation auf Ubuntu..."

# 1. System aktualisieren
echo "1. Systempaketliste aktualisieren..."
sudo apt update
if [ $? -ne 0 ]; then
    echo "Fehler: apt update fehlgeschlagen. Bitte überprüfen Sie Ihre Internetverbindung oder Paketquellen."
    exit 1
fi
sudo apt upgrade -y
if [ $? -ne 0 ]; then
    echo "Warnung: apt upgrade fehlgeschlagen. Installation wird fortgesetzt, aber Probleme könnten auftreten."
fi

echo "2. Installiere 'dirmngr' (falls benötigt)..."
sudo apt install dirmngr -y
if [ $? -ne 0 ]; then
    echo "Fehler: Installation von dirmngr fehlgeschlagen."
    exit 1
fi

echo "3. Importiere Yggdrasil GPG-Schlüssel..."
sudo mkdir -p /usr/local/share/keyrings
curl -sL https://neilalexander.s3.dualstack.eu-west-2.amazonaws.com/deb/key.txt | sudo gpg --dearmor -o /usr/local/share/keyrings/yggdrasil-keyring.gpg
if [ $? -ne 0 ]; then
    echo "Fehler: Import des Yggdrasil GPG-Schlüssels fehlgeschlagen."
    exit 1
fi

echo "4. Füge Yggdrasil APT-Repository hinzu..."
echo "deb [signed-by=/usr/local/share/keyrings/yggdrasil-keyring.gpg] http://neilalexander.s3.dualstack.eu-west-2.amazonaws.com/deb/ debian yggdrasil" | sudo tee /etc/apt/sources.list.d/yggdrasil.list > /dev/null
if [ $? -ne 0 ]; then
    echo "Fehler: Hinzufügen des APT-Repositories fehlgeschlagen."
    exit 1
fi

echo "5. Aktualisiere Paketliste erneut nach Hinzufügen des Repositorys..."
sudo apt update
if [ $? -ne 0 ]; then
    echo "Fehler: apt update nach Repository-Hinzufügung fehlgeschlagen."
    exit 1
fi

echo "6. Installiere Yggdrasil..."
sudo apt install yggdrasil -y
if [ $? -ne 0 ]; then
    echo "Fehler: Installation von Yggdrasil fehlgeschlagen. Möglicherweise ein Problem mit Abhängigkeiten oder Paketversionen."
    exit 1
fi

echo "7. Aktiviere und starte den Yggdrasil-Dienst..."
sudo systemctl enable yggdrasil
sudo systemctl start yggdrasil
if [ $? -ne 0 ]; then
    echo "Fehler: Aktivierung/Start des Yggdrasil-Dienstes fehlgeschlagen."
    echo "Versuchen Sie, den Status zu überprüfen mit: sudo systemctl status yggdrasil"
    exit 1
fi

echo "8. Überprüfe Yggdrasil-Status und Konfiguration..."
sudo systemctl status yggdrasil --no-pager
echo ""
echo "Yggdrasil sollte nun laufen. Die Standardkonfigurationsdatei befindet sich unter /etc/yggdrasil/yggdrasil.conf."
echo "Ihre Yggdrasil IPv6-Adresse ist:"
sudo yggdrasilctl getSelf | grep "IPv6 address"

echo ""
echo "WICHTIG: Um sich mit dem Yggdrasil-Netzwerk zu verbinden, müssen Sie Peers in der Datei /etc/yggdrasil.conf hinzufügen."
echo "Öffnen Sie die Datei mit einem Editor (z.B. nano):"
echo "  sudo nano /etc/yggdrasil.conf"
echo "Suchen Sie den Abschnitt 'Peers' und fügen Sie öffentliche Peers hinzu (z.B. von https://yggdrasil.network/peers/)."
echo "Nach Änderungen in der Konfigurationsdatei, starten Sie den Dienst neu:"
echo "  sudo systemctl restart yggdrasil"
echo ""
echo "Installation und grundlegende Einrichtung abgeschlossen!"
echo "---------------------------------------------------------"
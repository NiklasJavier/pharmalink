# Pharmalink

Dieses Repository enthält die Anwendung und die zugehörigen Smart Contracts für die Pharmalink-Plattform, eine auf Hyperledger Fabric basierende Lösung zur Nachverfolgung von Lieferketten in der Pharmaindustrie.

![GitHub release (latest by date)](https://img.shields.io/github/v/release/NiklasJavier/pharmalink)
![GitHub](https://img.shields.io/github/license/NiklasJavier/pharmalink)
![GitHub last commit](https://img.shields.io/github/last-commit/NiklasJavier/pharmalink)

-----

> 👋 **Willkommen\!** Diese Anleitung führt Sie durch die Installation der notwendigen Abhängigkeiten, die Einrichtung des Projekts und die Konfiguration des lokalen Test-Netzwerks.

-----

### ⚙️ 1. Voraussetzungen einrichten

Bevor Sie mit dem Projekt beginnen, müssen einige grundlegende Tools auf Ihrem System installiert werden.

#### Docker Installation

Der empfohlene Weg, um die neueste Version von Docker zu erhalten, ist das offizielle Installationsskript.

```bash
curl -fsSL https://get.docker.com -o get-docker.sh && sh get-docker.sh
```

#### Weitere Abhängigkeiten (Ubuntu)

Installieren Sie anschließend `curl`, `golang`, `git` und weitere Tools über den Paketmanager.

```bash
sudo apt-get install curl golang git gradle docker-compose zip jq -y
```

-----

### 📥 2. Projekt einrichten

Klonen Sie das Projekt-Repository in Ihr Home-Verzeichnis.

```bash
cd ~ && git clone git@github.com:NiklasJavier/pharmalink.git
```

-----

### 🚀 3. Entwicklungsumgebung starten

Die folgenden Schritte fahren die komplette lokale Entwicklungsumgebung hoch.

#### a) Fabric Test-Netzwerk starten

Dieser Schritt startet ein komplettes Hyperledger Fabric Test-Netzwerk. Führen Sie das Skript vom Hauptverzeichnis des Projekts aus.

```bash
./scripts/fabric_setup_test.sh up
```

#### b) Hyperledger Explorer starten

Starten Sie den Explorer, um eine Weboberfläche zur Visualisierung des Netzwerks zu erhalten.

```bash
./scripts/fabric_setup_test_explorer.sh up
```

> 🔎 Die Weboberfläche des Explorers ist nach dem Start unter **`http://localhost:8088`** erreichbar.

#### c) Netzwerk-Endpoints

Das Test-Netzwerk verwendet die folgenden standardisierten Adressen (Endpoints):

* **Orderer:**
  * `orderer.example.com:7050`
* **Organisation 1 (Org1):**
  * **Peer 0:** `peer0.org1.example.com:7051`
  * **Certificate Authority (CA):** `localhost:7054`
* **Organisation 2 (Org2):**
  * **Peer 0:** `peer0.org2.example.com:9051`
  * **Certificate Authority (CA):** `localhost:8054`

#### d) Fabric CLI einrichten und konfigurieren

Laden Sie die Kommandozeilen-Tools für Hyperledger Fabric herunter.

```bash
./scripts/fabric_setup_cli.sh
```

Damit Sie die `peer`-Befehle direkt ausführen können, müssen folgende Umgebungsvariablen gesetzt werden:

```bash
export CORE_PEER_LOCALMSPID=Org1MSP
export CORE_PEER_MSPCONFIGPATH="$HOME/pharmalink/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp"
export CORE_PEER_ADDRESS=localhost:7051 
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_TLS_ROOTCERT_FILE="$HOME/pharmalink/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt"
export FABRIC_CFG_PATH="$HOME/fabric-cli/config"
```

> 💡 **Tipp:** Um diese Variablen dauerhaft zu speichern, fügen Sie sie am Ende Ihrer `~/.bashrc`-Datei ein und laden Sie die Konfiguration mit `source ~/.bashrc` neu.

-----

### ▶️ 4. Anwendung ausführen

#### a) ☕ Java-Umgebung einrichten (Einmalig)

Der empfohlene Weg zur Installation von Java ist **SDKMAN\!**.

```bash
# SDKMAN! installieren
curl -s "https://get.sdkman.io" | bash

# SDKMAN! für die aktuelle Terminalsitzung laden
source "$HOME/.sdkman/bin/sdkman-init.sh"

# GraalVM 21 installieren und als Standard festlegen (für unsere Applikation)
sdk install java 21-graal
# JavaVM 11 installieren (für die Chaincodes unter /chaincode/*)
sdk install java 11.0.27-tem
```

#### b) 🔨 Anwendung bauen und starten

Führen Sie den folgenden Befehl im Stammverzeichnis aus. Er kompiliert die Anwendung, baut das Docker-Image und startet den Container.

```bash
./gradlew build && docker build -f src/main/docker/Dockerfile.jvm -t quarkus/pharmalink-jvm . && docker run -i --rm -p 8080:8080 quarkus/pharmalink-jvm
```

#### c) 🌐 Auf die Anwendung zugreifen

Nachdem der Container gestartet ist, ist der Service erreichbar unter:

* `http://IHRE-IP-ADRESSE:8080/`
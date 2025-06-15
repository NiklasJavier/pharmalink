# Anleitung zur Projekteinrichtung

Diese Anleitung führt dich durch die Installation der notwendigen Abhängigkeiten, die Einrichtung deines Projekts und die Konfiguration des Netzwerks.

-----

## 1\. Systemvoraussetzungen einrichten

Bevor du mit dem Projekt beginnst, musst du einige grundlegende Tools auf deinem System installieren.

### Docker Installation

Zuerst installierst du **Docker** mit dem offiziellen Installationsskript. Dies ist die empfohlene Methode, um die neueste Version von Docker zu erhalten.

```bash
curl -fsSL https://get.docker.com -o get-docker.sh && sh get-docker.sh
```

### Weitere Installationen

Installiere anschließend **curl**, **golang**, **git** und **docker-compose** über den Paketmanager von Ubuntu.

```bash
sudo apt-get install curl golang git gradle docker-compose zip jq -y
```

-----

## 2\. Projekt einrichten

Jetzt ist es an der Zeit, das Projekt-Repository zu klonen und in dein Home-Verzeichnis zu verschieben.

```bash
cd ~ && git clone git@github.com:NiklasJavier/pharmalink.git
```

Dieser Befehl wechselt zuerst in dein Home-Verzeichnis (`~`) und klont dann das `pharmalink`-Repository.

-----

## 3\. Setup eines Fabric Test Netzwerks

Dieser Schritt startet ein komplettes Hyperledger Fabric Test-Netzwerk. Führe das Skript vom Hauptverzeichnis (Root) des Projekts aus.

```bash
./scripts/fabric_setup_test.sh up
```

-----

## 4\. Setup des Hyperledger Explorers

Nachdem das Fabric-Netzwerk läuft, kannst du den Hyperledger Explorer starten, um eine Weboberfläche zur Visualisierung des Netzwerks zu erhalten. Führe auch dieses Skript vom Hauptverzeichnis des Projekts aus.

```bash
./scripts/fabric_setup_test_explorer.sh up
```

Nachdem das Skript erfolgreich durchgelaufen ist, ist die Weboberfläche des Explorers unter **`http://localhost:8088`** erreichbar.

-----

## 5\. Endpoint-Beschreibung des Test-Netzwerks

Das gestartete Test-Netzwerk verwendet standardisierte Adressen (Endpoints), um die Kommunikation zwischen den Komponenten zu ermöglichen. Diese sind wichtig für die Konfiguration von Client-Anwendungen oder die Nutzung der Kommandozeilen-Tools.

* **Orderer:**
    * `orderer.example.com:7050`
* **Organisation 1 (Org1):**
    * **Peer 0:** `peer0.org1.example.com:7051`
    * **Certificate Authority (CA):** `localhost:7054`
* **Organisation 2 (Org2):**
    * **Peer 0:** `peer0.org2.example.com:9051`
    * **Certificate Authority (CA):** `localhost:8054`

-----

## 6\. Setup der Fabric Kommandozeilen-Tools (CLI)

Dieser Schritt lädt die notwendigen Docker-Images und Binärdateien für Hyperledger Fabric herunter. Diese Tools (z.B. `peer`, `orderer`, `configtxgen`) werden benötigt, um mit dem aufgesetzten Netzwerk zu interagieren. Führe das Skript vom Hauptverzeichnis des Projekts aus.

```bash
./scripts/fabric_setup_cli.sh
```

Damit Sie die Fabric-CLI-Befehle (z.B. `peer`) direkt in Ihrem Terminal ausführen können, **müssen Systemumgebungsvariablen gesetzt werden**. Diese teilen Ihrem System mit, mit welchem Netzwerk-Peer es sich verbinden soll und unter welcher Identität (MSP) es agieren soll.

```bash
export CORE_PEER_LOCALMSPID=Org1MSP
export CORE_PEER_MSPCONFIGPATH="$HOME/pharmalink/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp"
export CORE_PEER_ADDRESS=localhost:7051 
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_TLS_ROOTCERT_FILE="$HOME/pharmalink/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt"
export FABRIC_CFG_PATH="$HOME/fabric-cli/test-network"
```

**Tipp:** Um diese Variablen nicht bei jeder neuen Terminalsitzung erneut setzen zu müssen, können Sie sie am Ende Ihrer `~/.bashrc`-Datei eintragen und die Datei mit `source ~/.bashrc` neu laden.

-----

## 7\. Applikation bauen und starten

### 7.1. Umgebung einrichten (Einmalig)

**Java (JDK installieren):**
Der empfohlene Weg zur Installation von Java ist [SDKMAN\!](https://sdkman.io/).

```bash
# SDKMAN! installieren
curl -s "https://get.sdkman.io" | bash

# SDKMAN! für die aktuelle Terminalsitzung laden
source "$HOME/.sdkman/bin/sdkman-init.sh"

# GraalVM 21 installieren und als Standard festlegen
sdk install java 21-graal
```

### 7.2. Anwendung bauen und starten

Führen Sie den folgenden Befehl im Stammverzeichnis des Projekts aus. Er kompiliert die Anwendung, baut das Docker-Image und startet den Container in einem Schritt.

```bash
./gradlew build && docker build -f src/main/docker/Dockerfile.jvm -t quarkus/pharmalink-jvm . && docker run -i --rm -p 8080:8080 quarkus/pharmalink-jvm
```

### 7.3. Auf die Anwendung zugreifen

Nachdem der Container gestartet ist, ist der Service erreichbar unter:

* `http://deine-eigene-ip-adresse:8080/`
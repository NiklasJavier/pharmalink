# Pharmalink

Dieses Repository enthält die Anwendung und die zugehörigen Smart Contracts für die Pharmalink-Plattform, eine auf Hyperledger Fabric basierende Lösung zur Nachverfolgung von Lieferketten in der Pharmaindustrie.

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
sudo apt-get install curl git docker-compose zip jq -y
```

#### Zum Bauen (App + Chaincode)

Der empfohlene Weg zur Installation von Java ist **SDKMAN\!**.

```bash
# SDKMAN! installieren
curl -s "https://get.sdkman.io" | bash

# SDKMAN! für die aktuelle Terminalsitzung laden
source "$HOME/.sdkman/bin/sdkman-init.sh"

# GraalVM 21 installieren und als Standard festlegen (für unsere Applikation)
sdk install java 21-graal
# JavaVM 11 installieren (für die Chaincodes unter /chaincode/*)
sdk install java 11.0.27-tem # (als default setzen)
# Gradle installieren
sdk install gradle 8.14.2
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

Damit Sie die `peer`-Befehle direkt ausführen können, müssen folgende Umgebungsvariablen gesetzt werden. Diese Variablen definieren die Identität (Org1 Admin), den Ziel-Peer und die notwendigen Zertifikate für eine sichere Kommunikation.

```bash
export CHANNEL_NAME="pharmalink"
export CHAINCODE="pharmalink_chaincode_main"

export ENDPOINT="localhost"
export BASE_DIR="$HOME"
export ORG_DIR="$BASE_DIR/pharmalink/fabric-samples/test-network/organizations"
export FABRIC_CFG_PATH="$BASE_DIR/fabric-cli/config"

export CORE_PEER_ADDRESS="$ENDPOINT:7051"
export ORDERER_ADDRESS="$ENDPOINT:7050"
export PEER0_ORG1_ADDRESS="$ENDPOINT:7051"
export PEER0_ORG2_ADDRESS="$ENDPOINT:9051"

export CORE_PEER_LOCALMSPID=Org1MSP
export CORE_PEER_MSPCONFIGPATH="$ORG_DIR/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp"
export CORE_PEER_TLS_ENABLED=true

export ORDERER_CA="$ORG_DIR/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem"
export PEER0_ORG1_CA="$ORG_DIR/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt"
export PEER0_ORG2_CA="$ORG_DIR/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt"

```

> 💡 **Tipp:** Um diese Variablen dauerhaft zu speichern, fügen Sie den gesamten Block am Ende Ihrer `~/.bashrc`-Datei ein und laden Sie die Konfiguration mit `source ~/.bashrc` neu.

-----

### ⛓️ 4. Mit dem Chaincode interagieren

Nachdem das Netzwerk läuft und die Umgebungsvariablen gesetzt sind, können Sie mit dem `peer`-CLI direkt mit dem Smart Contract interagieren.

#### a) Asset erstellen (Invoke)

Der `invoke`-Befehl schreibt neue Daten in den Ledger. Wir rufen die Funktion `CreateAsset` auf, um ein neues Medikamenten-Asset zu erstellen.

```bash
peer chaincode invoke -o $ORDERER_ADDRESS --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n $CHAINCODE --peerAddresses $PEER0_ORG1_ADDRESS --tlsRootCertFiles $PEER0_ORG1_CA --peerAddresses $PEER0_ORG2_ADDRESS --tlsRootCertFiles $PEER0_ORG2_CA -c '{"Args":["CreateAsset","asset102","Rot","5","Anna","450"]}'
```

> Bei Erfolg sehen Sie die Meldung `Chaincode invoke successful. result: status:200`.

#### b) Asset abfragen (Query)

Der `query`-Befehl liest Daten nur aus, ohne den Ledger zu verändern.

**Einzelnes Asset abfragen:**
Wir verwenden die Funktion `ReadAsset`, um das eben erstellte Asset zu überprüfen.

```bash
peer chaincode query -C $CHANNEL_NAME -n $CHAINCODE -c '{"Args":["ReadAsset","asset102"]}'
```

**Alle Assets auflisten:**
Die Funktion `GetAllAssets` gibt eine Liste aller vorhandenen Assets zurück.

```bash
peer chaincode query -C $CHANNEL_NAME -n $CHAINCODE -c '{"Args":["GetAllAssets"]}'
```

-----

### ▶️ 5. Anwendung ausführen

#### a) 🔨 Anwendung bauen und starten

Führen Sie den folgenden Befehl im Stammverzeichnis aus. Er kompiliert die Anwendung, baut das Docker-Image und startet den Container.

```bash
./gradlew build && docker build -f src/main/docker/Dockerfile.jvm -t quarkus/pharmalink-jvm . && docker run -i --rm -p 8080:8080 quarkus/pharmalink-jvm
```

#### b) 🌐 Auf die Anwendung zugreifen

Nachdem der Container gestartet ist, ist der Service erreichbar unter:

* `http://IHRE-IP-ADRESSE:8080/`
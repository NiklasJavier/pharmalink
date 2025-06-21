# Pharmalink
[![Latest Release](https://img.shields.io/github/v/release/NiklasJavier/pharmalink?logo=github)](https://github.com/NiklasJavier/pharmalink/releases/latest)
[![Docker Image](https://img.shields.io/badge/docker-ghcr.io-blue?logo=docker)](https://github.com/NiklasJavier/pharmalink/pkgs/container/pharmalink)

> Dieses Repository enthält die Anwendung und die zugehörigen Smart Contracts für die Pharmalink-Plattform, eine auf Hyperledger Fabric basierende Lösung zur Nachverfolgung von Lieferketten in der Pharmaindustrie.

### 🏗️ 1. Architektur

Die Plattform besteht aus drei Hauptkomponenten:

* **SpringBoot Applikation (`./src`):** Ein Java-basiertes Backend, das eine REST-API bereitstellt. Es dient als Schnittstelle für Benutzer und externe Systeme und kommuniziert per gRPC mit dem Hyperledger Fabric Netzwerk.
* **Chaincode (`./chaincode/pharmalink_chaincode_main`):** Der Smart Contract, geschrieben in Java, der die gesamte Geschäftslogik enthält. Er definiert die Datenstrukturen (Assets) und die Regeln für deren Erstellung und Veränderung auf der Blockchain.
* **Hyperledger Fabric Test-Netzwerk (`./docker`, `./scripts`):** Eine Sammlung von Docker-Containern und Skripten, um eine lokale Blockchain-Umgebung mit mehreren Organisationen, Peers und einem Orderer zu starten und zu verwalten.

-----

### ⚙️ 2. Voraussetzungen einrichten

Bevor Sie mit dem Projekt beginnen, müssen einige grundlegende Tools auf Ihrem System installiert werden.

#### Docker Installation

```bash
curl -fsSL https://get.docker.com -o get-docker.sh && sh get-docker.sh
```

#### Weitere Abhängigkeiten (Ubuntu)

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

# Java 11 für die Chaincodes installieren und als Standard festlegen
sdk install java 11.0.27-tem
sdk default java 11.0.27-tem

# GraalVM 21 für die SpringBoot-Applikation installieren
sdk install java 21-graal
```

> 💡 **Hinweis:** Der Chaincode muss mit Java 11 gebaut werden, während die Applikation Java 21 benötigt. Wechseln Sie bei Bedarf die Version mit `sdk use java ...`.

-----

### 📥 3. Projekt einrichten

Klonen Sie das Projekt-Repository in Ihr Home-Verzeichnis.

```bash
cd ~ && git clone git@github.com:NiklasJavier/pharmalink.git
```

-----

### 🚀 4. Entwicklungsumgebung starten

#### a) Fabric Test-Netzwerk starten

Dieser Schritt startet ein komplettes Hyperledger Fabric Test-Netzwerk.

```bash
bash ./scripts/fabric_setup_test.sh up
```

#### b) Fabric CLI einrichten

Laden Sie die Kommandozeilen-Tools für Hyperledger Fabric herunter.

```bash
bash ./scripts/fabric_setup_cli.sh
```

#### c) Umgebungsvariablen für die CLI setzen

Damit Sie die `peer`-Befehle direkt ausführen können, müssen die passenden Umgebungsvariablen für Ihre Rolle im Netzwerk gesetzt werden. Die Skripte dafür liegen unter [`scripts/roles/`](./scripts/roles/).

Um beispielsweise als **Admin** zu agieren, führen Sie folgenden Befehl aus:

```bash
source ./scripts/fabric_setEnv.sh
```

#### d) Chaincode initialisieren und Akteure registrieren

Dieser entscheidende Schritt installiert, genehmigt und committet den Chaincode im Netzwerk. Zusätzlich werden die initialen Akteure (Hersteller, Großhändler etc.) auf der Blockchain registriert.

```bash
bash ./scripts/fabric_setup_test_consortium.sh
```

#### e) Optional: Hyperledger Explorer starten

Starten Sie den Explorer, um eine Weboberfläche zur Visualisierung des Netzwerks und der Transaktionen zu erhalten.

```bash
bash ./scripts/fabric_setup_test_explorer.sh up
```

> 🔎 Die Weboberfläche des Explorers ist nach dem Start unter **`http://localhost:8088`** erreichbar.

-----

### 👥 5. Akteure im Netzwerk

Das Test-Netzwerk simuliert eine Lieferkette mit den folgenden vordefinierten Rollen und Organisationen:

| Akteur / Rolle | CLI-Skript |
| :--- | :--- |
| **Hersteller** | `fabric_role_hersteller.sh` |
| **Großhändler** | `fabric_role_grosshaendler.sh` |
| **Apotheke** | `fabric_role_apotheke.sh` |
| **Behörde** | `fabric_role_behoerde.sh` |

Sie können die Identität in Ihrer Kommandozeile jederzeit wechseln, indem Sie das entsprechende Skript aus dem Verzeichnis `scripts/roles/` ausführen.

-----

### ⛓️ 6. Mit dem Chaincode per CLI interagieren

Nachdem das Netzwerk läuft und die Umgebungsvariablen gesetzt sind, können Sie mit dem `peer`-CLI direkt mit dem Smart Contract interagieren.

Eine detaillierte Beschreibung aller verfügbaren Chaincode-Funktionen und deren Parameter finden Sie in der [`README` des Chaincode-Verzeichnisses](./chaincode/README.md).

**Beispiel: `initCall` als Hersteller ausführen**

Mit dem folgenden Befehl wird die `initCall`-Funktion auf dem Chaincode aufgerufen. Zuerst wird das passende Rollen-Skript geladen, um die Identität zu setzen.

```bash
source scripts/roles/fabric_role_hersteller.sh && \
peer chaincode invoke -o $ORDERER_ADDRESS --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n $CHAINCODE \
--peerAddresses $PEER0_ORG1_ADDRESS --tlsRootCertFiles $PEER0_ORG1_CA \
--peerAddresses $PEER0_ORG2_ADDRESS --tlsRootCertFiles $PEER0_ORG2_CA \
-c '{"function":"initCall","Args":["max.mustermann@example.com","QmWgX..."]}'
```

-----

### ▶️ 7. Anwendung ausführen

#### a) 🔨 Anwendung bauen

Führen Sie den folgenden Befehl im Stammverzeichnis aus. Er kompiliert die Anwendung und baut das Docker-Image.

```bash
./gradlew build && docker build -f src/main/docker/Dockerfile.jvm -t pharmalink/app .
```

#### b) 🐳 Anwendung starten

Starten Sie den erstellten Container. Wichtig ist hierbei, den Container mit dem `pharmalink_default`-Netzwerk zu verbinden, damit die Applikation die Fabric-Peers erreichen kann.

```bash
docker run -p 8080:8080 --network="pharmalink" --rm pharmalink/app
```

-----

### 🖥️ 8. API-Endpunkte (unserer Anwendung)

Wenn die SpringBoot-Anwendung läuft, stellt sie die folgenden REST-Endpunkte zur Verfügung:

| Methode | URL | Beschreibung |
| :--- | :--- | :--- |
| `GET` | `/api/assets` | Ruft eine Liste aller Assets im Ledger ab. |
| `POST` | `/api/assets` | Erstellt ein neues Asset (z.B. ein Medikament). |
| `GET` | `/api/assets/{id}` | Ruft ein spezifisches Asset anhand seiner ID ab. |
| `GET` | `/api/assets/{id}/history` | Ruft die Transaktionshistorie für ein spezifisches Asset ab. |

-----

### 🛑 9. Herunterfahren der Umgebung

Um die Docker-Container zu stoppen und das erstellte Netzwerk zu bereinigen, verwenden Sie die `down`-Befehle der Skripte:

```bash
# Stoppt das Fabric-Netzwerk
./scripts/fabric_setup_test.sh down

# Stoppt den Explorer
./scripts/fabric_setup_test_explorer.sh down
```
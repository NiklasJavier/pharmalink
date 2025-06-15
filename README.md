# Anleitung zur Projekteinrichtung

Diese Anleitung führt dich durch die Installation der notwendigen Abhängigkeiten, die Einrichtung deines Projekts und die optionale Konfiguration eines Overlay-Netzwerks.

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

## 3\. Setup der Fabric Kommandozeilen-Tools (CLI)

Dieser Schritt lädt die notwendigen Docker-Images und Binärdateien für Hyperledger Fabric herunter. Diese Tools (z.B. `peer`, `orderer`, `configtxgen`) werden vom nachfolgenden Skript benötigt, um das Netzwerk aufzubauen. Führe das Skript vom Hauptverzeichnis des Projekts aus.

```bash
./scripts/fabric_setup_cli.sh
```

-----

## 4\. Setup eines Fabric Test Netzwerks

Dieser Schritt startet ein komplettes Hyperledger Fabric Test-Netzwerk. Führe das Skript vom Hauptverzeichnis (Root) des Projekts aus.

```bash
./scripts/fabric_setup_test.sh
```

-----

## 5\. Setup des Hyperledger Explorers

Nachdem das Fabric-Netzwerk läuft, kannst du den Hyperledger Explorer starten, um eine Weboberfläche zur Visualisierung des Netzwerks zu erhalten. Führe auch dieses Skript vom Hauptverzeichnis des Projekts aus.

```bash
./scripts/fabric_setup_test_explorer.sh
```

Nachdem das Skript erfolgreich durchgelaufen ist, ist die Weboberfläche des Explorers unter **`http://localhost:8088`** erreichbar.

-----

## 6\. Applikation bauen und starten

### 6.1. Umgebung einrichten (Einmalig)

**Java (JDK installieren\!):**
Der empfohlene Weg zur Installation von Java ist [SDKMAN\!](https://sdkman.io/).

```bash
# SDKMAN! installieren
curl -s "https://get.sdkman.io" | bash

# SDKMAN! für die aktuelle Terminalsitzung laden
source "$HOME/.sdkman/bin/sdkman-init.sh"

# GraalVM 21 installieren und als Standard festlegen
sdk install java 21-graal
```

### 6.2. Anwendung bauen und starten

Führen Sie den folgenden Befehl im Stammverzeichnis des Projekts aus. Er kompiliert die Anwendung, baut das Docker-Image und startet den Container in einem Schritt.

```bash
./gradlew build && docker build -f src/main/docker/Dockerfile.jvm -t quarkus/pharmalink-jvm . && docker run -i --rm -p 8080:8080 quarkus/pharmalink-jvm
```

### 6.3. Auf die Anwendung zugreifen

Nachdem der Container gestartet ist, ist der Service erreichbar unter:

* `http://deine-eigene-ip-adresse:8080/`

-----

## 7\. Optional: Overlay-Netzwerk einrichten (Yggdrasil)

Falls dein Projekt ein Overlay-Netzwerk (z.B. über Yggdrasil) benötigt, folge diesen Schritten. Dies ermöglicht die Kommunikation zwischen den verschiedenen Komponenten des Projekts über ein virtuelles Netzwerk.

### 7.1. Yggdrasil Installation

Navigiere in den geklonten Projektordner und führe das bereitgestellte Skript aus, um Yggdrasil einzurichten.

```bash
cd pharmalink # Stelle sicher, dass du im Projektordner bist
sh ./scripts/yggdrasil_setup.sh
```

### 7.2. Yggdrasil Konfiguration anpassen

Nach der Installation musst du die Yggdrasil-Konfigurationsdatei anpassen, um die richtigen Peer- und Listen-Adressen für dein Overlay-Netzwerk festzulegen. Öffne die Konfigurationsdatei mit einem Editor:

```bash
sudo nano /etc/yggdrasil/yggdrasil.conf
```

Passe die Sektionen `Peers` und `Listen` in der Datei an deine spezifischen Anforderungen an. Hier ist ein Beispiel, das du als Vorlage verwenden kannst:

```hjson
# Peer: Definiert die Adressen anderer Knoten, zu denen Ihr Knoten als Client aktiv eine Verbindung aufbauen soll.

Peers: [
  tls://EXTERNAL1.EXAMPLE.COM:21603
  tls://EXTERNAL2.EXAMPLE.COM:21603
  tls://EXTERNAL3.EXAMPLE.COM:21603
]

# Listen: Legt die Adressen fest, unter denen Ihr Knoten als Server passiv auf eingehende Verbindungen wartet.
# (Optional) Per UFW oder IPTables den eingehenden Verkehr einschränken.

Listen: [
  tls://MYHOST.EXAMPLE.COM:21603
]

# ... Rest der Konfiguration ...
```

**Wichtiger Hinweis:** Ersetze die Beispiel-Adressen (`MYHOST.EXAMPLE.COM:21603, EXTERNAL.EXAMPLE.COM:21603`) durch die tatsächlichen Adressen deiner Netzwerk-Peers.

Speichere die Änderungen in der Datei (in `nano`: **Strg+O**, dann **Enter**) und schließe den Editor (**Strg+X**).

### 7.3. Yggdrasil-Dienst neu starten

Damit die Änderungen in der Konfiguration wirksam werden, musst du den Yggdrasil-Dienst neu starten:

```bash
sudo systemctl restart yggdrasil
```
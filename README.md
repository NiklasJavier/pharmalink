# Anleitung zur Projekteinrichtung

Diese Anleitung führt dich durch die Installation der notwendigen Abhängigkeiten, die Einrichtung deines Projekts und die optionale Konfiguration eines Overlay-Netzwerks.

---

## 1. Systemvoraussetzungen einrichten

Bevor du mit dem Projekt beginnst, musst du einige grundlegende Tools auf deinem System installieren.

### Docker Installation

Zuerst installierst du **Docker** mit dem offiziellen Installationsskript. Dies ist die empfohlene Methode, um die neueste Version von Docker zu erhalten.

```bash
curl -fsSL https://get.docker.com -o get-docker.sh && sh get-docker.sh
```

### Weitere Installationen

Installiere anschließend **curl**, **golang**, **git** und **docker-compose** über den Paketmanager von Ubuntu.

```bash
sudo apt-get install curl golang git gradle docker-compose -y
```

---

## 2. Projekt einrichten

Jetzt ist es an der Zeit, das Projekt-Repository zu klonen und in dein Home-Verzeichnis zu verschieben.

```bash
cd ~ && git clone git@github.com:NiklasJavier/fabric-medical-supply-chain.git
```
Dieser Befehl wechselt zuerst in dein Home-Verzeichnis (`~`) und klont dann das `fabric-medical-supply-chain`-Repository.

---

## 3. Optional: Overlay-Netzwerk einrichten (Yggdrasil)

Falls dein Projekt ein Overlay-Netzwerk (z.B. über Yggdrasil) benötigt, folge diesen Schritten. Dies ermöglicht die Kommunikation zwischen den verschiedenen Komponenten des Projekts über ein virtuelles Netzwerk.

### Yggdrasil Installation

Navigiere in den geklonten Projektordner und führe das bereitgestellte Skript aus, um Yggdrasil einzurichten.

```bash
cd fabric-medical-supply-chain # Stelle sicher, dass du im Projektordner bist
sh ./scripts/setup_yggdrasil.sh
```

### Yggdrasil Konfiguration anpassen

Nach der Installation musst du die Yggdrasil-Konfigurationsdatei anpassen, um die richtigen Peer- und Listen-Adressen für dein Overlay-Netzwerk festzulegen. Öffne die Konfigurationsdatei mit einem Editor:

```bash
sudo nano /etc/yggdrasil.conf
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

### Yggdrasil-Dienst neu starten

Damit die Änderungen in der Konfiguration wirksam werden, musst du den Yggdrasil-Dienst neu starten:

```bash
sudo systemctl restart yggdrasil
```

### 4. Applikation als Docker-Container ausführen

**1. Bauen und Starten**

Führen Sie den folgenden Befehl im Stammverzeichnis des Projekts aus. Er kompiliert die Anwendung, baut das Docker-Image und startet den Container in einem Schritt:

```bash
./gradlew build && docker build -f src/main/docker/Dockerfile.jvm -t quarkus/pharmalink-jvm . && docker run -i --rm -p 8080:8080 quarkus/pharmalink-jvm
```

**2. Zugriff auf die Anwendung**

Nachdem der Container gestartet ist, ist die Anwendung erreichbar unter:
* `http://deine-ip-adresse:8080/`
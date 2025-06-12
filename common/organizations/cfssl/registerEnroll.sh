#!/usr/bin/env bash
# Copyright 2023 Aditya Joshi, All rights reserved

# Beschreibung:
# Dieses Skript ist dafür zuständig, alle Kryptomaterialien (Zertifikate und Schlüssel)
# für dein Hyperledger Fabric-Netzwerk zu generieren und sie in der korrekten
# Verzeichnisstruktur abzulegen. Es handelt sich hierbei um eine "Offline"-Methode,
# die das Befehlszeilen-Tool 'cfssl' direkt verwendet, anstatt über laufende Fabric CA Server zu gehen.
#
# Im Wesentlichen automatisiert das Skript die folgenden kritischen Schritte für jede Organisation:
# 1. Struktur aufbauen: Erstellt die notwendigen Verzeichnisse auf dem Host-System
#    (z.B. für CAs, MSPs, Peers, Orderer, TLS-Zertifikate) unter dem angepassten
#    '../../../fabric-samples/organizations/'-Schema.
# 2. Root CAs generieren: Erzeugt die Root-Zertifikate und privaten Schlüssel
#    für die Orderer-CA und jede der vier Peer-CAs der Regulatory Orgs.
# 3. TLS- und MSP-Zertifikate generieren: Stellt spezifische Zertifikate und Schlüssel
#    für jeden Orderer-Knoten, jeden Peer-Knoten, jeden Admin-Benutzer und jeden
#    Client-Benutzer aus. Hostnamen (Domains) werden dynamisch in die Zertifikate aufgenommen.
# 4. MSP-Konfiguration ('config.yaml') erstellen: Definiert die Organizational Units (OUs)
#    und verweist auf das Root-Zertifikat, damit Fabric die Rollen der Identitäten erkennt.
# 5. Dateien aufräumen: Entfernt temporär erstellte CSR-Dateien.
#
# Bedeutung: Ohne diese korrekt generierten und platzierten Zertifikate kann dein
# Fabric-Netzwerk nicht starten oder funktioniert nicht ordnungsgemäß.
# Dieses Skript ist der Schritt, der die "Ausweise" für alle Teilnehmer deines
# Netzwerks ausstellt und bereitstellt, damit sie sich sicher im Netzwerk bewegen
# und miteinander kommunizieren können.

# Anpassung der Basis-Pfade
BASE_ORG_PATH="organizations"

# Überprüfen, ob der BASE_ORG_PATH existiert
# Dies ist entscheidend, da das Skript sonst möglicherweise versucht, Dateien in nicht existierende oder falsche Pfade zu schreiben.
if [ ! -d "$BASE_ORG_PATH" ]; then
    echo "Fehler: Der angegebene Basis-Pfad für Organisationen ($BASE_ORG_PATH) existiert nicht."
    echo "Bitte stelle sicher, dass dieser Pfad korrekt ist und alle notwendigen übergeordneten Verzeichnisse vorhanden sind."
    exit 1
fi

# Funktionen für die Generierung von Peer-Zertifikaten (CA und Identities)
function peer_cert() {

    TYPE=$1 #peer user
    USER=$2 # z.B. peer0.de.navine.tech
    ORG_DOMAIN=$3 # z.B. de.navine.tech
    ORG_NAME=$4 # z.B. RegDeMSP

    # Verzeichnisse für die Peer-Organisation erstellen
    mkdir -p "$BASE_ORG_PATH/peerOrganizations/$ORG_DOMAIN/ca"
    mkdir -p "$BASE_ORG_PATH/peerOrganizations/$ORG_DOMAIN/msp/cacerts"
    mkdir -p "$BASE_ORG_PATH/peerOrganizations/$ORG_DOMAIN/msp/tlscacerts"
    mkdir -p "$BASE_ORG_PATH/peerOrganizations/$ORG_DOMAIN/peers"
    mkdir -p "$BASE_ORG_PATH/peerOrganizations/$ORG_DOMAIN/tlsca"

    CERT_DIR="$BASE_ORG_PATH/peerOrganizations/$ORG_DOMAIN"

    # CA-Zertifikate generieren, falls nicht vorhanden
    if [ ! -f "$CERT_DIR/ca/ca-key.pem" ]; then
        # Hier wird der CA-Name aus dem cfssl/ca-peer.json verwendet, der generisch ist
        # Die spezifische CA-ID für diese Org wird später über fabric-ca-server-config.yaml gesetzt
        cfssl gencert -initca "${PWD}/cfssl/ca-peer.json" | cfssljson -bare "$CERT_DIR/ca/ca"

        cp "$CERT_DIR/ca/ca.pem" "$CERT_DIR/tlsca/tlsca.$ORG_DOMAIN-cert.pem"
        cp "$CERT_DIR/ca/ca.pem" "$CERT_DIR/ca/ca.$ORG_DOMAIN-cert.pem"

        cp "$CERT_DIR/ca/ca.pem" "$CERT_DIR/msp/cacerts/"
        cp "$CERT_DIR/ca/ca.pem" "$CERT_DIR/msp/tlscacerts/"

        echo "NodeOUs:
    Enable: true
    ClientOUIdentifier:
        Certificate: cacerts/ca.pem
        OrganizationalUnitIdentifier: client
    PeerOUIdentifier:
        Certificate: cacerts/ca.pem
        OrganizationalUnitIdentifier: peer
    AdminOUIdentifier:
        Certificate: cacerts/ca.pem
        OrganizationalUnitIdentifier: admin
    OrdererOUIdentifier:
        Certificate: cacerts/ca.pem
        OrganizationalUnitIdentifier: orderer" >"$CERT_DIR/msp/config.yaml"
    fi

    if [[ "$TYPE" == "peer" ]]; then
        generate_peer_certs "$CERT_DIR" "$USER" "$ORG_DOMAIN" "$ORG_NAME"
    fi
    if [[ "$TYPE" == "admin" || "$TYPE" == "client" ]]; then # Auch client-Typ hinzugefügt
        generate_user_certs "$CERT_DIR" "$USER" "$TYPE" "$ORG_DOMAIN"
    fi

    # Aufräumen von CSR-Dateien, aber nur die, die von cfssl selbst generiert werden
    # Besser ist, die spezifischen temporären Dateien zu löschen, die sed erstellt
    # find . -name "*.csr" -print0 | xargs -0 rm # Dies könnte zu viel löschen
}

# Funktionen für die Generierung von Orderer-Zertifikaten (CA und Identities)
function orderer_cert() {
    TYPE=$1 #orderer user
    USER=$2 #orderer.navine.tech
    ORG_DOMAIN=$3 # navine.tech

    # Verzeichnisse für die Orderer-Organisation erstellen
    mkdir -p "$BASE_ORG_PATH/ordererOrganizations/$ORG_DOMAIN/ca"
    mkdir -p "$BASE_ORG_PATH/ordererOrganizations/$ORG_DOMAIN/msp/cacerts"
    mkdir -p "$BASE_ORG_PATH/ordererOrganizations/$ORG_DOMAIN/msp/tlscacerts"
    mkdir -p "$BASE_ORG_PATH/ordererOrganizations/$ORG_DOMAIN/orderers"
    mkdir -p "$BASE_ORG_PATH/ordererOrganizations/$ORG_DOMAIN/tlsca"

    CERT_DIR="$BASE_ORG_PATH/ordererOrganizations/$ORG_DOMAIN"

    # CA-Zertifikate generieren, falls nicht vorhanden
    if [ ! -f "$CERT_DIR/ca/ca-key.pem" ]; then
        cfssl gencert -initca "${PWD}/cfssl/ca-orderer.json" | cfssljson -bare "$CERT_DIR/ca/ca"

        cp "$CERT_DIR/ca/ca.pem" "$CERT_DIR/tlsca/tlsca.$ORG_DOMAIN-cert.pem" # Angepasster Dateiname
        cp "$CERT_DIR/ca/ca.pem" "$CERT_DIR/msp/cacerts/"
        cp "$CERT_DIR/ca/ca.pem" "$CERT_DIR/msp/tlscacerts/tlsca.$ORG_DOMAIN-cert.pem" # Angepasster Dateiname

        echo "NodeOUs:
    Enable: true
    ClientOUIdentifier:
        Certificate: cacerts/ca.pem
        OrganizationalUnitIdentifier: client
    PeerOUIdentifier:
        Certificate: cacerts/ca.pem
        OrganizationalUnitIdentifier: peer
    AdminOUIdentifier:
        Certificate: cacerts/ca.pem
        OrganizationalUnitIdentifier: admin
    OrdererOUIdentifier:
        Certificate: cacerts/ca.pem
        OrganizationalUnitIdentifier: orderer" >"$CERT_DIR/msp/config.yaml"
    fi

    if [[ "$TYPE" == "orderer" ]]; then
        generate_orderer_certs "$CERT_DIR" "$USER" "$ORG_DOMAIN"
    fi

    if [[ "$TYPE" == "admin" ]]; then
        generate_user_certs "$CERT_DIR" "$USER" "$TYPE" "$ORG_DOMAIN"
    fi

    # Aufräumen von CSR-Dateien
    # find . -name "*.csr" -print0 | xargs -0 rm # Dies könnte zu viel löschen
}

# Funktion zur Generierung von Benutzer-Zertifikaten (Admin, Client)
function generate_user_certs() {
    CERT_DIR=$1
    USER=$2
    TYPE=$3
    ORG_DOMAIN=$4 # Wird verwendet, um Hostnamen in CSR-Vorlage zu ersetzen

    # Verzeichnisse für den Benutzer erstellen
    mkdir -p "$CERT_DIR/users/$USER/tls"
    for DIR in cacerts keystore signcerts tlscacerts; do
        mkdir -p "$CERT_DIR/users/$USER/msp/$DIR"
    done

    # Templated CSR-Datei erstellen und mit relevanten Hostnamen füllen
    # Hostnamen aus der Vorlage + domains
    TEMP_CSR_JSON="${PWD}/cfssl/${TYPE}-${USER}-csr.json"
    cat "${PWD}/cfssl/${TYPE}-csr-template.json" | \
        sed -e "s/{USER}/$USER/g" | \
        sed -e "s/\"hosts\": \[\s*\"{USER}\"\s*,\s*\"localhost\"\s*,\s*\"127.0.0.1\"\s*,\s*\"0.0.0.0\"\s*\]/\"hosts\": [\"${USER}\", \"localhost\", \"127.0.0.1\", \"0.0.0.0\", \"de.navine.tech\", \"fr.navine.tech\", \"be.navine.tech\", \"es.navine.tech\", \"orderer.navine.tech\"]/g" \
        >"$TEMP_CSR_JSON"

    # Signieren des Benutzer-Zertifikats (MSP-Zertifikat)
    cfssl gencert \
        -ca="$CERT_DIR/ca/ca.pem" \
        -ca-key="$CERT_DIR/ca/ca-key.pem" \
        -config="${PWD}/cfssl/cert-signing-config.json" \
        -cn="$USER" \
        -hostname="$USER,localhost,127.0.0.1,$ORG_DOMAIN" \
        -profile="sign" \
        "$TEMP_CSR_JSON" | cfssljson -bare "$CERT_DIR/users/$USER/msp/signcerts/cert"

    mv "$CERT_DIR/users/$USER/msp/signcerts/cert-key.pem" "$CERT_DIR/users/$USER/msp/keystore/cert-key.pem"
    cp "$CERT_DIR/ca/ca.pem" "$CERT_DIR/users/$USER/msp/cacerts"
    cp "$CERT_DIR/ca/ca.pem" "$CERT_DIR/users/$USER/msp/tlscacerts"

    # msp/config.yaml generieren
    echo "NodeOUs:
    Enable: true
    ClientOUIdentifier:
      Certificate: cacerts/ca.pem
      OrganizationalUnitIdentifier: client
    PeerOUIdentifier:
      Certificate: cacerts/ca.pem
      OrganizationalUnitIdentifier: peer
    AdminOUIdentifier:
      Certificate: cacerts/ca.pem
      OrganizationalUnitIdentifier: admin
    OrdererOUIdentifier:
      Certificate: cacerts/ca.pem
      OrganizationalUnitIdentifier: orderer" >"$CERT_DIR/users/$USER/msp/config.yaml"

    # Signieren des Benutzer-Zertifikats (TLS-Zertifikat)
    cfssl gencert \
        -ca="$CERT_DIR/ca/ca.pem" \
        -ca-key="$CERT_DIR/ca/ca-key.pem" \
        -config="${PWD}/cfssl/cert-signing-config.json" \
        -cn="$USER" \
        -hostname="$USER,localhost,127.0.0.1,$ORG_DOMAIN" \
        -profile="tls" \
        "$TEMP_CSR_JSON" | cfssljson -bare "$CERT_DIR/users/$USER/tls/client"

    cp "$CERT_DIR/ca/ca.pem" "$CERT_DIR/users/$USER/tls/ca.crt"
    mv "$CERT_DIR/users/$USER/tls/client-key.pem" "$CERT_DIR/users/$USER/tls/client.key"
    mv "$CERT_DIR/users/$USER/tls/client.pem" "$CERT_DIR/users/$USER/tls/client.crt"

    rm "$TEMP_CSR_JSON" # Temporäre CSR-Datei entfernen
}

# Funktion zur Generierung von Peer-Knoten-Zertifikaten
function generate_peer_certs() {
    CERT_DIR=$1
    USER=$2 # z.B. peer0.de.navine.tech
    ORG_DOMAIN=$3 # z.B. de.navine.tech
    ORG_NAME=$4 # z.B. RegDeMSP

    # Verzeichnisse für den Peer erstellen
    for DIR in cacerts keystore signcerts tlscacerts; do
        mkdir -p "$CERT_DIR/peers/$USER/msp/$DIR"
    done
    mkdir -p "$CERT_DIR/peers/$USER/tls"

    # Templated CSR-Datei erstellen und mit relevanten Hostnamen füllen
    TEMP_CSR_JSON="${PWD}/cfssl/peer-${USER}.json"
    cat "${PWD}/cfssl/peer-csr-template.json" | \
        sed -e "s/{USER}/$USER/g" | \
        sed -e "s/\"hosts\": \[\s*\"{USER}\"\s*,\s*\"localhost\"\s*,\s*\"127.0.0.1\"\s*,\s*\"0.0.0.0\"\s*\]/\"hosts\": [\"${USER}\", \"localhost\", \"127.0.0.1\", \"0.0.0.0\", \"${ORG_DOMAIN}\"]/g" \
        >"$TEMP_CSR_JSON"


    # Signieren des Peer-Zertifikats (MSP-Zertifikat)
    cfssl gencert \
        -ca="$CERT_DIR/ca/ca.pem" \
        -ca-key="$CERT_DIR/ca/ca-key.pem" \
        -config="${PWD}/cfssl/cert-signing-config.json" \
        -cn="$USER" \
        -hostname="$USER,localhost,127.0.0.1,$ORG_DOMAIN" \
        -profile="sign" \
        "$TEMP_CSR_JSON" | cfssljson -bare "$CERT_DIR/peers/${USER}/msp/signcerts/cert"

    mv "$CERT_DIR/peers/$USER/msp/signcerts/cert-key.pem" "$CERT_DIR/peers/$USER/msp/keystore"

    cp "$CERT_DIR/ca/ca.pem" "$CERT_DIR/peers/$USER/msp/cacerts"
    cp "$CERT_DIR/ca/ca.pem" "$CERT_DIR/peers/$USER/msp/tlscacerts"

    # msp/config.yaml generieren
    echo "NodeOUs:
    Enable: true
    ClientOUIdentifier:
        Certificate: cacerts/ca.pem
        OrganizationalUnitIdentifier: client
    PeerOUIdentifier:
        Certificate: cacerts/ca.pem
        OrganizationalUnitIdentifier: peer
    AdminOUIdentifier:
        Certificate: cacerts/ca.pem
        OrganizationalUnitIdentifier: admin
    OrdererOUIdentifier:
        Certificate: cacerts/ca.pem
        OrganizationalUnitIdentifier: orderer" >"$CERT_DIR/peers/$USER/msp/config.yaml"

    # Signieren des Peer-Zertifikats (TLS-Zertifikat)
    cfssl gencert \
        -ca="$CERT_DIR/ca/ca.pem" \
        -ca-key="$CERT_DIR/ca/ca-key.pem" \
        -config="${PWD}/cfssl/cert-signing-config.json" \
        -cn="$USER" \
        -hostname="$USER,localhost,127.0.0.1,$ORG_DOMAIN" \
        -profile="tls" \
        "$TEMP_CSR_JSON" | cfssljson -bare "$CERT_DIR/peers/$USER/tls/server"

    cp "$CERT_DIR/ca/ca.pem" "$CERT_DIR/peers/$USER/tls/ca.crt"
    mv "$CERT_DIR/peers/$USER/tls/server.pem" "$CERT_DIR/peers/$USER/tls/server.crt"
    mv "$CERT_DIR/peers/$USER/tls/server-key.pem" "$CERT_DIR/peers/$USER/tls/server.key"

    rm "$TEMP_CSR_JSON" # Temporäre CSR-Datei entfernen
}

# Funktion zur Generierung von Orderer-Knoten-Zertifikaten
function generate_orderer_certs() {

    CERT_DIR=$1
    USER=$2 # z.B. orderer.navine.tech
    ORG_DOMAIN=$3 # z.B. navine.tech

    # Verzeichnisse für den Orderer erstellen
    for DIR in cacerts keystore signcerts tlscacerts; do
        mkdir -p "$CERT_DIR/orderers/$USER/msp/$DIR"
    done
    mkdir -p "$CERT_DIR/orderers/$USER/tls"

    # Templated CSR-Datei erstellen und mit relevanten Hostnamen füllen
    TEMP_CSR_JSON="${PWD}/cfssl/orderer-${USER}.json"
    cat "${PWD}/cfssl/orderer-csr-template.json" | \
        sed -e "s/{USER}/$USER/g" | \
        sed -e "s/\"hosts\": \[\s*\"{USER}\"\s*,\s*\"localhost\"\s*,\s*\"127.0.0.1\"\s*,\s*\"0.0.0.0\"\s*\]/\"hosts\": [\"${USER}\", \"localhost\", \"127.0.0.1\", \"0.0.0.0\", \"${ORG_DOMAIN}\"]/g" \
        >"$TEMP_CSR_JSON"

    # Signieren des Orderer-Zertifikats (MSP-Zertifikat)
    cfssl gencert \
        -ca="$CERT_DIR/ca/ca.pem" \
        -ca-key="$CERT_DIR/ca/ca-key.pem" \
        -config="${PWD}/cfssl/cert-signing-config.json" \
        -cn="$USER" \
        -hostname="$USER,localhost,127.0.0.1,$ORG_DOMAIN" \
        -profile="sign" \
        "$TEMP_CSR_JSON" | cfssljson -bare "$CERT_DIR/orderers/$USER/msp/signcerts/cert"

    mv "$CERT_DIR/orderers/$USER/msp/signcerts/cert-key.pem" "$CERT_DIR/orderers/$USER/msp/keystore"

    cp "$CERT_DIR/ca/ca.pem" "$CERT_DIR/orderers/$USER/msp/cacerts"
    cp "$CERT_DIR/ca/ca.pem" "$CERT_DIR/orderers/$USER/msp/tlscacerts/tlsca.$ORG_DOMAIN-cert.pem" # Angepasster Dateiname

    # msp/config.yaml generieren
    echo "NodeOUs:
    Enable: true
    ClientOUIdentifier:
        Certificate: cacerts/ca.pem
        OrganizationalUnitIdentifier: client
    PeerOUIdentifier:
        Certificate: cacerts/ca.pem
        OrganizationalUnitIdentifier: peer
    AdminOUIdentifier:
        Certificate: cacerts/ca.pem
        OrganizationalUnitIdentifier: admin
    OrdererOUIdentifier:
        Certificate: cacerts/ca.pem
        OrganizationalUnitIdentifier: orderer" >"$CERT_DIR/orderers/$USER/msp/config.yaml"

    # Signieren des Orderer-Zertifikats (TLS-Zertifikat)
    cfssl gencert \
        -ca="$CERT_DIR/ca/ca.pem" \
        -ca-key="$CERT_DIR/ca/ca-key.pem" \
        -config="${PWD}/cfssl/cert-signing-config.json" \
        -cn="$USER" \
        -hostname="$USER,localhost,127.0.0.1,$ORG_DOMAIN" \
        -profile="tls" \
        "$TEMP_CSR_JSON" | cfssljson -bare "$CERT_DIR/orderers/$USER/tls/server"

    cp "$CERT_DIR/ca/ca.pem" "$CERT_DIR/orderers/$USER/tls/ca.crt"
    mv "$CERT_DIR/orderers/$USER/tls/server.pem" "$CERT_DIR/orderers/$USER/tls/server.crt"
    mv "$CERT_DIR/orderers/$USER/tls/server-key.pem" "$CERT_DIR/orderers/$USER/tls/server.key"
    rm "$TEMP_CSR_JSON" # Temporäre CSR-Datei entfernen
}

# --- Skriptausführung ---

# Clean up existing crypto materials first (optional, but recommended for fresh start)
echo "Removing existing crypto materials..."
rm -rf "$BASE_ORG_PATH/peerOrganizations"
rm -rf "$BASE_ORG_PATH/ordererOrganizations"

echo "Generating Orderer CA and identities for navine.tech..."
orderer_cert "admin" "Admin@navine.tech" "navine.tech"
orderer_cert "orderer" "orderer.navine.tech" "navine.tech"

echo "Generating Peer CAs and identities for Regulatory Orgs (DE, FR, BE, ES)..."

# Deutschland
peer_cert "admin" "Admin@de.navine.tech" "de.navine.tech" "RegDeMSP"
peer_cert "peer" "peer0.de.navine.tech" "de.navine.tech" "RegDeMSP"
peer_cert "client" "User1@de.navine.tech" "de.navine.tech" "RegDeMSP" # Beispiel Client

# Frankreich
peer_cert "admin" "Admin@fr.navine.tech" "fr.navine.tech" "RegFrMSP"
peer_cert "peer" "peer0.fr.navine.tech" "fr.navine.tech" "RegFrMSP"
peer_cert "client" "User1@fr.navine.tech" "fr.navine.tech" "RegFrMSP" # Beispiel Client

# Belgien
peer_cert "admin" "Admin@be.navine.tech" "be.navine.tech" "RegBeMSP"
peer_cert "peer" "peer0.be.navine.tech" "be.navine.tech" "RegBeMSP"
peer_cert "client" "User1@be.navine.tech" "be.navine.tech" "RegBeMSP" # Beispiel Client

# Spanien
peer_cert "admin" "Admin@es.navine.tech" "es.navine.tech" "RegEsMSP"
peer_cert "peer" "peer0.es.navine.tech" "es.navine.tech" "RegEsMSP"
peer_cert "client" "User1@es.navine.tech" "es.navine.tech" "RegEsMSP" # Beispiel Client

echo "All certificates and MSPs generated successfully."
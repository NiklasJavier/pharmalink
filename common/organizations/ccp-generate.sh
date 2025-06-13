#!/usr/bin/env bash

# Beschreibung:
# Dieses Skript generiert die Client Connection Profiles (CCPs) für jede
# Organisation in deinem Hyperledger Fabric-Netzwerk. Diese CCPs sind JSON-
# und YAML-Dateien, die Clients (wie z.B. Fabric SDKs) benötigen, um sich
# mit dem Netzwerk zu verbinden und Transaktionen einzureichen oder abzufragen.
#
# Das Skript ersetzt Platzhalter in den 'ccp-template.json' und 'ccp-template.yaml'
# Dateien mit den spezifischen Informationen jeder Organisation (z.B. deren Domain,
# Peer- und CA-Ports, sowie Pfade zu den TLS-Root-Zertifikaten).
# Die generierten CCPs werden in der jeweiligen Organisationsstruktur unter
# './peerOrganizations/<org-domain>/' abgelegt.

# Funktion zum Konvertieren eines PEM-Zertifikats in eine Ein-Zeilen-String-Darstellung
function one_line_pem {
    echo "`awk 'NF {sub(/\\n/, ""); printf "%s\\\\\\\n",$0;}' $1`"
}

# Funktion zum Generieren eines JSON CCPs
# Parameter: $1=ORG_NAME_SHORT (z.B. 'de'), $2=PEER_PORT, $3=CA_PORT, $4=PEER_TLS_CERT_PATH, $5=CA_TLS_CERT_PATH, $6=ORG_DOMAIN
function json_ccp {
    local ORG_NAME_SHORT=$1
    local PEER_PORT=$2
    local CA_PORT=$3
    local PEER_TLS_CERT_PATH=$4
    local CA_TLS_CERT_PATH=$5
    local ORG_DOMAIN=$6

    local PP=$(one_line_pem "$PEER_TLS_CERT_PATH")
    local CP=$(one_line_pem "$CA_TLS_CERT_PATH")

    # Der sed-Befehl muss jetzt auch die Domain ersetzen und die Port-Platzhalter
    sed -e "s/\${ORG_NAME_SHORT}/$ORG_NAME_SHORT/" \
        -e "s/\${ORG_DOMAIN}/$ORG_DOMAIN/" \
        -e "s/\${P0PORT}/$PEER_PORT/" \
        -e "s/\${CAPORT}/$CA_PORT/" \
        -e "s#\${PEERPEM}#$PP#" \
        -e "s#\${CAPEM}#$CP#" \
        "./ccp-template.json" # Pfad zum Template anpassen
}

# Funktion zum Generieren eines YAML CCPs
# Parameter: $1=ORG_NAME_SHORT (z.B. 'de'), $2=PEER_PORT, $3=CA_PORT, $4=PEER_TLS_CERT_PATH, $5=CA_TLS_CERT_PATH, $6=ORG_DOMAIN
function yaml_ccp {
    local ORG_NAME_SHORT=$1
    local PEER_PORT=$2
    local CA_PORT=$3
    local PEER_TLS_CERT_PATH=$4
    local CA_TLS_CERT_PATH=$5
    local ORG_DOMAIN=$6

    local PP=$(one_line_pem "$PEER_TLS_CERT_PATH")
    local CP=$(one_line_pem "$CA_TLS_CERT_PATH")

    # Der sed-Befehl muss jetzt auch die Domain ersetzen und die Port-Platzhalter
    sed -e "s/\${ORG_NAME_SHORT}/$ORG_NAME_SHORT/" \
        -e "s/\${ORG_DOMAIN}/$ORG_DOMAIN/" \
        -e "s/\${P0PORT}/$PEER_PORT/" \
        -e "s/\${CAPORT}/$CA_PORT/" \
        -e "s#\${PEERPEM}#$PP#" \
        -e "s#\${CAPEM}#$CP#" \
        "./ccp-template.yaml" | sed -e $'s/\\\\n/\\\n          /g' # Pfad zum Template anpassen
}

# --- Definitionen der Organisationen und deren Parameter ---
# Hier werden die Daten für unsere 4 Regulatory Orgs festgelegt

# Regulatory Org Deutschland
ORG_DE_NAME_SHORT="de"
ORG_DE_DOMAIN="de.navine.tech"
ORG_DE_PEER_PORT="7051" # Muss dem Port des Peer0.de.navine.tech entsprechen
ORG_DE_CA_PORT="7054"   # Muss dem Port der CA_reg_de entsprechen
ORG_DE_PEER_TLS_CERT_PATH="./peerOrganizations/de.navine.tech/peers/peer0.de.navine.tech/tls/ca.crt"
ORG_DE_CA_TLS_CERT_PATH="./peerOrganizations/de.navine.tech/ca/ca.de.navine.tech-cert.pem" # Angepasster Dateiname

# Regulatory Org Frankreich
ORG_FR_NAME_SHORT="fr"
ORG_FR_DOMAIN="fr.navine.tech"
ORG_FR_PEER_PORT="8051" # Muss dem Port des Peer0.fr.navine.tech entsprechen
ORG_FR_CA_PORT="8054"   # Muss dem Port der CA_reg_fr entsprechen
ORG_FR_PEER_TLS_CERT_PATH="./peerOrganizations/fr.navine.tech/peers/peer0.fr.navine.tech/tls/ca.crt"
ORG_FR_CA_TLS_CERT_PATH="./peerOrganizations/fr.navine.tech/ca/ca.fr.navine.tech-cert.pem"

# Regulatory Org Belgien
ORG_BE_NAME_SHORT="be"
ORG_BE_DOMAIN="be.navine.tech"
ORG_BE_PEER_PORT="9051" # Muss dem Port des Peer0.be.navine.tech entsprechen
ORG_BE_CA_PORT="9054"   # Muss dem Port der CA_reg_be entsprechen
ORG_BE_PEER_TLS_CERT_PATH="./peerOrganizations/be.navine.tech/peers/peer0.be.navine.tech/tls/ca.crt"
ORG_BE_CA_TLS_CERT_PATH="./peerOrganizations/be.navine.tech/ca/ca.be.navine.tech-cert.pem"

# Regulatory Org Spanien
ORG_ES_NAME_SHORT="es"
ORG_ES_DOMAIN="es.navine.tech"
ORG_ES_PEER_PORT="10051" # Muss dem Port des Peer0.es.navine.tech entsprechen
ORG_ES_CA_PORT="10054"   # Muss dem Port der CA_reg_es entsprechen
ORG_ES_PEER_TLS_CERT_PATH="./peerOrganizations/es.navine.tech/peers/peer0.es.navine.tech/tls/ca.crt"
ORG_ES_CA_TLS_CERT_PATH="./peerOrganizations/es.navine.tech/ca/ca.es.navine.tech-cert.pem"

# --- Generierung der CCPs für jede Organisation ---

echo "Generiere CCP für Regulatory Org Deutschland (${ORG_DE_DOMAIN})..."
# Pfad zur Ausgabedatei anpassen
OUT_DIR="./peerOrganizations/${ORG_DE_DOMAIN}"
mkdir -p "$OUT_DIR" # Sicherstellen, dass das Verzeichnis existiert
echo "$(json_ccp "$ORG_DE_NAME_SHORT" "$ORG_DE_PEER_PORT" "$ORG_DE_CA_PORT" "$ORG_DE_PEER_TLS_CERT_PATH" "$ORG_DE_CA_TLS_CERT_PATH" "$ORG_DE_DOMAIN")" > "${OUT_DIR}/connection-${ORG_DE_NAME_SHORT}.json"
echo "$(yaml_ccp "$ORG_DE_NAME_SHORT" "$ORG_DE_PEER_PORT" "$ORG_DE_CA_PORT" "$ORG_DE_PEER_TLS_CERT_PATH" "$ORG_DE_CA_TLS_CERT_PATH" "$ORG_DE_DOMAIN")" > "${OUT_DIR}/connection-${ORG_DE_NAME_SHORT}.yaml"

echo "Generiere CCP für Regulatory Org Frankreich (${ORG_FR_DOMAIN})..."
OUT_DIR="./peerOrganizations/${ORG_FR_DOMAIN}"
mkdir -p "$OUT_DIR"
echo "$(json_ccp "$ORG_FR_NAME_SHORT" "$ORG_FR_PEER_PORT" "$ORG_FR_CA_PORT" "$ORG_FR_PEER_TLS_CERT_PATH" "$ORG_FR_CA_TLS_CERT_PATH" "$ORG_FR_DOMAIN")" > "${OUT_DIR}/connection-${ORG_FR_NAME_SHORT}.json"
echo "$(yaml_ccp "$ORG_FR_NAME_SHORT" "$ORG_FR_PEER_PORT" "$ORG_FR_CA_PORT" "$ORG_FR_PEER_TLS_CERT_PATH" "$ORG_FR_CA_TLS_CERT_PATH" "$ORG_FR_DOMAIN")" > "${OUT_DIR}/connection-${ORG_FR_NAME_SHORT}.yaml"

echo "Generiere CCP für Regulatory Org Belgien (${ORG_BE_DOMAIN})..."
OUT_DIR="./peerOrganizations/${ORG_BE_DOMAIN}"
mkdir -p "$OUT_DIR"
echo "$(json_ccp "$ORG_BE_NAME_SHORT" "$ORG_BE_PEER_PORT" "$ORG_BE_CA_PORT" "$ORG_BE_PEER_TLS_CERT_PATH" "$ORG_BE_CA_TLS_CERT_PATH" "$ORG_BE_DOMAIN")" > "${OUT_DIR}/connection-${ORG_BE_NAME_SHORT}.json"
echo "$(yaml_ccp "$ORG_BE_NAME_SHORT" "$ORG_BE_PEER_PORT" "$ORG_BE_CA_PORT" "$ORG_BE_PEER_TLS_CERT_PATH" "$ORG_BE_CA_TLS_CERT_PATH" "$ORG_BE_DOMAIN")" > "${OUT_DIR}/connection-${ORG_BE_NAME_SHORT}.yaml"

echo "Generiere CCP für Regulatory Org Spanien (${ORG_ES_DOMAIN})..."
OUT_DIR="./peerOrganizations/${ORG_ES_DOMAIN}"
mkdir -p "$OUT_DIR"
echo "$(json_ccp "$ORG_ES_NAME_SHORT" "$ORG_ES_PEER_PORT" "$ORG_ES_CA_PORT" "$ORG_ES_PEER_TLS_CERT_PATH" "$ORG_ES_CA_TLS_CERT_PATH" "$ORG_ES_DOMAIN")" > "${OUT_DIR}/connection-${ORG_ES_NAME_SHORT}.json"
echo "$(yaml_ccp "$ORG_ES_NAME_SHORT" "$ORG_ES_PEER_PORT" "$ORG_ES_CA_PORT" "$ORG_ES_PEER_TLS_CERT_PATH" "$ORG_ES_CA_TLS_CERT_PATH" "$ORG_ES_DOMAIN")" > "${OUT_DIR}/connection-${ORG_ES_NAME_SHORT}.yaml"

echo "Alle Client Connection Profiles generiert."
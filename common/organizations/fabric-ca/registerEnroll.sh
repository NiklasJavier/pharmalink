#!/usr/bin/env bash

# Beschreibung:
# Dieses Skript ist verantwortlich für die Registrierung und Einschreibung aller
# notwendigen Identitäten (Admins, Peers, Orderer, Clients) im Hyperledger Fabric-Netzwerk
# über die jeweils laufenden Fabric Certificate Authority (CA) Server.
#
# Es automatisiert folgende Schritte:
# 1.  **CA-Admin einschreiben:** Jede Org-CA und die Orderer-CA haben einen Bootstrap-Admin.
#     Dieses Skript meldet diesen Admin an, um die CA für weitere Registrierungen nutzen zu können.
# 2.  **Identitäten registrieren:** Es registriert neue Identitäten (Peer, User, Admin, Orderer)
#     bei der jeweiligen CA mit einem Benutzernamen, Passwort und Typ.
# 3.  **Identitäten einschreiben:** Es ruft Zertifikate und Schlüssel für die registrierten
#     Identitäten ab. Diese Materialien werden dann in der korrekten MSP-Struktur abgelegt.
# 4.  **MSP-Konfigurationen anpassen:** Die 'config.yaml'-Dateien für die MSPs werden erstellt/kopiert,
#     um die NodeOU-Erweiterungen (Client, Peer, Admin, Orderer) zu ermöglichen.
# 5.  **TLS-Zertifikate generieren und kopieren:** Spezielle TLS-Zertifikate werden für Peers
#     und Orderer generiert, um die sichere Kommunikation zu gewährleisten, und an die von
#     Fabric erwarteten Standardorte kopiert.
#
# Wichtigkeit: Dieses Skript ist unerlässlich für die PKI (Public Key Infrastructure) deines
# Fabric-Netzwerks. Ohne die korrekte Registrierung und Einschreibung der Identitäten
# können die Komponenten des Netzwerks nicht miteinander kommunizieren oder Transaktionen
# validieren. Es bereitet die Krypto-Materialien für die Docker-Container vor.

# Utility-Funktion zum Protokollieren von Informationen
infoln() {
  echo -e "\e[32m[INFO]\e[0m $1"
}

# Funktion zum Erstellen einer Regulatory Organisation (Peer Org)
# Parameter:
#   $1: Kurzer Name der Org (z.B. "de", "fr", "be", "es")
#   $2: Volle Domain der Org (z.B. "de.navine.tech")
#   $3: MSP ID der Org (z.B. "RegDeMSP")
#   $4: Host-Port der CA für diese Org (z.B. "7054", "8054" etc.)
#   $5: Name der CA (z.B. "ca-reg-de")
function createOrg() {
  local ORG_NAME_SHORT="$1"
  local ORG_DOMAIN="$2"
  local ORG_MSPID="$3"
  local CA_HOST_PORT="$4"
  local CA_NAME="$5"

  infoln "Enrolling the CA admin for ${ORG_DOMAIN}"
  # Anpassung: 'organizations' direkt, da das Skript aus dem 'test-network'-Root läuft
  mkdir -p "organizations/peerOrganizations/${ORG_DOMAIN}/"

  export FABRIC_CA_CLIENT_HOME="${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/"

  set -x
  # Anpassung: Pfad zu CA-Zertifikat korrigiert, da 'fabric-ca' auf gleicher Ebene wie 'organizations' liegt
  fabric-ca-client enroll -u "https://admin:adminpw@localhost:${CA_HOST_PORT}" --caname "${CA_NAME}" --tls.certfiles "${PWD}/organizations/fabric-ca/${ORG_NAME_SHORT}/ca-cert.pem"
  { set +x; } 2>/dev/null

  # Wichtiger Hinweis: Die CA-Zertifikatsnamen im config.yaml müssen korrekt sein.
  echo "NodeOUs:
  Enable: true
  ClientOUIdentifier:
    Certificate: cacerts/localhost-${CA_HOST_PORT}-${CA_NAME}.pem
    OrganizationalUnitIdentifier: client
  PeerOUIdentifier:
    Certificate: cacerts/localhost-${CA_HOST_PORT}-${CA_NAME}.pem
    OrganizationalUnitIdentifier: peer
  AdminOUIdentifier:
    Certificate: cacerts/localhost-${CA_HOST_PORT}-${CA_NAME}.pem
    OrganizationalUnitIdentifier: admin
  OrdererOUIdentifier:
    Certificate: cacerts/localhost-${CA_HOST_PORT}-${CA_NAME}.pem
    OrganizationalUnitIdentifier: orderer" > "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/msp/config.yaml"

  # Kopieren des Org CA-Zertifikats in die org-level ca und tlsca Verzeichnisse
  mkdir -p "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/msp/tlscacerts"
  cp "${PWD}/organizations/fabric-ca/${ORG_NAME_SHORT}/ca-cert.pem" "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/msp/tlscacerts/ca.crt"

  mkdir -p "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/tlsca"
  cp "${PWD}/organizations/fabric-ca/${ORG_NAME_SHORT}/ca-cert.pem" "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/tlsca/tlsca.${ORG_DOMAIN}-cert.pem"

  mkdir -p "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/ca"
  cp "${PWD}/organizations/fabric-ca/${ORG_NAME_SHORT}/ca-cert.pem" "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/ca/ca.${ORG_DOMAIN}-cert.pem"

  infoln "Registering peer0 for ${ORG_DOMAIN}"
  set -x
  fabric-ca-client register --caname "${CA_NAME}" --id.name "peer0.${ORG_DOMAIN}" --id.secret "peer0pw" --id.type peer --tls.certfiles "${PWD}/organizations/fabric-ca/${ORG_NAME_SHORT}/ca-cert.pem"
  { set +x; } 2>/dev/null

  infoln "Registering user1 for ${ORG_DOMAIN}"
  set -x
  fabric-ca-client register --caname "${CA_NAME}" --id.name "user1" --id.secret "user1pw" --id.type client --tls.certfiles "${PWD}/organizations/fabric-ca/${ORG_NAME_SHORT}/ca-cert.pem"
  { set +x; } 2>/dev/null

  infoln "Registering the org admin for ${ORG_DOMAIN}"
  set -x
  fabric-ca-client register --caname "${CA_NAME}" --id.name "${ORG_NAME_SHORT}admin" --id.secret "${ORG_NAME_SHORT}adminpw" --id.type admin --tls.certfiles "${PWD}/organizations/fabric-ca/${ORG_NAME_SHORT}/ca-cert.pem"
  { set +x; } 2>/dev/null

  infoln "Generating the peer0 msp for ${ORG_DOMAIN}"
  set -x
  fabric-ca-client enroll -u "https://peer0.${ORG_DOMAIN}:peer0pw@localhost:${CA_HOST_PORT}" --caname "${CA_NAME}" -M "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/peers/peer0.${ORG_DOMAIN}/msp" --tls.certfiles "${PWD}/organizations/fabric-ca/${ORG_NAME_SHORT}/ca-cert.pem"
  { set +x; } 2>/dev/null

  cp "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/msp/config.yaml" "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/peers/peer0.${ORG_DOMAIN}/msp/config.yaml"

  infoln "Generating the peer0-tls certificates for ${ORG_DOMAIN}, use --csr.hosts to specify Subject Alternative Names"
  set -x
  fabric-ca-client enroll -u "https://peer0.${ORG_DOMAIN}:peer0pw@localhost:${CA_HOST_PORT}" --caname "${CA_NAME}" -M "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/peers/peer0.${ORG_DOMAIN}/tls" --enrollment.profile tls --csr.hosts "peer0.${ORG_DOMAIN}" --csr.hosts localhost --tls.certfiles "${PWD}/organizations/fabric-ca/${ORG_NAME_SHORT}/ca-cert.pem"
  { set +x; } 2>/dev/null

  # Kopieren der TLS CA cert, server cert, server keystore zu bekannten Dateinamen
  cp "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/peers/peer0.${ORG_DOMAIN}/tls/tlscacerts/"* "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/peers/peer0.${ORG_DOMAIN}/tls/ca.crt"
  cp "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/peers/peer0.${ORG_DOMAIN}/tls/signcerts/"* "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/peers/peer0.${ORG_DOMAIN}/tls/server.crt"
  cp "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/peers/peer0.${ORG_DOMAIN}/tls/keystore/"* "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/peers/peer0.${ORG_DOMAIN}/tls/server.key"

  infoln "Generating the user msp for ${ORG_DOMAIN}"
  set -x
  fabric-ca-client enroll -u "https://user1:user1pw@localhost:${CA_HOST_PORT}" --caname "${CA_NAME}" -M "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/users/User1@${ORG_DOMAIN}/msp" --tls.certfiles "${PWD}/organizations/fabric-ca/${ORG_NAME_SHORT}/ca-cert.pem"
  { set +x; } 2>/dev/null

  cp "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/msp/config.yaml" "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/users/User1@${ORG_DOMAIN}/msp/config.yaml"

  infoln "Generating the org admin msp for ${ORG_DOMAIN}"
  set -x
  fabric-ca-client enroll -u "https://${ORG_NAME_SHORT}admin:${ORG_NAME_SHORT}adminpw@localhost:${CA_HOST_PORT}" --caname "${CA_NAME}" -M "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/users/Admin@${ORG_DOMAIN}/msp" --tls.certfiles "${PWD}/organizations/fabric-ca/${ORG_NAME_SHORT}/ca-cert.pem"
  { set +x; } 2>/dev/null

  cp "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/msp/config.yaml" "${PWD}/organizations/peerOrganizations/${ORG_DOMAIN}/users/Admin@${ORG_DOMAIN}/msp/config.yaml"
}

# Funktion zum Erstellen der Orderer Organisation
# Parameter:
#   $1: Volle Domain der Orderer Org (z.B. "navine.tech")
#   $2: Host-Port der Orderer CA (z.B. "11054")
#   $3: Name der Orderer CA (z.B. "ca-orderer")
function createOrdererOrg() {
  local ORDERER_ORG_DOMAIN="$1"
  local ORDERER_CA_HOST_PORT="$2"
  local ORDERER_CA_NAME="$3"

  infoln "Enrolling the Orderer CA admin"
  # Anpassung: 'organizations' direkt, da das Skript aus dem 'test-network'-Root läuft
  mkdir -p "organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}"

  export FABRIC_CA_CLIENT_HOME="${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}"

  set -x
  # Anpassung: Pfad zu CA-Zertifikat korrigiert
  fabric-ca-client enroll -u "https://admin:adminpw@localhost:${ORDERER_CA_HOST_PORT}" --caname "${ORDERER_CA_NAME}" --tls.certfiles "${PWD}/organizations/fabric-ca/ordererOrg/ca-cert.pem"
  { set +x; } 2>/dev/null

  # Wichtiger Hinweis: Die CA-Zertifikatsnamen im config.yaml müssen korrekt sein.
  echo "NodeOUs:
  Enable: true
  ClientOUIdentifier:
    Certificate: cacerts/localhost-${ORDERER_CA_HOST_PORT}-${ORDERER_CA_NAME}.pem
    OrganizationalUnitIdentifier: client
  PeerOUIdentifier:
    Certificate: cacerts/localhost-${ORDERER_CA_HOST_PORT}-${ORDERER_CA_NAME}.pem
    OrganizationalUnitIdentifier: peer
  AdminOUIdentifier:
    Certificate: cacerts/localhost-${ORDERER_CA_HOST_PORT}-${ORDERER_CA_NAME}.pem
    OrganizationalUnitIdentifier: admin
  OrdererOUIdentifier:
    Certificate: cacerts/localhost-${ORDERER_CA_HOST_PORT}-${ORDERER_CA_NAME}.pem
    OrganizationalUnitIdentifier: orderer" > "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/msp/config.yaml"

  # Kopieren des Orderer CA-Zertifikats in die orderer org-level ca und tlsca Verzeichnisse
  mkdir -p "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/msp/tlscacerts"
  cp "${PWD}/organizations/fabric-ca/ordererOrg/ca-cert.pem" "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/msp/tlscacerts/tlsca.${ORDERER_ORG_DOMAIN}-cert.pem"

  mkdir -p "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/tlsca"
  cp "${PWD}/organizations/fabric-ca/ordererOrg/ca-cert.pem" "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/tlsca/tlsca.${ORDERER_ORG_DOMAIN}-cert.pem"

  # Schleife durch jeden Orderer-Knoten (hier nehmen wir einen einzelnen Orderer an: orderer.navine.tech)
  local ORDERER_NAME="orderer" # Name des einzelnen Orderer-Knotens

  infoln "Registering ${ORDERER_NAME}.${ORDERER_ORG_DOMAIN}"
  set -x
  fabric-ca-client register --caname "${ORDERER_CA_NAME}" --id.name "${ORDERER_NAME}" --id.secret "ordererpw" --id.type orderer --tls.certfiles "${PWD}/organizations/fabric-ca/ordererOrg/ca-cert.pem"
  { set +x; } 2>/dev/null

  infoln "Generating the ${ORDERER_NAME}.${ORDERER_ORG_DOMAIN} MSP"
  set -x
  fabric-ca-client enroll -u "https://${ORDERER_NAME}:ordererpw@localhost:${ORDERER_CA_HOST_PORT}" --caname "${ORDERER_CA_NAME}" -M "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/orderers/${ORDERER_NAME}.${ORDERER_ORG_DOMAIN}/msp" --tls.certfiles "${PWD}/organizations/fabric-ca/ordererOrg/ca-cert.pem"
  { set +x; } 2>/dev/null

  cp "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/msp/config.yaml" "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/orderers/${ORDERER_NAME}.${ORDERER_ORG_DOMAIN}/msp/config.yaml"

  # Workaround: Rename the signcert file to ensure consistency with Cryptogen generated artifacts
  mv "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/orderers/${ORDERER_NAME}.${ORDERER_ORG_DOMAIN}/msp/signcerts/cert.pem" "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/orderers/${ORDERER_NAME}.${ORDERER_ORG_DOMAIN}/msp/signcerts/${ORDERER_NAME}.${ORDERER_ORG_DOMAIN}-cert.pem"

  infoln "Generating the ${ORDERER_NAME}.${ORDERER_ORG_DOMAIN} TLS certificates, use --csr.hosts to specify Subject Alternative Names"
  set -x
  fabric-ca-client enroll -u "https://${ORDERER_NAME}:ordererpw@localhost:${ORDERER_CA_HOST_PORT}" --caname "${ORDERER_CA_NAME}" -M "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/orderers/${ORDERER_NAME}.${ORDERER_ORG_DOMAIN}/tls" --enrollment.profile tls --csr.hosts "${ORDERER_NAME}.${ORDERER_ORG_DOMAIN}" --csr.hosts localhost --tls.certfiles "${PWD}/organizations/fabric-ca/ordererOrg/ca-cert.pem"
  { set +x; } 2>/dev/null

  # Kopieren der TLS CA cert, server cert, server keystore zu bekannten Dateinamen
  cp "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/orderers/${ORDERER_NAME}.${ORDERER_ORG_DOMAIN}/tls/tlscacerts/"* "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/orderers/${ORDERER_NAME}.${ORDERER_ORG_DOMAIN}/tls/ca.crt"
  cp "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/orderers/${ORDERER_NAME}.${ORDERER_ORG_DOMAIN}/tls/signcerts/"* "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/orderers/${ORDERER_NAME}.${ORDERER_ORG_DOMAIN}/tls/server.crt"
  cp "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/orderers/${ORDERER_NAME}.${ORDERER_ORG_DOMAIN}/tls/keystore/"* "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/orderers/${ORDERER_NAME}.${ORDERER_ORG_DOMAIN}/tls/server.key"

  # Kopieren des Orderer Org CA-Zertifikats in das /msp/tlscacerts Verzeichnis des Orderers
  mkdir -p "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/orderers/${ORDERER_NAME}.${ORDERER_ORG_DOMAIN}/msp/tlscacerts"
  cp "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/orderers/${ORDERER_NAME}.${ORDERER_ORG_DOMAIN}/tls/tlscacerts/"* "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/orderers/${ORDERER_NAME}.${ORDERER_ORG_DOMAIN}/msp/tlscacerts/tlsca.${ORDERER_ORG_DOMAIN}-cert.pem"

  # Registrieren und Generieren von Artefakten für den Orderer Admin
  infoln "Registering the orderer admin"
  set -x
  fabric-ca-client register --caname "${ORDERER_CA_NAME}" --id.name "ordererAdmin" --id.secret "ordererAdminpw" --id.type admin --tls.certfiles "${PWD}/organizations/fabric-ca/ordererOrg/ca-cert.pem"
  { set +x; } 2>/dev/null

  infoln "Generating the admin msp"
  set -x
  fabric-ca-client enroll -u "https://ordererAdmin:ordererAdminpw@localhost:${ORDERER_CA_HOST_PORT}" --caname "${ORDERER_CA_NAME}" -M "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/users/Admin@${ORDERER_ORG_DOMAIN}/msp" --tls.certfiles "${PWD}/organizations/fabric-ca/ordererOrg/ca-cert.pem"
  { set +x; } 2>/dev/null

  cp "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/msp/config.yaml" "${PWD}/organizations/ordererOrganizations/${ORDERER_ORG_DOMAIN}/users/Admin@${ORDERER_ORG_DOMAIN}/msp/config.yaml"
}
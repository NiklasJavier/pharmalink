#!/usr/bin/env bash
#
# Copyright IBM Corp All Rights Reserved
#
# SPDX-License-Identifier: Apache-2.0
#

# Dieses Skript ist dafür verantwortlich, einen Kanal auf dem Orderer-Dienst
# mithilfe des 'osnadmin'-Befehls zu erstellen. Es wird typischerweise
# von 'scripts/createChannel.sh' aufgerufen.

# Imports und Pfad-Definitionen
# Annahme: ROOTDIR wird vom aufrufenden Skript (z.B. network.sh) gesetzt und
# zeigt auf das Wurzelverzeichnis des 'test-network'-Ordners.
# Pfad zu den Fabric-Binaries
export PATH=${ROOTDIR}/bin:${PATH} # Korrigiert den Pfad zu Binaries

# Parameter, die von createChannel.sh übergeben werden:
# $1: CHANNEL_NAME           - Name des zu erstellenden Kanals
# $2: CHANNEL_TX_FILE        - Pfad zur Kanal-Erstellungs-Transaktionsdatei (z.B. ./channel-artifacts/mychannel.tx)
# $3: ORDERER_HOSTNAME       - Hostname des Orderers (z.B. orderer.navine.tech)
# $4: ORDERER_PORT           - Port des Orderers (z.B. 7050)
# $5: ORDERER_ADMIN_TLS_CLIENT_CERT - Pfad zum TLS-Client-Zertifikat des Orderer-Admins
# $6: ORDERER_ADMIN_TLS_PRIVATE_KEY - Pfad zum TLS-Privatschlüssel des Orderer-Admins
# $7: ORDERER_TLS_ROOTCERT_FILE     - Pfad zum TLS-Root-CA-Zertifikat des Orderers

CHANNEL_NAME="$1"
CHANNEL_TX_FILE="$2"
ORDERER_HOSTNAME="$3"
ORDERER_PORT="$4"
ORDERER_ADMIN_TLS_CLIENT_CERT="$5"
ORDERER_ADMIN_TLS_PRIVATE_KEY="$6"
ORDERER_TLS_ROOTCERT_FILE="$7" # Dies entspricht der ORDERER_CA Variable aus envVar.sh

# --- Korrigierter osnadmin-Befehl ---
# Dieser Befehl führt den Kanal-Join auf dem Orderer-Dienst aus.
# Es wird der Orderer-Admin mit seinem TLS-Client-Zertifikat authentifiziert
# und das Orderer-TLS-Root-CA-Zertifikat zur Validierung der Orderer-TLS-Verbindung verwendet.

infoln "Attempting to join channel '${CHANNEL_NAME}' on Orderer via osnadmin..."
set -x
osnadmin channel join \
  --channelID "${CHANNEL_NAME}" \
  --config-block "${CHANNEL_TX_FILE}" \
  -o "${ORDERER_HOSTNAME}:${ORDERER_PORT}" \
  --client-cert "${ORDERER_ADMIN_TLS_CLIENT_CERT}" \
  --client-key "${ORDERER_ADMIN_TLS_PRIVATE_KEY}" \
  --ca-file "${ORDERER_TLS_ROOTCERT_FILE}" \
  >> log.txt 2>&1 # Ausgabe in log.txt umleiten
res=$?
{ set +x; } 2>/dev/null

verifyResult $res "OSNAdmin failed to join channel '${CHANNEL_NAME}'"
infoln "Channel '${CHANNEL_NAME}' successfully joined on Orderer."
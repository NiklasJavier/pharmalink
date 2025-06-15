#!/bin/bash

FABRIC_VERSION="2.5.13"
INSTALL_DIR="${HOME}/fabric-cli"
BIN_PATH="${INSTALL_DIR}/bin"

ARCH=$(uname -m)
case "${ARCH}" in
    x86_64)
        FABRIC_ARCH="linux-amd64"
        ;;
    aarch64|arm64)
        FABRIC_ARCH="linux-arm64"
        ;;
    *)
        echo "Fehler: Nicht unterstützte Architektur '${ARCH}'. Unterstützt werden: x86_64 (amd64) und aarch64/arm64."
        exit 1
        ;;
esac

FABRIC_TAR_GZ_URL="https://github.com/hyperledger/fabric/releases/download/v${FABRIC_VERSION}/hyperledger-fabric-${FABRIC_ARCH}-${FABRIC_VERSION}.tar.gz"
DOWNLOAD_FILENAME="hyperledger-fabric-${FABRIC_ARCH}-${FABRIC_VERSION}.tar.gz"

echo "Starte Einrichtung der Hyperledger Fabric Binaries..."
echo "Architektur: ${FABRIC_ARCH}"
echo "Download-URL: ${FABRIC_TAR_GZ_URL}"
echo "Installationsverzeichnis: ${INSTALL_DIR}"

echo "1. Lade Hyperledger Fabric Binaries herunter..."
curl -L "${FABRIC_TAR_GZ_URL}" -o "${HOME}/${DOWNLOAD_FILENAME}"
if [ $? -ne 0 ]; then
    echo "Fehler beim Herunterladen der Datei. Bitte überprüfen Sie die URL und Ihre Internetverbindung."
    exit 1
fi
echo "Download abgeschlossen: ${HOME}/${DOWNLOAD_FILENAME}"

if [ -d "${INSTALL_DIR}" ]; then
    echo "2. Vorhandenes Installationsverzeichnis '${INSTALL_DIR}' wird entfernt..."
    rm -rf "${INSTALL_DIR}"
fi

echo "3. Entpacke Fabric Binaries nach '${INSTALL_DIR}'..."
mkdir -p "${INSTALL_DIR}"
tar -xzf "${HOME}/${DOWNLOAD_FILENAME}" -C "${INSTALL_DIR}"
if [ $? -ne 0 ]; then
    echo "Fehler beim Entpacken der Datei."
    exit 1
fi
echo "Entpacken abgeschlossen."

echo "4. Lösche die heruntergeladene Tar-Datei: ${HOME}/${DOWNLOAD_FILENAME}..."
rm "${HOME}/${DOWNLOAD_FILENAME}"

echo "5. Binde den Fabric 'bin'-Pfad in die Shell-Konfiguration ein..."

SHELL_CONFIG_FILE=""
if [[ "echo $SHELL" == */zsh ]]; then
    SHELL_CONFIG_FILE="${HOME}/.zshrc"
elif [[ "echo $SHELL" == */bash ]]; then
    SHELL_CONFIG_FILE="${HOME}/.bashrc"
else
    echo "Warnung: Unbekannte Shell '${SHELL}'. Verwende ~/.bashrc als Fallback."
    SHELL_CONFIG_FILE="${HOME}/.bashrc"
fi

if ! grep -q "export PATH=\"\$PATH:${BIN_PATH}\"" "${SHELL_CONFIG_FILE}"; then
    echo -e "\n# Add Hyperledger Fabric binaries to PATH" >> "${SHELL_CONFIG_FILE}"
    echo "export PATH=\"\$PATH:${BIN_PATH}\"" >> "${SHELL_CONFIG_FILE}"
    echo "${SHELL_CONFIG_FILE} wurde aktualisiert."
else
    echo "Der Fabric 'bin'-Pfad ist bereits in ${SHELL_CONFIG_FILE} vorhanden. Keine Änderung vorgenommen."
fi

echo "6. Lade ${SHELL_CONFIG_FILE} für die aktuelle Sitzung neu..."
source "${SHELL_CONFIG_FILE}"

echo ""
echo "---------------------------------------------------------"
echo "Einrichtung der Hyperledger Fabric Binaries abgeschlossen!"
echo "Bitte öffnen Sie ein NEUES Terminal, damit der PATH vollständig wirksam wird,"
echo "oder führen Sie 'source ${SHELL_CONFIG_FILE}' manuell aus, um die PATH-Änderung sofort zu nutzen."
echo ""
echo "Testen Sie die Installation mit:"
echo "peer version"
echo "fabric-ca-client version"
echo "---------------------------------------------------------"
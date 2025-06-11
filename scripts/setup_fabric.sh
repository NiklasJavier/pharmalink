#!/bin/bash

FABRIC_VERSION="3.1.1"
FABRIC_ARCH="linux-amd64"
FABRIC_TAR_GZ_URL="https://github.com/hyperledger/fabric/releases/download/v${FABRIC_VERSION}/hyperledger-fabric-${FABRIC_ARCH}-${FABRIC_VERSION}.tar.gz"
DOWNLOAD_FILENAME="hyperledger-fabric-${FABRIC_ARCH}-${FABRIC_VERSION}.tar.gz"
INSTALL_DIR="${HOME}/hyperledger-fabric" # Zielverzeichnis im Home-Ordner
BIN_PATH="${INSTALL_DIR}/bin"            # Der Bin-Ordner, der zum PATH hinzugefügt wird

echo "Starte Einrichtung der Hyperledger Fabric Binaries..."
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

echo "5. Binde den Fabric 'bin'-Pfad in ~/.bashrc ein..."

if ! grep -q "export PATH=\"\$PATH:${BIN_PATH}\"" "${HOME}/.bashrc"; then
    echo -e "\n# Add Hyperledger Fabric binaries to PATH" >> "${HOME}/.bashrc"
    echo "export PATH=\"\$PATH:${BIN_PATH}\"" >> "${HOME}/.bashrc"
    echo ".bashrc wurde aktualisiert."
else
    echo "Der Fabric 'bin'-Pfad ist bereits in ~/.bashrc vorhanden. Keine Änderung vorgenommen."
fi

echo "6. Lade ~/.bashrc für die aktuelle Sitzung neu..."
source "${HOME}/.bashrc"

echo ""
echo "---------------------------------------------------------"
echo "Einrichtung der Hyperledger Fabric Binaries abgeschlossen!"
echo "Bitte öffnen Sie ein NEUES Terminal, damit der PATH vollständig wirksam wird,"
echo "oder führen Sie 'source ~/.bashrc' manuell aus, wenn Sie die PATH-Änderung in der aktuellen Sitzung benötigen."
echo ""
echo "Testen Sie die Installation mit:"
echo "peer version"
echo "fabric-ca-client version"
echo "---------------------------------------------------------"
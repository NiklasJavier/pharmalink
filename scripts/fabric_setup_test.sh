#!/bin/bash

source ./fabric_setEnv.sh

SRC_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )" # Where the script lives
FABRIC_VERSION="2.5.13"
CA_VERSION="1.5.15"

function networkUp() {
  networkDown
  cd $SRC_DIR/..
  curl -sSLO https://raw.githubusercontent.com/hyperledger/fabric/main/scripts/install-fabric.sh && chmod +x install-fabric.sh
  ./install-fabric.sh --fabric-version ${FABRIC_VERSION} --ca-version ${CA_VERSION}

  cp -r ./common/organizations ./fabric-samples/test-network/

  cd fabric-samples

  # default to fabric-samples main branch unless v2.2.x is downloaded
  if [[ "${FABRIC_VERSION}" =~ ^2\.2\.[0-9]+$ ]]; then
   git checkout release-2.2
  fi
  cd test-network
  ./network.sh up createChannel -ca -c pharmalink -s couchdb
}

function networkDown() {
  if [[ -d $SRC_DIR/../fabric-samples/test-network ]]; then
   cd $SRC_DIR/../fabric-samples/test-network
   ./network.sh down
   rm -fr $SRC_DIR/../fabric-samples
  fi
}

# NEUE FUNKTION HINZUGEFÜGT
function deployChaincode() {
  echo "## 1. Compiling and packaging chaincode with Gradle..."
  # Führt build und installDist aus. -p zeigt auf das Projektverzeichnis.
  # Wir nehmen an, das Skript wird aus dem übergeordneten Ordner von "chaincode" ausgeführt.
  gradle -p $SRC_DIR/../chaincode/pharmalink_chaincode_main/ installDist

  if [ $? -ne 0 ]; then
    echo "!!! ERROR: Gradle build failed."
    exit 1
  fi
  echo "## Gradle build successful."

  echo ""
  echo "## 2. Deploying chaincode to the Fabric network..."
  $SRC_DIR/../fabric-samples/test-network/network.sh deployCC -c pharmalink -ccn pharmalink_chaincode_main -ccp $SRC_DIR/../chaincode/pharmalink_chaincode_main -ccl java

  if [ $? -ne 0 ]; then
    echo "!!! ERROR: Chaincode deployment failed."
    exit 1
  fi
  echo "## Chaincode deployment successful."
}


function printHelp() {
 echo "./fabric_setup_test up"
 echo "./fabric_setup_test down"
 echo "./fabric_setup_test cc" # HILFE-TEXT ERWEITERT
}

if [[ $# -lt 1 ]] ; then
  printHelp
  exit 0
else
  MODE=$1
  shift
fi

while [[ $# -ge 1 ]] ; do
  key="$1"
  case $key in
  -h )
    printHelp $MODE
    exit 0
    ;;
  -v )
    FABRIC_VERSION="$2"
  if [ ! -z "$3" ];then
      CA_VERSION="$3"
  fi
    shift
    ;;
  esac
  shift
done

if [ "${MODE}" == "up" ]; then
  networkUp
elif [ "${MODE}" == "down" ]; then
  networkDown
# NEUER MODUS HINZUGEFÜGT
elif [ "${MODE}" == "cc" ]; then
  deployChaincode
else
  printHelp
  exit 1
fi
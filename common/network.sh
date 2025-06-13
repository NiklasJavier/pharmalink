#!/usr/bin/env bash
#
# Copyright IBM Corp All Rights Reserved
#
# SPDX-License-Identifier: Apache-2.0
#

# Dieses Skript bringt ein Hyperledger Fabric Netzwerk für den Test von Smart Contracts
# und Anwendungen auf. Das Netzwerk besteht aus VIER Organisationen (DE, FR, BE, ES)
# mit je einem Peer, und einem einzelnen Orderer-Knoten im Raft Ordering Service.
# Benutzer können dieses Skript auch verwenden, um einen Kanal zu erstellen und einen Chaincode
# auf diesem Kanal bereitzustellen.

# prepending $PWD/../bin to PATH to ensure we are picking up the correct binaries
# this may be commented out to resolve installed version of tools if desired
#
# However using PWD in the path has the side effect that location that
# this script is run from is critical. To ease this, get the directory
# this script is actually in and infer location from there. (putting first)

ROOTDIR=$(cd "$(dirname "$0")" && pwd)
# Korrektur: Pfad zu bin-Verzeichnis, angenommen, es liegt im 'test-network/bin'
export PATH=${ROOTDIR}/bin:${PATH} # Nur das eigene bin-Verzeichnis hinzufügen, da ROOTDIR bereits 'test-network' ist
export FABRIC_CFG_PATH=${ROOTDIR}/configtx # configtx sollte direkt im ROOTDIR liegen
export VERBOSE=false

# push to the required directory & set a trap to go back if needed
pushd ${ROOTDIR} > /dev/null
trap "popd > /dev/null" EXIT

. scripts/utils.sh # Annahme: scripts/utils.sh liegt relativ zu ROOTDIR

: ${CONTAINER_CLI:="docker"}
if command -v ${CONTAINER_CLI}-compose > /dev/null 2>&1; then
    : ${CONTAINER_CLI_COMPOSE:="${CONTAINER_CLI}-compose"}
else
    : ${CONTAINER_CLI_COMPOSE:="${CONTAINER_CLI} compose"}
fi
infoln "Using ${CONTAINER_CLI} and ${CONTAINER_CLI_COMPOSE}"

# Obtain CONTAINER_IDS and remove them
# This function is called when you bring a network down
function clearContainers() {
  infoln "Removing remaining containers"
  ${CONTAINER_CLI} rm -f $(${CONTAINER_CLI} ps -aq --filter label=service=hyperledger-fabric) 2>/dev/null || true
  ${CONTAINER_CLI} rm -f $(${CONTAINER_CLI} ps -aq --filter name='dev-peer*') 2>/dev/null || true
  ${CONTAINER_CLI} kill "$(${CONTAINER_CLI} ps -q --filter name=ccaas)" 2>/dev/null || true
}

# Delete any images that were generated as a part of this setup
# specifically the following images are often left behind:
# This function is called when you bring the network down
function removeUnwantedImages() {
  infoln "Removing generated chaincode docker images"
  ${CONTAINER_CLI} image rm -f $(${CONTAINER_CLI} images -aq --filter reference='dev-peer*') 2>/dev/null || true
}

# Versions of fabric known not to work with the test network
NONWORKING_VERSIONS="^1\.0\. ^1\.1\. ^1\.2\. ^1\.3\. ^1\.4\."

# Do some basic sanity checking to make sure that the appropriate versions of fabric
# binaries/images are available.
function checkPrereqs() {
  ## Check if your have cloned the peer binaries and configuration files.
  peer version > /dev/null 2>&1

  if [[ $? -ne 0 || ! -d "../config" ]]; then # Geändert von "../config" zu "config"
    errorln "Peer binary and configuration files not found.."
    errorln
    errorln "Follow the instructions in the Fabric docs to install the Fabric Binaries:"
    errorln "https://hyperledger-fabric.readthedocs.io/en/latest/install.html"
    exit 1
  fi
  LOCAL_VERSION=$(peer version | sed -ne 's/^ Version: //p')
  DOCKER_IMAGE_VERSION=$(${CONTAINER_CLI} run --rm hyperledger/fabric-peer:latest peer version | sed -ne 's/^ Version: //p')

  infoln "LOCAL_VERSION=$LOCAL_VERSION"
  infoln "DOCKER_IMAGE_VERSION=$DOCKER_IMAGE_VERSION"

  if [ "$LOCAL_VERSION" != "$DOCKER_IMAGE_VERSION" ]; then
    warnln "Local fabric binaries and docker images are out of sync. This may cause problems."
  fi

  for UNSUPPORTED_VERSION in $NONWORKING_VERSIONS; do
    infoln "$LOCAL_VERSION" | grep -q "$UNSUPPORTED_VERSION"
    if [ $? -eq 0 ]; then
      fatalln "Local Fabric binary version of $LOCAL_VERSION does not match the versions supported by the test network."
    fi

    infoln "$DOCKER_IMAGE_VERSION" | grep -q "$UNSUPPORTED_VERSION"
    if [ $? -eq 0 ]; then
      fatalln "Fabric Docker image version of $DOCKER_IMAGE_VERSION does not match the versions supported by the test network."
    fi
  done

  ## check for cfssl binaries
  # if [ "$CRYPTO" == "cfssl" ]; then # Dieser Block wird nicht benötigt, wenn nur Fabric CA verwendet wird
  #   cfssl version > /dev/null 2>&1
  #   if [[ $? -ne 0 ]]; then
  #     errorln "cfssl binary not found.."
  #     errorln
  #     errorln "Follow the instructions to install the cfssl and cfssljson binaries:"
  #     errorln "https://github.com/cloudflare/cfssl#installation"
  #     exit 1
  #   fi
  # fi

  ## Check for fabric-ca (immer prüfen, da wir Fabric CA verwenden)
  fabric-ca-client version > /dev/null 2>&1
  if [[ $? -ne 0 ]]; then
    errorln "fabric-ca-client binary not found.."
    errorln
    errorln "Follow the instructions in the Fabric docs to install the Fabric Binaries:"
    errorln "https://hyperledger-fabric.readthedocs.io/en/latest/install.html"
    exit 1
  fi
  CA_LOCAL_VERSION=$(fabric-ca-client version | sed -ne 's/ Version: //p')
  CA_DOCKER_IMAGE_VERSION=$(${CONTAINER_CLI} run --rm hyperledger/fabric-ca:latest fabric-ca-client version | sed -ne 's/ Version: //p' | head -1)
  infoln "CA_LOCAL_VERSION=$CA_LOCAL_VERSION"
  infoln "CA_DOCKER_IMAGE_VERSION=$CA_DOCKER_IMAGE_VERSION"

  if [ "$CA_LOCAL_VERSION" != "$CA_DOCKER_IMAGE_VERSION" ]; then
    warnln "Local fabric-ca binaries and docker images are out of sync. This may cause problems."
  fi
}

# Create Organization crypto material using cryptogen or Fabric CAs
function createOrgs() {
  # Aufräumen von alten Krypto-Materialien
  if [ -d "organizations/peerOrganizations" ]; then
    rm -Rf organizations/peerOrganizations && rm -Rf organizations/ordererOrganizations
  fi
  # Aufräumen von alten Fabric CA Ordnern (um sicherzustellen, dass keine alten ca-cert.pem liegen)
  rm -rf organizations/fabric-ca/ordererOrg
  rm -rf organizations/fabric-ca/reg-de
  rm -rf organizations/fabric-ca/reg-fr
  rm -rf organizations/fabric-ca/reg-be
  rm -rf organizations/fabric-ca/reg-es


  # Wir verwenden ausschließlich Fabric CAs
  infoln "Generating certificates using Fabric CA"
  # Starten der CA-Dienste
  # Hier nur die CA-Compose-Dateien hochfahren
  ${CONTAINER_CLI_COMPOSE} -f compose/${COMPOSE_FILE_CA} up -d

  # Ausführen des registerEnroll.sh Skripts aus dem fabric-ca Ordner
  # Wichtig: Der Pfad muss relativ zur Ausführungsposition von network.sh sein.
  # Annahme: network.sh und fabric-ca/ liegen im selben Verzeichnis (test-network/).
  . organizations/fabric-ca/registerEnroll.sh # Angepasster Pfad

  infoln "Creating Orderer Org Identities (navine.tech)..."
  # Parameter: Domain, CA-Host-Port, CA-Name
  createOrdererOrg "navine.tech" "11054" "ca-orderer"

  infoln "Creating Regulatory Organization Germany (de.navine.tech)..."
  createOrg "reg-de" "de.navine.tech" "RegDeMSP" "7054" "ca-reg-de"

  infoln "Creating Regulatory Organization France (fr.navine.tech)..."
  createOrg "reg-fr" "fr.navine.tech" "RegFrMSP" "8054" "ca-reg-fr"

  infoln "Creating Regulatory Organization Belgium (be.navine.tech)..."
  createOrg "reg-be" "be.navine.tech" "RegBeMSP" "9054" "ca-reg-be"

  infoln "Creating Regulatory Organization Spain (es.navine.tech)..."
  createOrg "reg-es" "es.navine.tech" "RegEsMSP" "10054" "ca-reg-es"

  infoln "Generating CCP files for all Regulatory Orgs"
  # Pfad zu ccp-generate.sh anpassen, angenommen, es liegt im 'organizations'-Ordner
  ./organizations/ccp-generate.sh
}

# Once you create the organization crypto material, you need to create the
# genesis block of the application channel.
# The configtxgen tool is used to create the genesis block.
# We will use the 'ChannelUsingRaft' profile from 'configtx.yaml'.

# Bring up the peer and orderer nodes using docker compose.
function networkUp() {

  checkPrereqs

  # generate artifacts if they don't exist
  if [ ! -d "organizations/peerOrganizations" ]; then
    createOrgs
  fi

  # Generiere Orderer Genesis Block (system-genesis-block) und Channel Transaction Artefakte
  infoln "Generating Orderer Genesis block and Channel Transaction artifacts..."
  export FABRIC_CFG_PATH=${ROOTDIR}/configtx # Sicherstellen, dass FABRIC_CFG_PATH korrekt ist
  configtxgen -profile ChannelUsingRaft -channelID system-channel -outputBlock ./system-genesis-block/genesis.block
  if [ $? -ne 0 ]; then
    fatalln "Failed to generate Orderer Genesis block"
  fi
  # Hier kannst du weitere configtxgen Aufrufe hinzufügen, um Kanaltransaktionen zu erstellen,
  # z.B. configtxgen -profile <YourChannelProfile> -outputCreateChannelTx ./channel-artifacts/mychannel.tx -channelID mychannel

  # Kombinieren aller Docker Compose Dateien für den Start des gesamten Netzwerks
  # Annahme: Deine Compose-Dateien liegen im 'compose/' Unterordner
  COMPOSE_FILES="-f compose/${COMPOSE_FILE_CA} \
                 -f compose/${COMPOSE_FILE_ORDERER} \
                 -f compose/${COMPOSE_FILE_PEERS}"

  if [ "${DATABASE}" == "couchdb" ]; then
    # Die CouchDB-Dienste sind in unserer 'compose-peers.yaml' integriert
    # Daher ist hier keine zusätzliche CouchDB-Compose-Datei notwendig, es sei denn,
    # du hast sie als separate Datei. Wenn ja, füge sie hier hinzu:
    # COMPOSE_FILES="${COMPOSE_FILES} -f compose/${COMPOSE_FILE_COUCH}"
    # Da wir CouchDB bereits in docker-compose-peers.yaml integriert haben,
    # muss diese Zeile hier nicht angepasst werden, es sei denn, es gibt eine separate Datei.
    true # Platzhalter, falls kein zusätzlicher CouchDB-File
  fi

  DOCKER_SOCK="${DOCKER_SOCK}" ${CONTAINER_CLI_COMPOSE} ${COMPOSE_FILES} up -d 2>&1

  $CONTAINER_CLI ps -a
  if [ $? -ne 0 ]; then
    fatalln "Unable to start network"
  fi
}

# call the script to create the channel, join the peers of org1 and org2,
# and then update the anchor peers for each organization
function createChannel() {
  # Bring up the network if it is not already up.
  bringUpNetwork="false"

  local bft_true=$1

  if ! $CONTAINER_CLI info > /dev/null 2>&1 ; then
    fatalln "$CONTAINER_CLI network is required to be running to create a channel"
  fi

  # Check if all containers are present - Passt die Anzahl der erwarteten Container an:
  # 5 CAs + 1 Orderer + 4 Peers + 4 CouchDBs = 14 Dienste
  # plus optionaler CLI-Container
  CONTAINERS=($($CONTAINER_CLI ps | grep hyperledger/ | awk '{print $2}'))
  len=$(echo ${#CONTAINERS[@]})

  # Wenn weniger als 14 Hyperledger-Container laufen ODER die Krypto-Materialien fehlen, bring das Netzwerk hoch
  if [[ $len -lt 14 ]] || [[ ! -d "organizations/peerOrganizations" ]]; then
    bringUpNetwork="true"
    infoln "Bringing up network to create channel..."
    networkUp
  else
    echo "Network Running Already"
  fi

  # now run the script that creates a channel. This script uses configtxgen once
  # to create the channel creation transaction and the anchor peer updates.
  # Dies ist ein separates Skript, das wir auch anpassen müssen (scripts/createChannel.sh)
  scripts/createChannel.sh "$CHANNEL_NAME" "$CLI_DELAY" "$MAX_RETRY" "$VERBOSE" "$bft_true"
}


## Call the script to deploy a chaincode to the channel
function deployCC() {
  scripts/deployCC.sh "$CHANNEL_NAME" "$CC_NAME" "$CC_SRC_PATH" "$CC_SRC_LANGUAGE" "$CC_VERSION" "$CC_SEQUENCE" "$CC_INIT_FCN" "$CC_END_POLICY" "$CC_COLL_CONFIG" "$CLI_DELAY" "$MAX_RETRY" "$VERBOSE"

  if [ $? -ne 0 ]; then
    fatalln "Deploying chaincode failed"
  fi
}

## Call the script to deploy a chaincode to the channel
function deployCCAAS() {
  scripts/deployCCAAS.sh "$CHANNEL_NAME" "$CC_NAME" "$CC_SRC_PATH" "$CCAAS_DOCKER_RUN" "$CC_VERSION" "$CC_SEQUENCE" "$CC_INIT_FCN" "$CC_END_POLICY" "$CC_COLL_CONFIG" "$CLI_DELAY" "$MAX_RETRY" "$VERBOSE" "$CCAAS_DOCKER_RUN"

  if [ $? -ne 0 ]; then
    fatalln "Deploying chaincode-as-a-service failed"
  fi
}

## Call the script to package the chaincode
function packageChaincode() {

  infoln "Packaging chaincode"

  scripts/packageCC.sh "$CC_NAME" "$CC_SRC_PATH" "$CC_SRC_LANGUAGE" "$CC_VERSION" true

  if [ $? -ne 0 ]; then
    fatalln "Packaging the chaincode failed"
  fi

}

## Call the script to list installed and committed chaincode on a peer
function listChaincode() {

  # Korrektur: FABRIC_CFG_PATH ist relativ zum aktuellen Skript, nicht zum übergeordneten config
  export FABRIC_CFG_PATH=${ROOTDIR}/config

  . scripts/envVar.sh # Muss separat angepasst werden
  . scripts/ccutils.sh # Muss separat angepasst werden

  # Das Org-Argument muss hier übergeben werden, da wir mehrere haben
  # setGlobals $ORG # Muss angepasst werden, um für die richtige Org zu funktionieren

  println
  # queryInstalledOnPeer # Muss angepasst werden, um für die richtige Org zu funktionieren
  println

  # listAllCommitted # Muss angepasst werden, um für die richtige Org zu funktionieren

  errorln "TODO: listChaincode-Funktion muss für mehrere Orgs angepasst werden."
  exit 1
}

## Call the script to invoke
function invokeChaincode() {

  # Korrektur: FABRIC_CFG_PATH ist relativ zum aktuellen Skript, nicht zum übergeordneten config
  export FABRIC_CFG_PATH=${ROOTDIR}/config

  . scripts/envVar.sh # Muss separat angepasst werden
  . scripts/ccutils.sh # Muss separat angepasst werden

  # Das Org-Argument muss hier übergeben werden, da wir mehrere haben
  # setGlobals $ORG # Muss angepasst werden, um für die richtige Org zu funktionieren

  # chaincodeInvoke $ORG $CHANNEL_NAME $CC_NAME $CC_INVOKE_CONSTRUCTOR # Muss angepasst werden

  errorln "TODO: invokeChaincode-Funktion muss für mehrere Orgs angepasst werden."
  exit 1
}

## Call the script to query chaincode
function queryChaincode() {

  # Korrektur: FABRIC_CFG_PATH ist relativ zum aktuellen Skript, nicht zum übergeordneten config
  export FABRIC_CFG_PATH=${ROOTDIR}/config

  . scripts/envVar.sh # Muss separat angepasst werden
  . scripts/ccutils.sh # Muss separat angepasst werden

  # Das Org-Argument muss hier übergeben werden, da wir mehrere haben
  # setGlobals $ORG # Muss angepasst werden, um für die richtige Org zu funktionieren

  # chaincodeQuery $ORG $CHANNEL_NAME $CC_NAME $CC_QUERY_CONSTRUCTOR # Muss angepasst werden

  errorln "TODO: queryChaincode-Funktion muss für mehrere Orgs angepasst werden."
  exit 1
}


# Tear down running network
function networkDown() {
  infoln "Stopping network and removing volumes..."

  # Kombinieren aller Docker Compose Dateien für den Shutdown des gesamten Netzwerks
  # Hier werden alle Compose-Dateien aufgelistet, die Teil deines Netzwerks sind.
  COMPOSE_FILES_DOWN="-f compose/${COMPOSE_FILE_CA} \
                      -f compose/${COMPOSE_FILE_ORDERER} \
                      -f compose/${COMPOSE_FILE_PEERS}"

  # Führe den Docker Compose Down Befehl aus
  if [ "${CONTAINER_CLI}" == "docker" ]; then
    # DOCKER_SOCK="${DOCKER_SOCK}" ist nur notwendig, wenn der Socket über eine Env-Variable übergeben wird
    ${CONTAINER_CLI_COMPOSE} ${COMPOSE_FILES_DOWN} down --volumes --remove-orphans
  elif [ "${CONTAINER_CLI}" == "podman" ]; then
    ${CONTAINER_CLI_COMPOSE} ${COMPOSE_FILES_DOWN} down --volumes
  else
    fatalln "Container CLI  ${CONTAINER_CLI} not supported"
  fi

  # Bereinigung der restlichen Ressourcen
  # Entferne Chaincode-Container und Images
  clearContainers
  removeUnwantedImages

  # Entferne Orderer-Block, Channel-Konfigurationen und Organisationen
  infoln "Removing generated artifacts (genesis block, channel configs, crypto materials)..."
  ${CONTAINER_CLI} run --rm -v "$(pwd):/data" busybox sh -c 'cd /data && rm -rf system-genesis-block/*.block channel-artifacts organizations/peerOrganizations organizations/ordererOrganizations'

  # Entferne Fabric CA spezifische Artefakte (wenn sie nicht schon im 'organizations' Ordner sind)
  # Annahme: 'fabric-ca' Ordner liegen direkt unter 'test-network/'
  ${CONTAINER_CLI} run --rm -v "$(pwd):/data" busybox sh -c 'cd /data && rm -rf fabric-ca/ordererOrg/* fabric-ca/reg-de/* fabric-ca/reg-fr/* fabric-ca/reg-be/* fabric-ca/reg-es/*'

  # Entferne channel und script artifacts
  ${CONTAINER_CLI} run --rm -v "$(pwd):/data" busybox sh -c 'cd /data && rm -rf log.txt *.tar.gz'

  infoln "Network successfully brought down."
}


# --- Standardkonfigurationen und Variablen ---

# Die folgenden Compose-Dateien müssen in deinem 'compose/' Unterordner liegen
COMPOSE_FILE_CA="docker-compose-ca.yaml"      # Enthält CA-Dienste für alle 5 Orgs
COMPOSE_FILE_ORDERER="docker-compose-orderer.yaml" # Enthält den Orderer-Dienst
COMPOSE_FILE_PEERS="docker-compose-peers.yaml"     # Enthält Peer- und CouchDB-Dienste für alle 4 Orgs

# Standardwerte (können über Kommandozeilen-Flags überschrieben werden)
CHANNEL_NAME="mychannel"
CLI_DELAY="3"
MAX_RETRY="5"
DATABASE="couchdb" # Standard auf couchdb setzen, da wir es verwenden
CRYPTO="Certificate Authorities" # Standard auf Fabric CA setzen

# Get docker sock path from environment variable (oder direkter Pfad)
SOCK="${DOCKER_HOST:-/var/run/docker.sock}"
DOCKER_SOCK="${SOCK##unix://}"

# BFT activated flag (Bleibt so, falls du Raft/BFT nicht explizit umschaltest)
BFT=0


# --- Kommandozeilen-Argument Parsing (unverändert, außer Anpassung der Hilfe) ---

# Parse commandline args
if [[ $# -lt 1 ]] ; then
  printHelp
  exit 0
else
  MODE=$1
  shift
fi

if [ "$MODE" == "cc" ] && [[ $# -lt 1 ]]; then
  printHelp $MODE
  exit 0
fi

if [[ $# -ge 1 ]] ; then
  key="$1"
  if [[ "$key" == "createChannel" ]]; then
      export MODE="createChannel"
      shift
  elif [[ "$MODE" == "cc" ]]; then
    if [ "$1" != "-h" ]; then
      export SUBCOMMAND=$key
      shift
    fi
  fi
fi

while [[ $# -ge 1 ]] ; do
  key="$1"
  case $key in
  -h )
    printHelp $MODE
    exit 0
    ;;
  -c )
    CHANNEL_NAME="$2"
    shift
    ;;
  -bft )
    BFT=1
    ;;
  -ca )
    CRYPTO="Certificate Authorities"
    ;;
  -cfssl )
    CRYPTO="cfssl" # CFSSL-Modus bleibt, falls benötigt
    ;;
  -r )
    MAX_RETRY="$2"
    shift
    ;;
  -d )
    CLI_DELAY="$2"
    shift
    ;;
  -s )
    DATABASE="$2"
    shift
    ;;
  -ccl )
    CC_SRC_LANGUAGE="$2"
    shift
    ;;
  -ccn )
    CC_NAME="$2"
    shift
    ;;
  -ccv )
    CC_VERSION="$2"
    shift
    ;;
  -ccs )
    CC_SEQUENCE="$2"
    shift
    ;;
  -ccp )
    CC_SRC_PATH="$2"
    shift
    ;;
  -ccep )
    CC_END_POLICY="$2"
    shift
    ;;
  -cccg )
    CC_COLL_CONFIG="$2"
    shift
    ;;
  -cci )
    CC_INIT_FCN="$2"
    shift
    ;;
  -ccaasdocker )
    CCAAS_DOCKER_RUN="$2"
    shift
    ;;
  -verbose )
    VERBOSE=true
    ;;
  -org )
    ORG="$2" # Das Org-Flag muss für CLI-Befehle (list, invoke, query) angepasst werden
    shift
    ;;
  -i )
    IMAGETAG="$2"
    shift
    ;;
  -cai )
    CA_IMAGETAG="$2"
    shift
    ;;
  -ccic )
    CC_INVOKE_CONSTRUCTOR="$2"
    shift
    ;;
  -ccqc )
    CC_QUERY_CONSTRUCTOR="$2"
    shift
    ;;
  * )
    errorln "Unknown flag: $key"
    printHelp
    exit 1
    ;;
  esac
  shift
done

# Wenn BFT aktiviert ist, wird der FABRIC_CFG_PATH und die Compose-Datei geändert.
# Beachte: Dein 'configtx' Ordner enthält nur 'configtx.yaml'. Wenn du ein separates
# BFT-Profil hast, muss es in 'bft-config' liegen oder 'FABRIC_CFG_PATH' angepasst werden.
if [ $BFT -eq 1 ]; then
  export FABRIC_CFG_PATH=${PWD}/bft-config # Annahme: 'bft-config' existiert
  COMPOSE_FILE_BASE=compose-bft-test-net.yaml # Muss eine angepasste BFT-Compose-Datei sein
  # Stelle sicher, dass COMPOSE_FILE_ORDERER und COMPOSE_FILE_PEERS auch für BFT-Mode korrekt sind
  COMPOSE_FILE_ORDERER="compose-bft-orderer.yaml" # Beispiel
  COMPOSE_FILE_PEERS="compose-bft-peers.yaml" # Beispiel
fi

# Bestimme den Operationsmodus
if [ "$MODE" == "prereq" ]; then
  infoln "Installing binaries and fabric images. Fabric Version: ${IMAGETAG}  Fabric CA Version: ${CA_IMAGETAG}"
  installPrereqs # Funktion ist in scripts/utils.sh, muss dort existieren
elif [ "$MODE" == "up" ]; then
  infoln "Starting nodes with CLI timeout of '${MAX_RETRY}' tries and CLI delay of '${CLI_DELAY}' seconds and using database '${DATABASE}' ${CRYPTO_MODE}"
  networkUp
elif [ "$MODE" == "createChannel" ]; then
  infoln "Creating channel '${CHANNEL_NAME}'."
  infoln "If network is not up, starting nodes with CLI timeout of '${MAX_RETRY}' tries and CLI delay of '${CLI_DELAY}' seconds and using database '${DATABASE}' ${CRYPTO_MODE}"
  createChannel $BFT
elif [ "$MODE" == "down" ]; then
  infoln "Stopping network"
  networkDown
elif [ "$MODE" == "restart" ]; then
  infoln "Restarting network"
  networkDown
  networkUp
elif [ "$MODE" == "deployCC" ]; then
  infoln "Deploying chaincode on channel '${CHANNEL_NAME}'"
  deployCC
elif [ "$MODE" == "deployCCAAS" ]; then
  infoln "Deploying chaincode-as-a-service on channel '${CHANNEL_NAME}'"
  deployCCAAS
elif [ "$MODE" == "cc" ] && [ "$SUBCOMMAND" == "package" ]; then
  packageChaincode
elif [ "$MODE" == "cc" ] && [ "$SUBCOMMAND" == "list" ]; then
  listChaincode
elif [ "$MODE" == "cc" ] && [ "$SUBCOMMAND" == "invoke" ]; then
  invokeChaincode
elif [ "$MODE" == "cc" ] && [ "$SUBCOMMAND" == "query" ]; then
  queryChaincode
else
  printHelp # Diese Funktion muss in scripts/utils.sh sein
  exit 1
fi
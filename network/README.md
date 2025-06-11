
Die gesamte Logik Ihres Netzwerks wird hauptsächlich an drei Stellen definiert:

#### 1. Die Netzwerk-Topologie (`configtx/configtx.yaml`)
Dies ist der **Master-Bauplan** Ihres Netzwerks.

* **Was es tut:** Definiert, welche Organisationen es gibt, wie der "Ordering Service" heißt und wer zum Konsortium gehört.
* **Was Sie anpassen müssen:**
    * **`Organizations:`** Ändern Sie die Namen `OrdererOrg`, `Org1` und `Org2` in die Namen Ihrer eigenen Organisationen (z.B. `PharmaLinkOrderer`, `PharmaLinkMSP`). Passen Sie die `MSPDir`-Pfade entsprechend an.
    * **`Profiles:`** Im `TwoOrgsApplicationGenesis`-Profil müssen Sie unter `Consortiums` die Namen Ihrer neuen Organisationen eintragen.

#### 2. Die Organisations-Struktur (`organizations/cryptogen/`)
Diese Dateien definieren, wie jede einzelne Organisation aufgebaut ist (z.B. wie viele Peers, wie viele Benutzer).

* **Was es tut:** Erstellt die Vorlagen für das `cryptogen`-Tool, das die gesamten Krypto-Materialien (Zertifikate und Schlüssel) generiert.
* **Was Sie anpassen müssen:**
    * Öffnen Sie z.B. `crypto-config-org1.yaml`.
    * Ändern Sie den `Name:` von `Org1` auf den Namen Ihrer Organisation.
    * Passen Sie unter `Specs:` die Hostnamen an (z.B. `peer0.org1.example.com` wird zu `peer0.pharmalink.de`).

#### 3. Die laufenden Services (`compose/docker-compose-test-net.yaml`)
Dies ist die **Blaupause für Ihre laufenden Docker-Container**.

* **Was es tut:** Definiert jeden einzelnen Peer, Orderer und die Certificate Authority (CA) als Docker-Service, inklusive ihrer Ports, Volumes und Umgebungsvariablen.
* **Was Sie anpassen müssen:**
    * **Service-Namen:** Benennen Sie die Services um (z.B. `peer0.org1.example.com:` wird zu `peer0.pharmalink.de:`).
    * **`environment:`** Passen Sie alle Umgebungsvariablen wie `CORE_PEER_LOCALMSPID` und `CORE_PEER_ADDRESS` an Ihre neuen Organisations- und Peer-Namen an.
    * **`volumes:`** Stellen Sie sicher, dass die Pfade zu den Krypto-Materialien auf die Verzeichnisse zeigen, die in Schritt 2 generiert werden.



# Running the test network

You can use the `./network.sh` script to stand up a simple Fabric test network. The test network has two peer organizations with one peer each and a single node raft ordering service. You can also use the `./network.sh` script to create channels and deploy chaincode. For more information, see [Using the Fabric test network](https://hyperledger-fabric.readthedocs.io/en/latest/test_network.html). The test network is being introduced in Fabric v2.0 as the long term replacement for the `first-network` sample.

If you are planning to run the test network with consensus type BFT then please pass `-bft` flag as input to the `network.sh` script when creating the channel. This sample also supports the use of consensus type BFT and CA together.
That is to create a network use:
```bash
./network.sh up -bft
```

To create a channel use:

```bash
./network.sh createChannel -bft
```

To restart a running network use:

```bash
./network.sh restart -bft
```

Note that running the createChannel command will start the network, if it is not already running.

Before you can deploy the test network, you need to follow the instructions to [Install the Samples, Binaries and Docker Images](https://hyperledger-fabric.readthedocs.io/en/latest/install.html) in the Hyperledger Fabric documentation.

## Using the Peer commands

The `setOrgEnv.sh` script can be used to set up the environment variables for the organizations, this will help to be able to use the `peer` commands directly.

First, ensure that the peer binaries are on your path, and the Fabric Config path is set assuming that you're in the `test-network` directory.

```bash
 export PATH=$PATH:$(realpath ../bin)
 export FABRIC_CFG_PATH=$(realpath ../config)
```

You can then set up the environment variables for each organization. The `./setOrgEnv.sh` command is designed to be run as follows.

```bash
export $(./setOrgEnv.sh Org2 | xargs)
```

(Note bash v4 is required for the scripts.)

You will now be able to run the `peer` commands in the context of Org2. If a different command prompt, you can run the same command with Org1 instead.
The `setOrgEnv` script outputs a series of `<name>=<value>` strings. These can then be fed into the export command for your current shell.

## Chaincode-as-a-service

To learn more about how to use the improvements to the Chaincode-as-a-service please see this [tutorial](./test-network/../CHAINCODE_AS_A_SERVICE_TUTORIAL.md). It is expected that this will move to augment the tutorial in the [Hyperledger Fabric ReadTheDocs](https://hyperledger-fabric.readthedocs.io/en/release-2.4/cc_service.html)


```bash
CONTAINER_CLI=podman ./network.sh up
````

As there is no Docker-Daemon when using podman, only the `./network.sh deployCCAAS` command will work. Following the Chaincode-as-a-service Tutorial above should work. 



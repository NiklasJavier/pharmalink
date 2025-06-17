# Chaincode-Dokumentation: PharmacyChaincode

Dies ist eine Übersicht aller aufrufbaren Transaktionen (Funktionen) des `PharmacyChaincode`. Der Chaincode modelliert eine pharmazeutische Lieferkette mit strenger Governance durch eine Regulierungsbehörde.

## 1. Governance & Stammdaten-Verwaltung

Diese Funktionen bilden das administrative Fundament des Systems. Sie regeln, wer am Netzwerk teilnehmen darf und welche Produkte gehandelt werden können.

### `initLedger`
Initialisiert den Ledger bei der ersten Instanziierung des Chaincodes. Diese Funktion wird nur einmal aufgerufen. Sie erstellt den initialen `behoerde`-Akteur und setzt systeminterne Zähler zurück.

* **Zugriff:** Nur beim Instanziieren des Chaincodes mit dem `--isInit` Flag.
* **Parameter:** Keine.
* **Beispielaufruf (beim Instanziieren):**
    ```sh
    peer chaincode instantiate -o orderer.example.com:7050 -C mychannel -n pharmacy -v 1.0 -c '{"function":"initLedger","Args":[]}' --isInit
    ```

### `requestActorRegistration`
Ein neuer Teilnehmer (Hersteller, Großhändler, Apotheke) stellt einen Antrag auf Aufnahme ins Netzwerk. Sein Status ist initial "Pending".

* **Zugriff:** Jeder neue Teilnehmer mit einem gültigen Zertifikat (außer `behoerde`).
* **Parameter:**
    * `name (String)`: Der offizielle Name des Unternehmens (z.B. "Bayer AG").
* **Beispielaufruf:**
    ```sh
    peer chaincode invoke -o orderer.example.com:7050 -C mychannel -n pharmacy -c '{"function":"requestActorRegistration","Args":["Bayer AG"]}'
    ```

### `approveActor`
Genehmigt einen wartenden Akteur. Nach der Genehmigung erhält der Akteur eine einzigartige, systemgenerierte ID (z.B. `HERSTELLER-001`) und darf am Netzwerk teilnehmen.

* **Zugriff:** Nur Akteure mit der Rolle `behoerde`.
* **Parameter:**
    * `actorMspId (String)`: Die MSP-ID des zu genehmigenden Akteurs.
* **Beispielaufruf:**
    ```sh
    peer chaincode invoke -o orderer.example.com:7050 -C mychannel -n pharmacy -c '{"function":"approveActor","Args":["hersteller-msp-id"]}'
    ```

### `createDrugInfo`
Ein genehmigter Hersteller legt die Stammdaten für ein neues Medikament an. Die ID des Medikaments wird dabei automatisch vom System generiert. Der Status ist initial "NotApproved".

* **Zugriff:** Nur Akteure mit der Rolle `hersteller`, die genehmigt (`Approved`) sind.
* **Parameter:**
    * `name (String)`: Der Name des Medikaments (z.B. "Aspirin 500mg").
    * `description (String)`: Eine kurze Beschreibung.
    * `gtin (String)`: Die 13-stellige Global Trade Item Number. Muss einzigartig sein.
* **Beispielaufruf:**
    ```sh
    peer chaincode invoke -o orderer.example.com:7050 -C mychannel -n pharmacy -c '{"function":"createDrugInfo","Args":["Aspirin 500mg","Schmerz- und Fiebermittel","9123456789012"]}'
    ```

### `approveDrugInfo`
Die Behörde gibt die Stammdaten eines Medikaments frei, sodass Chargen davon produziert werden können.

* **Zugriff:** Nur Akteure mit der Rolle `behoerde`.
* **Parameter:**
    * `drugId (String)`: Die automatisch generierte ID des Medikaments (z.B. `DRUG-tx123...`).
* **Beispielaufruf:**
    ```sh
    peer chaincode invoke -o orderer.example.com:7050 -C mychannel -n pharmacy -c '{"function":"approveDrugInfo","Args":["DRUG-tx123abc"]}'
    ```

## 2. Token Lifecycle & Supply-Chain-Logik

Diese Funktionen steuern die Erzeugung, den Transfer und die grundlegenden Abfragen der einzelnen Medikamenten-Einheiten (als Token modelliert).

### `mintBatch`
Ein genehmigter Hersteller "prägt" (minted) eine neue Charge eines genehmigten Medikaments. Dabei werden für jede Einheit in der Charge fälschungssichere, einzigartige Token auf dem Ledger erstellt.

* **Zugriff:** Nur `hersteller`, die genehmigt sind und der Ersteller des `DrugInfo`-Assets sind.
* **Parameter:**
    * `drugId (String)`: Die ID des (genehmigten) Medikaments.
    * `quantity (long)`: Die Anzahl der zu erstellenden Einheiten.
    * `beschreibung (String)`: Eine Beschreibung für diese Charge (z.B. Produktionsort, -datum).
* **Beispielaufruf:**
    ```sh
    peer chaincode invoke -o orderer.example.com:7050 -C mychannel -n pharmacy -c '{"function":"mintBatch","Args":["DRUG-tx123abc","1000","Produziert in Leverkusen, Charge für EU-Markt"]}'
    ```

### `transferFrom`
Überträgt das Eigentum einer einzelnen Einheit von einem Akteur zum nächsten.

* **Zugriff:** Nur der aktuelle Eigentümer der Einheit.
* **Parameter:**
    * `fromActorId (String)`: Die ID des aktuellen Besitzers (muss mit der ID des Aufrufers übereinstimmen).
    * `toActorId (String)`: Die ID des neuen Besitzers.
    * `unitId (String)`: Die ID der zu übertragenden Einheit.
    * `newState (String)`: Der neue Status der Einheit (z.B. "In-Transit", "Delivered").
* **Beispielaufruf:**
    ```sh
    peer chaincode invoke -o orderer.example.com:7050 -C mychannel -n pharmacy -c '{"function":"transferFrom","Args":["HERSTELLER-001","GROSSHAENDLER-001","BATCH-tx123-00001", "In-Transit"]}'
    ```

### `balanceOf`
Gibt die Anzahl der Einheiten zurück, die ein bestimmter Akteur besitzt.

* **Zugriff:** Jeder Teilnehmer des Netzwerks.
* **Parameter:**
    * `ownerActorId (String)`: Die ID des Akteurs, dessen Besitzstand abgefragt wird.
* **Beispielaufruf:**
    ```sh
    peer chaincode query -C mychannel -n pharmacy -c '{"function":"balanceOf","Args":["HERSTELLER-001"]}'
    ```

### `ownerOf`
Gibt die Besitzer-ID für eine bestimmte Einheit zurück.

* **Zugriff:** Jeder Teilnehmer des Netzwerks.
* **Parameter:**
    * `unitId (String)`: Die ID der Einheit.
* **Beispielaufruf:**
    ```sh
    peer chaincode query -C mychannel -n pharmacy -c '{"function":"ownerOf","Args":["BATCH-tx123-00001"]}'
    ```

## 3. Spezialfunktionen & erweiterte Abfragen

Diese Funktionen decken besondere Anwendungsfälle wie die Intervention durch die Behörde, die Abgabe an Endkunden und komplexe, kombinierte Abfragen ab.

### `tagBatch`
Die Behörde markiert eine ganze Charge mit einem "Tag". Dieser Tag wird automatisch an alle Einheiten dieser Charge weitergegeben.

* **Zugriff:** Nur Akteure mit der Rolle `behoerde`.
* **Parameter:**
    * `batchId (String)`: Die ID der zu markierenden Charge.
    * `tag (String)`: Das zu setzende Tag (z.B. `"sperre"`, `"RECALL"`).
* **Beispielaufruf:**
    ```sh
    peer chaincode invoke -o orderer.example.com:7050 -C mychannel -n pharmacy -c '{"function":"tagBatch","Args":["BATCH-tx123abc","sperre"]}'
    ```

### `dispenseDrugUnit`
Eine genehmigte Apotheke trägt eine Einheit als "an einen Empfänger ausgegeben" ein. Dies ist der letzte Schritt in der Lieferkette. Die Transaktion schlägt fehl, wenn die Einheit z.B. mit `"sperre"` getaggt ist.

* **Zugriff:** Nur `apotheke`, die Besitzer der Einheit ist.
* **Parameter:**
    * `unitId (String)`: Die ID der auszugebenden Einheit.
    * `recipientId (String)`: Eine (anonymisierte) ID des Empfängers.
* **Beispielaufruf:**
    ```sh
    peer chaincode invoke -o orderer.example.com:7050 -C mychannel -n pharmacy -c '{"function":"dispenseDrugUnit","Args":["BATCH-tx123abc-00001","Patient-9876"]}'
    ```

### `queryDrugByGTIN`
Sucht ein Medikament anhand seiner GTIN und gibt die Stammdaten zusammen mit einer Zusammenfassung aller zugehörigen Chargen zurück.

* **Zugriff:** Jeder Teilnehmer des Netzwerks.
* **Parameter:**
    * `gtin (String)`: Die GTIN des Medikaments.
* **Beispielaufruf:**
    ```sh
    peer chaincode query -C mychannel -n pharmacy -c '{"function":"queryDrugByGTIN","Args":["9123456789012"]}'
    ```

### `queryBatchUnits`
Fragt eine Charge ab und liefert eine Liste aller zugehörigen Einheiten.

* **Zugriff:** Jeder Teilnehmer des Netzwerks.
* **Parameter:**
    * `batchId (String)`: Die ID der abzufragenden Charge.
* **Beispielaufruf:**
    ```sh
    peer chaincode query -C mychannel -n pharmacy -c '{"function":"queryBatchUnits","Args":["BATCH-tx123abc"]}'
    ```

### `queryUnitDetails`
Liefert eine vollständige "Top-Down"-Ansicht einer einzelnen Einheit, inklusive der übergeordneten Medikamenten- und Chargen-Informationen sowie der vollständigen Transfer-Historie.

* **Zugriff:** Jeder Teilnehmer des Netzwerks.
* **Parameter:**
    * `unitId (String)`: Die ID der Einheit.
* **Beispielaufruf:**
    ```sh
    peer chaincode query -C mychannel -n pharmacy -c '{"function":"queryUnitDetails","Args":["BATCH-tx123abc-00001"]}'
    ```

### `queryUnitsByTag`
Findet alle Einheiten, die mit einem bestimmten Tag markiert wurden.

* **Zugriff:** Jeder Teilnehmer des Netzwerks.
* **Parameter:**
    * `tag (String)`: Das gesuchte Tag.
* **Beispielaufruf:**
    ```sh
    peer chaincode query -C mychannel -n pharmacy -c '{"function":"queryUnitsByTag","Args":["sperre"]}'
    ```

### `queryDispensedByPharmacy`
Listet alle Einheiten auf, die von einer bestimmten Apotheke ausgegeben wurden.

* **Zugriff:** Nur die abfragende `apotheke` selbst oder die `behoerde`.
* **Parameter:**
    * `apothekenId (String)`: Die ID der Apotheke.
* **Beispielaufruf:**
    ```sh
    peer chaincode query -C mychannel -n pharmacy -c '{"function":"queryDispensedByPharmacy","Args":["APOTHEKE-001"]}'
    ```
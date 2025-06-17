// src/main/java/de/jklein/fabric/samples/assettransfer/PharmaChaincode.java
package de.jklein.fabric.samples.assettransfer;

import de.jklein.fabric.samples.assettransfer.model.Actor;
import de.jklein.fabric.samples.assettransfer.model.Batch; //
import de.jklein.fabric.samples.assettransfer.model.Medication; //
import de.jklein.fabric.samples.assettransfer.model.MedicationUnit;
import de.jklein.fabric.samples.assettransfer.model.RegulatoryAction;
import de.jklein.fabric.samples.assettransfer.model.Shipment;
import de.jklein.fabric.samples.assettransfer.model.Tag;
import de.jklein.fabric.samples.assettransfer.model.TransferCompletedEvent;
import de.jklein.fabric.samples.assettransfer.model.TransferInitiatedEvent; // Import hinzugefügt
import de.jklein.fabric.samples.assettransfer.model.TransferRecord;
import de.jklein.fabric.samples.assettransfer.permission.ContractPermission;
import de.jklein.fabric.samples.assettransfer.permission.RoleConstants; //
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.contract.Context; //
import org.hyperledger.fabric.contract.annotation.Contract; //
import org.hyperledger.fabric.contract.annotation.Default; //
import org.hyperledger.fabric.contract.annotation.Info; //
import org.hyperledger.fabric.contract.annotation.License; //
import org.hyperledger.fabric.contract.annotation.Transaction; //

import java.time.Instant;
import java.util.ArrayList; //
import java.util.Collections;
import java.util.List; //
import java.util.UUID;

@Contract(
        name = "pharma",
        info = @Info(
                title = "Pharma Supply Chain",
                description = "Manages medications, batches, shipments, and regulatory actions",
                version = "1.0.0",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"))) //
@Default //
public final class PharmaChaincode extends BaseContract {

    public PharmaChaincode() {
        super(); // Ruft den Konstruktor der Basisklasse auf, der die Services initialisiert
    }

    /**
     * init ist eine Fabric-spezifische Methode, die zur Initialisierung der Chaincode-Instanz aufgerufen wird.
     * Diese Implementation leitet zur vordefinierten InitLedger-Methode weiter.
     * @param ctx Der Transaktionskontext.
     * @return Ein leerer String als Erfolgsmeldung.
     */
    @Transaction
    public void init(final Context ctx) {
        // In Hyperledger Fabric 2.x ist init optional und wird nur aufgerufen, wenn explizit bei der Chaincode-Instanziierung angegeben
        // Bei Bedarf können hier Initialisierungen durchgeführt werden, die beim Start erforderlich sind
        // Wir delegieren an InitLedger, falls Beispieldaten erforderlich sind
        InitLedger(ctx);
    }

    /**
     * Initialisiert das Ledger mit Beispieldaten.
     * Nur für Demo-Zwecke. In einer Produktionsumgebung wird dies meist entfernt.
     * @param ctx Der Transaktionskontext.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT, name = "InitLedger") //
    public void InitLedger(final Context ctx) { //
        // Beispielhafte Initialisierung von Akteuren
        String adminActorId = ctx.getClientIdentity().getId(); // Annahme: InitLedger wird von einem Admin aufgerufen
        String adminPublicKey = "admin_pk_value"; // Platzhalter für echten Public Key

        // Behörde direkt als APPROVED registrieren
        registerActor(ctx, "RegAuthority1", "Bundesinstitut für Pharmazeutika", RoleConstants.BEHOERDE, adminPublicKey);
        // Hersteller muss erst genehmigt werden
        registerActor(ctx, "PharmaCorp", "Pharma Corp AG", RoleConstants.HERSTELLER, "pharma_pk_123");
        registerActor(ctx, "GrosshaendlerA", "Grosshändler AG A", RoleConstants.GROSSHAENDLER, "gross_pk_abc");
        registerActor(ctx, "ApothekeX", "Apotheke X GmbH", RoleConstants.APOTHEKE, "apo_pk_xyz");

        // Manuelle Genehmigung für PharmaCorp (durch RegAuthority1)
        // In einem echten Szenario müsste InitLedger die Rechte des Anlegers übergeben bekommen
        // oder die Freigabe separat erfolgen. Hier nur zur Initialisierung:
        // (Die 'approveActor' Transaktion sollte nur von einem BEHOERDE-Akteur aufgerufen werden)
        // Für InitLedger müssen wir die Prüfung für den Aufrufer hier umgehen oder davon ausgehen,
        // dass der Init-Aufrufer die Rechte hat.
        // Wir rufen die approveActor Methode direkt aus dem Service auf, um die Berechtigungsprüfung im Contract zu umgehen,
        // da InitLedger als 'admin' ausgeführt wird und alle Rechte haben sollte.
        actorService.approveActorRegistration(ctx, "PharmaCorp");
        actorService.approveActorRegistration(ctx, "GrosshaendlerA");
        actorService.approveActorRegistration(ctx, "ApothekeX");

        // Beispiel-Medikament (von PharmaCorp)
        String pharmaCorpActorId = "PharmaCorp"; // Annahme: Dies ist die ActorId des Herstellers
        Medication med1 = new Medication("MED001", "GTIN001", "Aspirin Complex", "PharmaCorp_Org", pharmaCorpActorId, RoleConstants.ERSTELLT, Collections.emptyList());
        assetService.putAsset(ctx, med1);

        // Medikament FREIGEGEBEN durch eine simulierte Behördenaktion (durch RegAuthority1)
        // Normalerweise eine separate Transaktion über RegulatoryContract
        Medication approvedMed1 = new Medication("MED001", "GTIN001", "Aspirin Complex", "PharmaCorp_Org", pharmaCorpActorId, RoleConstants.FREIGEGEBEN, Collections.emptyList());
        assetService.putAsset(ctx, approvedMed1);


        // Beispiel-Charge (von PharmaCorp)
        Batch batch1 = new Batch("BATCH001", approvedMed1.getKey(), "2025-01-01", "2027-01-01", 100, RoleConstants.ERSTELLT, Collections.emptyList());
        assetService.putAsset(ctx, batch1);

        // Charge FREIGEGEBEN (durch PharmaCorp, der Ersteller des Med.)
        Batch approvedBatch1 = new Batch("BATCH001", approvedMed1.getKey(), "2025-01-01", "2027-01-01", 100, RoleConstants.FREIGEGEBEN, Collections.emptyList());
        assetService.putAsset(ctx, approvedBatch1);


        // Medikamenteneinheiten für BATCH001 generieren (simuliert durch PharmaCorp als Creator)
        // Hier würde normalerweise 'addUnitsToBatch' im MedicationContract aufgerufen
        List<MedicationUnit> createdUnitsForBatch1 = new ArrayList<>();
        for (int i = 0; i < 5; i++) { // 5 Einheiten
            String unitId = "UNIT001-" + UUID.randomUUID().toString();
            MedicationUnit unit = new MedicationUnit(unitId, approvedMed1.getKey(), approvedBatch1.getKey(), pharmaCorpActorId, RoleConstants.FREIGEGEBEN, Collections.emptyList());
            assetService.putAsset(ctx, unit);
            createdUnitsForBatch1.add(unit);
            // Log for unit creation (simplified for InitLedger)
            eventService.logNewEvent(ctx, new TransferInitiatedEvent(
                    UUID.randomUUID().toString(), unit.getKey(), pharmaCorpActorId, pharmaCorpActorId, Instant.now().toString(), ctx.getStub().getTxId()
            ));
        }

        System.out.println("Ledger initialisiert mit Beispieldaten.");
    }

    /**
     * Registriert einen neuen Akteur im System.
     * @param ctx Der Transaktionskontext.
     * @param actorId Die eindeutige ID des Akteurs.
     * @param actorName Der Name des Akteurs/der Organisation.
     * @param roleType Die Rolle des Akteurs (z.B. RoleConstants.HERSTELLER).
     * @param publicKey Der öffentliche Schlüssel des Akteurs-Zertifikats.
     * @return Das neu erstellte Actor-Objekt.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT, name = "registerActor") //
    public Actor registerActor(final Context ctx, final String actorId, final String actorName, final String roleType, final String publicKey) {
        // Keine requireActorApproved() hier, da dies die Registrierungstransaktion ist.
        // Hier könnte man prüfen, ob der actorId des Aufrufers (aus dem Zertifikat) mit dem actorId übereinstimmt,
        // den er registrieren möchte, um Self-Registration zu erzwingen.
        // authService.requireCallerActorIdMatches(ctx, actorId); // z.B. eine neue Methode im AuthService

        return actorService.registerNewActor(ctx, actorId, actorName, roleType, publicKey);
    }

    /**
     * Genehmigt die Registrierung eines Akteurs.
     * Nur von einem BEHOERDE-Akteur aufrufbar.
     * @param ctx Der Transaktionskontext.
     * @param actorId Die ID des zu genehmigenden Akteurs.
     * @return Das aktualisierte Actor-Objekt.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT, name = "approveActor") //
    public Actor approveActor(final Context ctx, final String actorId) {
        // Prüfen, ob der Aufrufer eine Behörde ist und selbst approved.
        // Behörden sind direkt bei Registrierung approved.
        authService.requireActorApproved(ctx);
        authService.requireAnyOfRoles(ctx, RoleConstants.BEHOERDE); //

        return actorService.approveActorRegistration(ctx, actorId);
    }

    // BatchContract Funktionen

    /**
     * Erstellt eine neue Batch (Charge).
     * Regel: Darf nur von einem 'HERSTELLER' aufgerufen werden, dessen Akteur-Status 'APPROVED' ist.
     * Die verknüpfte Medikamentendefinition muss existieren und FREIGEGEBEN sein.
     * @param ctx Der Transaktionskontext.
     * @param batchId Die eindeutige ID der Charge.
     * @param medicationId Die ID des Medikaments, zu dem diese Charge gehört.
     * @param productionDate Das Produktionsdatum.
     * @param expiryDate Das Verfallsdatum.
     * @param quantity Die initiale Menge der Einheiten in dieser Charge.
     * @return Das neu erstellte Batch-Objekt.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT, name = "createBatch") //
    @ContractPermission(roles = {RoleConstants.HERSTELLER}, requireCreator = true)
    public Batch createBatch(final Context ctx, final String batchId, final String medicationId,
                             final String productionDate, final String expiryDate, final int quantity) {

        // 1. Berechtigungsprüfungen: Akteur muss APPROVED Hersteller sein
        authService.requireActorApproved(ctx);
        authService.requireAnyOfRoles(ctx, RoleConstants.HERSTELLER); //

        String batchKey = "BATCH_" + batchId; //
        String medicationKey = "MED_" + medicationId;

        // 2. Prüfen, ob die Batch bereits existiert
        if (assetService.assetExists(ctx, batchKey)) { //
            throw new ChaincodeException("Die Batch " + batchId + " existiert bereits", "ASSET_ALREADY_EXISTS"); //
        }

        // 3. Prüfen, ob das verknüpfte Medikament existiert und FREIGEGEBEN ist
        Medication medication = assetService.getAssetByKey(ctx, medicationKey, Medication.class);
        authService.requireAssetStatus(medicationKey, medication.getStatus(), RoleConstants.FREIGEGEBEN); //
        authService.requireCallerIsCreator(ctx, medication.getCreatorActorId(), medication.getKey()); // Nur der Creator des Medikaments darf Batches dafür erstellen

        // 4. Neue Batch erstellen (initial im Status ERSTELLT)
        Batch batch = new Batch(batchId, medicationKey, productionDate, expiryDate, quantity,
                RoleConstants.ERSTELLT, Collections.emptyList()); //

        // 5. Im Ledger speichern
        assetService.putAsset(ctx, batch); //

        // 6. Event loggen (optional, kann auch in EventService.logNewEvent gekapselt werden)
        eventService.logNewEvent(ctx, new TransferInitiatedEvent(
                UUID.randomUUID().toString(), // Event ID
                batch.getKey(),
                actorService.getCallerActorId(ctx), // Triggering actor
                actorService.getCallerActorId(ctx), // No explicit recipient for batch creation
                Instant.now().toString(),
                ctx.getStub().getTxId()
        ));


        return batch;
    }

    /**
     * Gibt eine Batch frei.
     * Regel: Darf nur vom 'HERSTELLER' aufgerufen werden, der auch der Creator des zugehörigen Medikaments ist.
     * Die Batch muss im Status 'ERSTELLT' sein.
     * @param ctx Der Transaktionskontext.
     * @param batchId Die ID der freizugebenden Charge.
     * @return Das aktualisierte Batch-Objekt.
     * @apiNote Erforderliche Berechtigungen:
     *         - Der Aufrufer muss ein genehmigter Akteur sein (requireActorApproved)
     *         - Der Aufrufer muss die Rolle HERSTELLER haben (requireAnyOfRoles)
     *         - Der Aufrufer muss der Ersteller des zugehörigen Medikaments sein (requireCallerIsCreator)
     *         - Die Charge muss im Status ERSTELLT sein (requireAssetStatus)
     *         - Die Charge darf nicht regulatorisch gesperrt sein (requireAssetNotBlockedByRegulator)
      * @apiNote Erforderliche Berechtigungen:
      *         - Der Aufrufer muss ein genehmigter Akteur sein (requireActorApproved)
      *         - Der Aufrufer muss die Rolle HERSTELLER haben (requireAnyOfRoles)
      *         - Der Aufrufer muss der Ersteller des zugehörigen Medikaments sein (requireCallerIsCreator)
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT, name = "releaseBatch") //
    @ContractPermission(
        roles = {RoleConstants.HERSTELLER},
        requireCreator = true,
        expectedStatus = RoleConstants.ERSTELLT,
        description = "Die Charge darf nicht regulatorisch gesperrt sein"
    )
    public Batch releaseBatch(final Context ctx, final String batchId) {
        // 1. Berechtigungsprüfungen: Akteur muss APPROVED Hersteller sein
        authService.requireActorApproved(ctx);
        authService.requireAnyOfRoles(ctx, RoleConstants.HERSTELLER); //

        String batchKey = "BATCH_" + batchId;
        Batch batch = assetService.getAssetByKey(ctx, batchKey, Batch.class); // Asset abrufen

        Medication medication = assetService.getAssetByKey(ctx, batch.getMedicationKey(), Medication.class);

        // 2. Spezifische Prüfungen:
        //    a) Aufrufer muss der Creator des Medikaments sein
        authService.requireCallerIsCreator(ctx, medication.getCreatorActorId(), medication.getKey());
        //    b) Batch muss im Status 'ERSTELLT' sein
        authService.requireAssetStatus(batchKey, batch.getCurrentBatchStatus(), RoleConstants.ERSTELLT); //
        //    c) Charge darf nicht regulatorisch gesperrt sein
        authService.requireAssetNotBlockedByRegulator(ctx, batchKey);

        // 3. Neues Batch-Objekt mit aktualisiertem Status erstellen (Immutable)
        Batch updatedBatch = new Batch(batch.getBatchId(), batch.getMedicationKey(),
                batch.getProductionDate(), batch.getExpiryDate(), batch.getQuantity(),
                RoleConstants.FREIGEGEBEN, // Status aktualisiert
                batch.getRegulatoryTags());

        // 4. Im Ledger speichern
        assetService.putAsset(ctx, updatedBatch); //

        // 5. Event loggen
        eventService.logNewEvent(ctx, new TransferCompletedEvent(// Could be a more generic EventType
                UUID.randomUUID().toString(), // Event ID
                batchKey,
                medication.getCreatorActorId(), // From (creator)
                medication.getCreatorActorId(), // To (creator still associated)
                Instant.now().toString(),
                ctx.getStub().getTxId()
        ));

        return updatedBatch;
    }

    /**
     * Ruft eine Batch anhand ihrer ID ab.
     * @param ctx Der Transaktionskontext.
     * @param batchId Die ID der Charge.
     * @return Das Batch-Objekt.
     * @apiNote Diese Funktion erfordert keine spezifischen Berechtigungen, da es sich um eine reine Leseoperation handelt.
     *         Daher wird auf requireActorApproved() verzichtet.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE, name = "getBatch") //
    @ContractPermission(requireApproved = false)
    public Batch getBatch(final Context ctx, final String batchId) {
        // Evaluate-Transaktionen brauchen keine requireActorApproved()
        String batchKey = "BATCH_" + batchId; //
        return assetService.getAssetByKey(ctx, batchKey, Batch.class); //
    }

    // RegulatoryContract Funktionen

    /**
     * Erstellt eine neue regulatorische Aktion und wendet sie auf ein Ziel-Asset an (Medikament oder Charge).
     * Regel: Darf nur von einem 'BEHOERDE'-Akteur aufgerufen werden.
     * @param ctx Der Transaktionskontext.
     * @param actionType Die Art der regulatorischen Aktion (z.B. "PRODUKT_SPERRUNG", "PRODUKT_FREIGABE").
     * @param targetAssetKey Der Schlüssel des betroffenen Assets (z.B. "MED_ABC", "BATCH_XYZ").
     * @param actionDescription Eine Beschreibung/Begründung für die Aktion.
     * @param isBlocking True, wenn diese Aktion zu einer Sperrung führt, sonst false.
     * @return Das neu erstellte RegulatoryAction-Objekt.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT, name = "issueRegulatoryAction") //
    public RegulatoryAction issueRegulatoryAction(final Context ctx, final String actionType,
                                                  final String targetAssetKey, final String actionDescription,
                                                  final boolean isBlocking) {
        // 1. Berechtigungsprüfungen: Aufrufer muss APPROVED Behörde sein
        authService.requireActorApproved(ctx);
        authService.requireAnyOfRoles(ctx, RoleConstants.BEHOERDE); //

        String actionId = UUID.randomUUID().toString(); // Eindeutige ID für die Aktion
        String issuingRegulatorActorId = actorService.getCallerActorId(ctx); //
        String actionTimestamp = Instant.now().toString(); //

        // 2. Ziel-Asset prüfen und aktualisieren
        if (!assetService.assetExists(ctx, targetAssetKey)) { //
            throw new ChaincodeException("Ziel-Asset " + targetAssetKey + " existiert nicht", "TARGET_ASSET_NOT_FOUND"); //
        }

        // Apply the action to the target asset (update its status or add tags)
        applyRegulatoryActionToAsset(ctx, targetAssetKey, actionType, isBlocking);

        // 3. Neue RegulatoryAction erstellen
        RegulatoryAction action = new RegulatoryAction(actionId, targetAssetKey, actionType,
                actionDescription, issuingRegulatorActorId, actionTimestamp); //

        // 4. Im Ledger speichern
        assetService.putAsset(ctx, action); //

        // 5. Event loggen
        eventService.logNewEvent(ctx, action); // RegulatoryAction implementiert bereits IEvent und hat alle nötigen Felder

        return action;
    }

    /**
     * Hilfsmethode, die die regulatorische Aktion auf das Ziel-Asset anwendet.
     * Aktualisiert Status oder Tags basierend auf actionType und isBlocking.
     * @param ctx Der Transaktionskontext.
     * @param targetAssetKey Der Schlüssel des Ziel-Assets.
     * @param actionType Der Typ der Aktion.
     * @param isBlocking Gibt an, ob die Aktion eine Blockierung ist.
     */
    private void applyRegulatoryActionToAsset(final Context ctx, final String targetAssetKey, final String actionType, final boolean isBlocking) {
        String currentActorId = actorService.getCallerActorId(ctx);
        String timestamp = Instant.now().toString();

        if (targetAssetKey.startsWith("MED_")) {
            Medication medication = assetService.getAssetByKey(ctx, targetAssetKey, Medication.class);
            List<Tag> updatedTags = new ArrayList<>(medication.getClassificationTags());
            String newStatus = medication.getStatus();

            if (actionType.equalsIgnoreCase("PRODUKT_SPERRUNG")) {
                newStatus = RoleConstants.GESPERRT; //
                updatedTags.add(new Tag("RegulatorischeSperre", "true", currentActorId, timestamp, true));
            } else if (actionType.equalsIgnoreCase("PRODUKT_FREIGABE")) {
                newStatus = RoleConstants.FREIGEGEBEN; //
                // Optional: Entferne blockierende Tags oder füge einen Freigabe-Tag hinzu
                updatedTags.removeIf(Tag::isBlocking); // Entfernt alle blockierenden Tags
                updatedTags.add(new Tag("RegulatorischeFreigabe", "true", currentActorId, timestamp, false));
            }

            Medication updatedMedication = new Medication(
                    medication.getMedicationId(), medication.getGtin(), medication.getProductName(),
                    medication.getProductManufacturerOrgId(), medication.getCreatorActorId(),
                    newStatus, updatedTags
            );
            assetService.putAsset(ctx, updatedMedication);

        } else if (targetAssetKey.startsWith("BATCH_")) {
            Batch batch = assetService.getAssetByKey(ctx, targetAssetKey, Batch.class);
            List<Tag> updatedTags = new ArrayList<>(batch.getRegulatoryTags());
            String newStatus = batch.getCurrentBatchStatus();

            if (actionType.equalsIgnoreCase("PRODUKT_SPERRUNG")) {
                newStatus = RoleConstants.GESPERRT; //
                updatedTags.add(new Tag("ChargenSperre", "true", currentActorId, timestamp, true));
            } else if (actionType.equalsIgnoreCase("PRODUKT_FREIGABE")) {
                newStatus = RoleConstants.FREIGEGEBEN; //
                updatedTags.removeIf(Tag::isBlocking);
                updatedTags.add(new Tag("ChargenFreigabe", "true", currentActorId, timestamp, false));
            }

            Batch updatedBatch = new Batch(
                    batch.getBatchId(), batch.getMedicationKey(), batch.getProductionDate(),
                    batch.getExpiryDate(), batch.getQuantity(), newStatus, updatedTags
            );
            assetService.putAsset(ctx, updatedBatch);

            // Alle zugehörigen Units ebenfalls aktualisieren (optional, aber empfohlen für Konsistenz)
            // Dies würde eine weitere Query erfordern, um alle Units einer Batch zu finden
            // z.B. List<MedicationUnit> units = new MedicationContract(authService, assetService, actorService, eventService).queryUnitsByBatch(ctx, batch.getBatchId());
            // Und dann jede Unit einzeln aktualisieren (Status setzen oder Tags entfernen/hinzufügen)
            // For now, we omit this complex recursive update here for brevity, assuming the AuthorizationService checks batch tags dynamically.

        } else if (targetAssetKey.startsWith("UNIT_")) {
            MedicationUnit unit = assetService.getAssetByKey(ctx, targetAssetKey, MedicationUnit.class);
            String newStatus = unit.getUnitStatus();

            if (actionType.equalsIgnoreCase("UNIT_SPERRUNG")) {
                newStatus = RoleConstants.GESPERRT; //
            } else if (actionType.equalsIgnoreCase("UNIT_FREIGABE")) {
                newStatus = RoleConstants.FREIGEGEBEN; //
            }
            // Hinweis: Unit erbt Tags von Batch. Direkte Tags an Unit könnten auch hier verwaltet werden.
            // Wenn Unit keine eigenen Tags hat, dann ist die Sperrung der Unit nur über den BatchKey relevant.

            MedicationUnit updatedUnit = new MedicationUnit(
                    unit.getUnitId(), unit.getMedicationKey(), unit.getBatchKey(),
                    unit.getCurrentOwnerActorId(), newStatus, unit.getTransferLog()
            );
            assetService.putAsset(ctx, updatedUnit);
        } else {
            throw new ChaincodeException("Unbekannter oder nicht unterstützter Asset-Typ für regulatorische Aktion: " + targetAssetKey, "UNKNOWN_ASSET_TYPE");
        }
    }

    // ShipmentContract Funktionen

    /**
     * Erstellt eine neue Sendung.
     * Regel: Darf nur vom 'currentOwner' der enthaltenen Einheiten initiiert werden.
     * Absender und Empfänger müssen registrierte und APPROVED Akteure sein.
     * @param ctx Der Transaktionskontext.
     * @param shipmentId Die eindeutige ID der Sendung.
     * @param receiverActorId Die Akteur-ID des Empfängers.
     * @param medicationUnitKeys Eine Liste der Keys der zu versendenden MedicationUnits.
     * @param expectedDeliveryDate Das erwartete Lieferdatum.
     * @return Das neu erstellte Shipment-Objekt.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT, name = "createShipment") //
    public Shipment createShipment(final Context ctx, final String shipmentId, final String receiverActorId,
                                   final List<String> medicationUnitKeys, final String expectedDeliveryDate) {
        // 1. Berechtigungsprüfungen: Aufrufer muss APPROVED sein
        authService.requireActorApproved(ctx);

        String shipmentKey = "SHIP_" + shipmentId; //

        // 2. Prüfen, ob die Sendung bereits existiert
        if (assetService.assetExists(ctx, shipmentKey)) { //
            throw new ChaincodeException("Sendung " + shipmentId + " existiert bereits", "ASSET_ALREADY_EXISTS"); //
        }

        String shipperActorId = actorService.getCallerActorId(ctx); // Absender ist der Aufrufer

        // 3. Prüfen des Empfängers
        if (!actorService.isActorCurrentlyApproved(ctx, receiverActorId)) { //
            throw new ChaincodeException(String.format("Empfänger-Akteur '%s' ist nicht 'APPROVED'.", receiverActorId), "RECIPIENT_NOT_APPROVED"); //
        }

        // 4. Prüfen der Medikamenteneinheiten
        if (medicationUnitKeys == null || medicationUnitKeys.isEmpty()) {
            throw new ChaincodeException("Keine Medikamenteneinheiten für die Sendung angegeben.", "NO_UNITS_IN_SHIPMENT");
        }

        for (String unitKey : medicationUnitKeys) {
            MedicationUnit unit = assetService.getAssetByKey(ctx, unitKey, MedicationUnit.class);
            // a) Absender muss der aktuelle Besitzer jeder Einheit sein
            authService.requireCallerIsCurrentOwner(ctx, unit.getCurrentOwnerActorId(), unitKey); //
            // b) Einheit darf nicht im PENDING_TRANSFER Status sein (wird bei Shipment-Erstellung nicht direkt transferiert)
            authService.requireAssetNotStatus(unitKey, unit.getUnitStatus(), RoleConstants.PENDING_TRANSFER);
            // c) Einheit muss FREIGEGEBEN sein
            authService.requireAssetStatus(unitKey, unit.getUnitStatus(), RoleConstants.FREIGEGEBEN);
            // d) Einheit darf nicht regulatorisch gesperrt sein
            authService.requireAssetNotBlockedByRegulator(ctx, unit.getBatchKey()); // Regulatorische Sperre der Batch prüfen
        }

        // 5. Neue Sendung erstellen
        Shipment.ShipmentInfo info = new Shipment.ShipmentInfo(shipmentId, shipperActorId, receiverActorId,
                medicationUnitKeys, expectedDeliveryDate);
        Shipment shipment = Shipment.create(info, Instant.now().toString(), "INITIATED"); // Initialer Status "INITIATED"

        // 6. Im Ledger speichern
        assetService.putAsset(ctx, shipment); //

        // 7. Event loggen (optional)
        eventService.logNewEvent(ctx, new TransferInitiatedEvent(// könnte auch ein ShipmentCreatedEvent sein
                UUID.randomUUID().toString(),
                shipmentKey,
                shipperActorId,
                receiverActorId,
                Instant.now().toString(),
                ctx.getStub().getTxId()
        ));

        return shipment;
    }

    /**
     * Ruft eine Sendung anhand ihrer ID ab.
     * @param ctx Der Transaktionskontext.
     * @param shipmentId Die ID der Sendung.
     * @return Das Shipment-Objekt.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE, name = "getShipment") //
    public Shipment getShipment(final Context ctx, final String shipmentId) {
        // Evaluate-Transaktionen brauchen keine requireActorApproved()
        String shipmentKey = "SHIP_" + shipmentId; //
        return assetService.getAssetByKey(ctx, shipmentKey, Shipment.class); //
    }

    // MedicationContract Funktionen

    /**
     * Erstellt ein neues Medikament (Produktdefinition).
     * Regel: Darf nur von einem 'HERSTELLER' aufgerufen werden, dessen Akteur-Status 'APPROVED' ist.
     * Der Aufrufer wird zum Creator.
     * @param ctx Der Transaktionskontext.
     * @param medicationId Die eindeutige ID des Medikaments.
     * @param gtin Die GTIN des Medikaments.
     * @param productName Der Name des Medikaments.
     * @param productManufacturerOrgId Die MSP-ID der Hersteller-Organisation (optional, kann auch aus ClientIdentity kommen).
     * @return Das neu erstellte Medication-Objekt.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT, name = "createMedication") //
    public Medication createMedication(final Context ctx, final String medicationId, final String gtin,
                                       final String productName, final String productManufacturerOrgId) {
        // 1. Berechtigungsprüfungen: Akteur muss APPROVED Hersteller sein
        authService.requireActorApproved(ctx);
        authService.requireAnyOfRoles(ctx, RoleConstants.HERSTELLER); //

        String medicationKey = "MED_" + medicationId; //

        // 2. Prüfen, ob das Medikament bereits existiert
        if (assetService.assetExists(ctx, medicationKey)) { //
            throw new ChaincodeException("Das Medikament " + medicationId + " existiert bereits", "ASSET_ALREADY_EXISTS"); //
        }

        // 3. Medikament erstellen (initial mit Status ERSTELLT)
        String creatorActorId = actorService.getCallerActorId(ctx); //
        Medication medication = new Medication(medicationId, gtin, productName, productManufacturerOrgId,
                creatorActorId, RoleConstants.ERSTELLT, Collections.emptyList()); //

        // 4. Im Ledger speichern
        assetService.putAsset(ctx, medication); //

        // 5. Event loggen
        eventService.logNewEvent(ctx, new TransferInitiatedEvent(
                UUID.randomUUID().toString(), // Event ID
                medication.getKey(),
                creatorActorId, // Triggering actor
                creatorActorId, // Initial creator/owner
                Instant.now().toString(),
                ctx.getStub().getTxId()
        ));

        return medication;
    }

    /**
     * Aktualisiert den Status eines Medikaments und gibt es frei (falls der Ersteller es tut).
     * Regel: Darf nur vom 'HERSTELLER' aufgerufen werden, der auch der Creator ist.
     * Setzt den Status auf FREIGEGEBEN.
     * @param ctx Der Transaktionskontext.
     * @param medicationId Die ID des Medikaments.
     * @param newStatus Der neue Status (z.B. RoleConstants.FREIGEGEBEN).
     * @return Das aktualisierte Medication-Objekt.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT, name = "updateMedicationStatus")
    public Medication updateMedicationStatus(final Context ctx, final String medicationId, final String newStatus) {
        // 1. Berechtigungsprüfungen: Akteur muss APPROVED Hersteller sein
        authService.requireActorApproved(ctx);
        authService.requireAnyOfRoles(ctx, RoleConstants.HERSTELLER);

        String medicationKey = "MED_" + medicationId;
        Medication medication = assetService.getAssetByKey(ctx, medicationKey, Medication.class);

        // 2. Spezifische Prüfungen:
        //    a) Aufrufer muss der Creator des Medikaments sein
        authService.requireCallerIsCreator(ctx, medication.getCreatorActorId(), medicationKey);
        //    b) Medikament darf nicht regulatorisch gesperrt sein
        authService.requireAssetNotBlockedByRegulator(ctx, medicationKey);

        // Wenn der Status nicht zulässig ist, wird eine Exception geworfen
        if (!RoleConstants.isValidStatus(newStatus)) {
            throw new ChaincodeException(String.format("Der Status '%s' ist ungültig.", newStatus), "INVALID_STATUS");
        }

        // 3. Neues Medikament-Objekt mit aktualisiertem Status erstellen (Immutable)
        Medication updatedMedication = new Medication(
                medication.getMedicationId(), medication.getGtin(), medication.getProductName(),
                medication.getProductManufacturerOrgId(), medication.getCreatorActorId(),
                newStatus, medication.getClassificationTags());

        // 4. Im Ledger speichern
        assetService.putAsset(ctx, updatedMedication);

        return updatedMedication;
    }

    /**
     * Fügt Einheiten eines Medikaments aus einer Charge hinzu.
     * Jede Einheit bekommt eine eindeutige ID und erbt Informationen.
     * Regel: Darf nur vom 'HERSTELLER' aufgerufen werden, der auch der Creator des Medikaments ist.
     * Das Medikament muss den Status 'FREIGEGEBEN' haben.
     * @param ctx Der Transaktionskontext.
     * @param batchKey Der Schlüssel der betroffenen Charge (z.B. "BATCH001").
     * @param quantity Die Anzahl der hinzuzufügenden Einheiten.
     * @return Eine Liste der neu erstellten MedicationUnit-Objekte.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT, name = "addUnitsToBatch") //
    public List<MedicationUnit> addUnitsToBatch(final Context ctx, final String batchKey, final int quantity) {
        // 1. Berechtigungsprüfungen: Akteur muss APPROVED Hersteller sein
        authService.requireActorApproved(ctx);
        authService.requireAnyOfRoles(ctx, RoleConstants.HERSTELLER);

        String fullBatchKey = batchKey.startsWith("BATCH_") ? batchKey : "BATCH_" + batchKey;

        // 2. Prüfen, ob die Batch existiert und FREIGEGEBEN ist
        Batch batch = assetService.getAssetByKey(ctx, fullBatchKey, Batch.class);
        Medication medication = assetService.getAssetByKey(ctx, batch.getMedicationKey(), Medication.class);

        // 3. Spezifische Prüfungen:
        //    a) Aufrufer muss der Creator des Medikaments sein
        authService.requireCallerIsCreator(ctx, medication.getCreatorActorId(), medication.getKey());
        //    b) Medikament muss FREIGEGEBEN sein
        authService.requireAssetStatus(medication.getKey(), medication.getStatus(), RoleConstants.FREIGEGEBEN);
        //    c) Batch muss FREIGEGEBEN sein
        authService.requireAssetStatus(fullBatchKey, batch.getCurrentBatchStatus(), RoleConstants.FREIGEGEBEN);
        //    d) Medikament und Batch dürfen nicht regulatorisch gesperrt sein
        authService.requireAssetNotBlockedByRegulator(ctx, medication.getKey());
        authService.requireAssetNotBlockedByRegulator(ctx, fullBatchKey);
        //    e) Menge muss positiv sein
        if (quantity <= 0) {
            throw new ChaincodeException("Die Anzahl der Einheiten muss größer als 0 sein.", "INVALID_QUANTITY");
        }

        // 4. Units erstellen
        List<MedicationUnit> createdUnits = new ArrayList<>();
        String callerActorId = actorService.getCallerActorId(ctx);

        for (int i = 0; i < quantity; i++) {
            String unitId = String.format("%s-%s-%d", batch.getBatchId(), UUID.randomUUID().toString().substring(0, 8), i);
            String unitKey = "UNIT_" + unitId;

            // 4.1 Neue Unit erstellen (Status FREIGEGEBEN, da die Batch FREIGEGEBEN ist)
            MedicationUnit unit = new MedicationUnit(unitId, medication.getKey(), fullBatchKey,
                    callerActorId, RoleConstants.FREIGEGEBEN, Collections.emptyList());

            // 4.2 Im Ledger speichern
            assetService.putAsset(ctx, unit);

            // 4.3 Zur Rückgabeliste hinzufügen
            createdUnits.add(unit);

            // 4.4 Event loggen
            eventService.logNewEvent(ctx, new TransferInitiatedEvent(
                    UUID.randomUUID().toString(), // Event ID
                    unitKey,
                    callerActorId,
                    callerActorId, // Selbst-Transfer (initial)
                    Instant.now().toString(),
                    ctx.getStub().getTxId()
            ));
        }

        return createdUnits;
    }

    /**
     * Initiiert den Transfer einer Medikamenteneinheit an einen neuen Empfänger.
     * Dies ist der erste Schritt des Zwei-Phasen-Transfers.
     * Regel: Nur vom 'currentOwner' aufrufbar. Asset muss FREIGEGEBEN und nicht gesperrt sein.
     * Setzt den Status der Einheit auf PENDING_TRANSFER.
     * @param ctx Der Transaktionskontext.
     * @param unitId Die ID der zu transferierenden Einheit.
     * @param intendedRecipientActorId Die Akteur-ID des beabsichtigten Empfängers.
     * @return Das aktualisierte MedicationUnit-Objekt im Status PENDING_TRANSFER.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT, name = "initiateUnitTransfer")
    public MedicationUnit initiateUnitTransfer(final Context ctx, final String unitId, final String intendedRecipientActorId) {
        // 1. Berechtigungsprüfungen: Akteur muss APPROVED sein
        authService.requireActorApproved(ctx);

        String unitKey = unitId.startsWith("UNIT_") ? unitId : "UNIT_" + unitId;

        // 2. Prüfen, ob die Unit existiert
        MedicationUnit unit = assetService.getAssetByKey(ctx, unitKey, MedicationUnit.class);

        // 3. Spezifische Prüfungen:
        //    a) Aufrufer muss der currentOwner der Einheit sein
        authService.requireCallerIsCurrentOwner(ctx, unit.getCurrentOwnerActorId(), unitKey);
        //    b) Einheit muss FREIGEGEBEN sein
        authService.requireAssetStatus(unitKey, unit.getUnitStatus(), RoleConstants.FREIGEGEBEN);
        //    c) Einheit darf nicht regulatorisch gesperrt sein
        authService.requireAssetNotBlockedByRegulator(ctx, unit.getBatchKey());
        //    d) Empfänger muss existieren und APPROVED sein
        if (!actorService.isActorCurrentlyApproved(ctx, intendedRecipientActorId)) {
            throw new ChaincodeException(String.format("Empfänger-Akteur '%s' ist nicht 'APPROVED'.", intendedRecipientActorId), "RECIPIENT_NOT_APPROVED");
        }

        // 4. TransferRecord erstellen und zur Unit hinzufügen
        List<TransferRecord> updatedTransferLog = new ArrayList<>(unit.getTransferLog());
        TransferRecord newTransferRecord = new TransferRecord(
                unit.getCurrentOwnerActorId(), // Absender
                intendedRecipientActorId, // Empfänger
                Instant.now().toString(), // Zeitstempel
                "INITIATED", // Status
                ctx.getStub().getTxId() // Transaction ID
        );
        updatedTransferLog.add(newTransferRecord);

        // 5. Unit-Status aktualisieren auf PENDING_TRANSFER
        MedicationUnit updatedUnit = new MedicationUnit(
                unit.getUnitId(), unit.getMedicationKey(), unit.getBatchKey(),
                unit.getCurrentOwnerActorId(), // Besitzer bleibt bis zur Bestätigung
                RoleConstants.PENDING_TRANSFER, // Status auf "pending" setzen
                updatedTransferLog
        );

        // 6. Im Ledger speichern
        assetService.putAsset(ctx, updatedUnit);

        // 7. Event loggen
        eventService.logNewEvent(ctx, new TransferInitiatedEvent(
                UUID.randomUUID().toString(), // Event ID
                unitKey,
                unit.getCurrentOwnerActorId(),
                intendedRecipientActorId,
                Instant.now().toString(),
                ctx.getStub().getTxId()
        ));

        return updatedUnit;
    }

    /**
     * Bestätigt den Transfer einer Medikamenteneinheit durch den Empfänger.
     * Dies ist der zweite Schritt des Zwei-Phasen-Transfers.
     * Regel: Nur vom 'intendedRecipient' aufrufbar. Einheit muss im Status 'PENDING_TRANSFER' sein.
     * Aktualisiert den Besitzer und den Status der Einheit.
     * @param ctx Der Transaktionskontext.
     * @param unitId Die ID der zu bestätigenden Einheit.
     * @return Das aktualisierte MedicationUnit-Objekt mit neuem Besitzer.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT, name = "confirmUnitTransfer")
    public MedicationUnit confirmUnitTransfer(final Context ctx, final String unitId) {
        // 1. Berechtigungsprüfungen: Akteur muss APPROVED sein
        authService.requireActorApproved(ctx);

        String unitKey = unitId.startsWith("UNIT_") ? unitId : "UNIT_" + unitId;

        // 2. Prüfen, ob die Unit existiert
        MedicationUnit unit = assetService.getAssetByKey(ctx, unitKey, MedicationUnit.class);

        // 3. Spezifische Prüfungen:
        //    a) Einheit muss im Status PENDING_TRANSFER sein
        authService.requireAssetStatus(unitKey, unit.getUnitStatus(), RoleConstants.PENDING_TRANSFER);
        //    b) Einheit darf nicht regulatorisch gesperrt sein
        authService.requireAssetNotBlockedByRegulator(ctx, unit.getBatchKey());

        // 4. Prüfen, ob der Aufrufer der beabsichtigte Empfänger ist
        String callerActorId = actorService.getCallerActorId(ctx);
        String intendedRecipientId = getLatestIntendedRecipientFromLog(unit.getTransferLog());

        if (!callerActorId.equals(intendedRecipientId)) {
            throw new ChaincodeException("Nur der beabsichtigte Empfänger kann die Übertragung bestätigen.", "NOT_INTENDED_RECIPIENT");
        }

        // 5. TransferLog aktualisieren
        List<TransferRecord> updatedTransferLog = new ArrayList<>(unit.getTransferLog());
        // Den letzten Eintrag aktualisieren
        if (!updatedTransferLog.isEmpty()) {
            TransferRecord lastRecord = updatedTransferLog.get(updatedTransferLog.size() - 1);
            // Nur aktualisieren, wenn es ein INITIATED Eintrag ist
            if ("INITIATED".equals(lastRecord.getStatus())) {
                TransferRecord completedRecord = new TransferRecord(
                        lastRecord.getSenderActorId(),
                        lastRecord.getReceiverActorId(),
                        Instant.now().toString(), // Aktualisierter Zeitstempel
                        "COMPLETED", // Status auf "completed" setzen
                        ctx.getStub().getTxId() // Neue Transaction ID
                );
                updatedTransferLog.set(updatedTransferLog.size() - 1, completedRecord);
            }
        }

        // 6. Unit aktualisieren
        MedicationUnit updatedUnit = new MedicationUnit(
                unit.getUnitId(), unit.getMedicationKey(), unit.getBatchKey(),
                callerActorId, // Besitzer auf Empfänger ändern
                RoleConstants.FREIGEGEBEN, // Status auf "released" zurücksetzen
                updatedTransferLog
        );

        // 7. Im Ledger speichern
        assetService.putAsset(ctx, updatedUnit);

        // 8. Event loggen
        eventService.logNewEvent(ctx, new TransferCompletedEvent(
                UUID.randomUUID().toString(), // Event ID
                unitKey,
                unit.getCurrentOwnerActorId(), // Vorheriger Besitzer
                callerActorId, // Neuer Besitzer
                Instant.now().toString(),
                ctx.getStub().getTxId()
        ));

        return updatedUnit;
    }

    /**
     * Ruft ein Medikament anhand seiner ID ab.
     * @param ctx Der Transaktionskontext.
     * @param medicationId Die ID des Medikaments.
     * @return Das Medication-Objekt.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE, name = "getMedication") //
    public Medication getMedication(final Context ctx, final String medicationId) {
        // Evaluate-Transaktionen brauchen keine requireActorApproved()
        String medicationKey = "MED_" + medicationId; //
        return assetService.getAssetByKey(ctx, medicationKey, Medication.class); //
    }

    /**
     * Ruft eine Medikamenteneinheit anhand ihrer ID ab.
     * @param ctx Der Transaktionskontext.
     * @param unitId Die ID der Einheit.
     * @return Das MedicationUnit-Objekt.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE, name = "getMedicationUnit") //
    public MedicationUnit getMedicationUnit(final Context ctx, final String unitId) {
        // Evaluate-Transaktionen brauchen keine requireActorApproved()
        String unitKey = "UNIT_" + unitId; //
        return assetService.getAssetByKey(ctx, unitKey, MedicationUnit.class); //
    }


    // --- Hilfsmethoden ---

    // Hilfsmethode, um den beabsichtigten Empfänger aus dem Log zu holen
    private String getLatestIntendedRecipientFromLog(final List<TransferRecord> transferLog) {
        if (transferLog == null || transferLog.isEmpty()) {
            return null;
        }
        // Hole den letzten Eintrag und prüfe, ob er den Status "INITIATED" hat
        TransferRecord lastRecord = transferLog.get(transferLog.size() - 1);
        if ("INITIATED".equals(lastRecord.getStatus())) {
            return lastRecord.getReceiverActorId();
        }
        return null;
    }
}

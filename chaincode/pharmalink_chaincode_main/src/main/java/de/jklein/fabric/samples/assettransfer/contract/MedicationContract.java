// src/main/java/de/jklein/fabric/samples/assettransfer/contract/MedicationContract.java
package de.jklein.fabric.samples.assettransfer.contract;

import de.jklein.fabric.samples.assettransfer.BaseContract;
import de.jklein.fabric.samples.assettransfer.model.Actor;
import de.jklein.fabric.samples.assettransfer.model.Batch;
import de.jklein.fabric.samples.assettransfer.model.Medication;
import de.jklein.fabric.samples.assettransfer.model.MedicationUnit;
import de.jklein.fabric.samples.assettransfer.model.TransferCompletedEvent;
import de.jklein.fabric.samples.assettransfer.model.TransferInitiatedEvent;
import de.jklein.fabric.samples.assettransfer.model.TransferRecord;
import de.jklein.fabric.samples.assettransfer.permission.RoleConstants; //
import org.hyperledger.fabric.contract.Context; //
import org.hyperledger.fabric.contract.annotation.Contract; //
import org.hyperledger.fabric.contract.annotation.Transaction; //
import org.hyperledger.fabric.shim.ChaincodeException; //

import java.time.Instant;
import java.util.ArrayList; //
import java.util.Collections;
import java.util.List; //
import java.util.UUID; //

/**
 * Smart Contract zur Verwaltung von Medikamenten (Produktdefinitionen) und Medikamenteneinheiten in der Lieferkette.
 */
@Contract(name = "MedicationContract") //
public final class MedicationContract extends BaseContract {

    public MedicationContract() {
        super(); // Initialisiert die Services aus BaseContract
    }

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
    @Transaction(intent = Transaction.TYPE.SUBMIT) //
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

        // 3. Akteur-ID des Erstellers aus der Client Identity abrufen
        String creatorActorId = actorService.getCallerActorId(ctx);

        // 4. Neues Medikament erstellen (initial im Status ERSTELLT)
        Medication medication = new Medication(medicationId, gtin, productName, productManufacturerOrgId,
                creatorActorId, RoleConstants.ERSTELLT, Collections.emptyList()); //

        // 5. Im Ledger speichern
        assetService.putAsset(ctx, medication); //

        // 6. Event loggen
        eventService.logNewEvent(ctx, new TransferInitiatedEvent(
                UUID.randomUUID().toString(), // Event ID
                medicationKey,
                creatorActorId, // Initiated by (creator)
                creatorActorId, // Intended recipient (creator initially owns)
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
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Medication updateMedicationStatus(final Context ctx, final String medicationId, final String newStatus) {
        authService.requireActorApproved(ctx);
        Medication medication = assetService.getAssetByKey(ctx, "MED_" + medicationId, Medication.class); //

        // Nur der Ersteller darf den Status ändern (hier: MANUFACTURER = CREATOR)
        authService.requireAnyOfRoles(ctx, RoleConstants.HERSTELLER);
        authService.requireCallerIsCreator(ctx, medication.getCreatorActorId(), medication.getKey());

        // Statusprüfung: Nur von ERSTELLT auf FREIGEGEBEN (oder andere erlaubte Übergänge)
        authService.requireAssetNotStatus(medication.getKey(), medication.getStatus(), RoleConstants.GESPERRT); //
        // Weitere spezifische Statusübergänge hier prüfen, z.B. nur von ERSTELLT zu FREIGEGEBEN
        if (!medication.getStatus().equals(RoleConstants.ERSTELLT) && newStatus.equals(RoleConstants.FREIGEGEBEN)) {
            throw new ChaincodeException(String.format("Medikament %s kann nicht von Status '%s' auf '%s' gesetzt werden.", medication.getKey(), medication.getStatus(), newStatus), "INVALID_STATUS_TRANSITION");
        }


        // Neues Medication-Objekt mit aktualisiertem Status erstellen (Immutable)
        Medication updatedMedication = new Medication(medication.getMedicationId(), medication.getGtin(),
                medication.getProductName(), medication.getProductManufacturerOrgId(),
                medication.getCreatorActorId(), newStatus, medication.getClassificationTags());

        assetService.putAsset(ctx, updatedMedication); //

        // Event loggen
        eventService.logNewEvent(ctx, new TransferCompletedEvent(
                UUID.randomUUID().toString(), // Event ID
                medication.getKey(),
                medication.getCreatorActorId(), // From (current creator)
                medication.getCreatorActorId(), // To (creator still owns "definition")
                Instant.now().toString(),
                ctx.getStub().getTxId()
        ));


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
    @Transaction(intent = Transaction.TYPE.SUBMIT) //
    public List<MedicationUnit> addUnitsToBatch(final Context ctx, final String batchKey, final int quantity) {
        // 1. Berechtigungsprüfungen: Akteur muss APPROVED Hersteller sein
        authService.requireActorApproved(ctx);
        authService.requireAnyOfRoles(ctx, RoleConstants.HERSTELLER); //

        // 2. Batch und Medikament abrufen
        Batch batch = assetService.getAssetByKey(ctx, batchKey, Batch.class);
        Medication medication = assetService.getAssetByKey(ctx, batch.getMedicationKey(), Medication.class);

        // 3. Zusätzliche Prüfungen:
        //    a) Aufrufer muss der Creator des Medikaments sein
        authService.requireCallerIsCreator(ctx, medication.getCreatorActorId(), medication.getKey());
        //    b) Medikament muss im Status 'FREIGEGEBEN' sein
        authService.requireAssetStatus(medication.getKey(), medication.getStatus(), RoleConstants.FREIGEGEBEN); //
        //    c) Charge darf nicht regulatorisch gesperrt sein
        authService.requireAssetNotBlockedByRegulator(ctx, batch.getKey());
        //    d) Die Charge selbst muss FREIGEGEBEN sein
        authService.requireAssetStatus(batch.getKey(), batch.getCurrentBatchStatus(), RoleConstants.FREIGEGEBEN);


        List<MedicationUnit> createdUnits = new ArrayList<>();
        String currentOwnerActorId = actorService.getCallerActorId(ctx); // Initialer Besitzer der Einheiten ist der aufrufende Hersteller

        // 4. Einheiten erstellen und im Ledger speichern
        for (int i = 0; i < quantity; i++) {
            String unitId = UUID.randomUUID().toString(); // Eindeutige ID für jede Einheit
            MedicationUnit unit = new MedicationUnit(
                    unitId,
                    medication.getKey(),
                    batch.getKey(),
                    currentOwnerActorId,
                    RoleConstants.FREIGEGEBEN, // Einheiten erben den Status "Freigegeben" bei Erstellung
                    Collections.emptyList() // Initial kein TransferLog
            );

            assetService.putAsset(ctx, unit); //
            createdUnits.add(unit);

            // 5. Event loggen für jede generierte Einheit
            eventService.logNewEvent(ctx, new TransferRecord(
                    UUID.randomUUID().toString(), // Event ID
                    unit.getKey(),
                    null, // Von niemandem (initial erstellt)
                    currentOwnerActorId, // Zu (initialer Besitzer)
                    Instant.now().toString(),
                    ctx.getStub().getTxId(),
                    "CREATED" // Spezifische Phase für Erstellung
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
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public MedicationUnit initiateUnitTransfer(final Context ctx, final String unitId, final String intendedRecipientActorId) {
        // 1. Berechtigungsprüfungen: Aufrufer muss APPROVED sein
        authService.requireActorApproved(ctx);

        String unitKey = "UNIT_" + unitId;
        MedicationUnit unit = assetService.getAssetByKey(ctx, unitKey, MedicationUnit.class); // Einheit abrufen

        // 2. Spezifische Berechtigungen für Transfer-Initiator:
        //    a) Aufrufer muss der aktuelle Besitzer sein
        authService.requireCallerIsCurrentOwner(ctx, unit.getCurrentOwnerActorId(), unitKey); //
        //    b) Asset muss FREIGEGEBEN sein
        authService.requireAssetStatus(unitKey, unit.getUnitStatus(), RoleConstants.FREIGEGEBEN); //
        //    c) Medikament und Charge dürfen nicht regulatorisch gesperrt sein
        Medication medication = assetService.getAssetByKey(ctx, unit.getMedicationKey(), Medication.class);
        authService.requireAssetNotBlockedByRegulator(ctx, medication.getKey());
        Batch batch = assetService.getAssetByKey(ctx, unit.getBatchKey(), Batch.class);
        authService.requireAssetNotBlockedByRegulator(ctx, batch.getKey());

        // 3. Empfänger muss registrierter und APPROVED Akteur sein
        Actor recipientActor = actorService.getActorById(ctx, intendedRecipientActorId);
        if (!recipientActor.isApproved()) {
            throw new ChaincodeException(String.format("Empfänger-Akteur '%s' ist nicht 'APPROVED' und kann keine Transfers empfangen.", intendedRecipientActorId), "RECIPIENT_NOT_APPROVED");
        }

        // 4. Status der Einheit auf PENDING_TRANSFER setzen (Immutable-Pattern)
        MedicationUnit updatedUnit = new MedicationUnit(
                unit.getUnitId(),
                unit.getMedicationKey(),
                unit.getBatchKey(),
                unit.getCurrentOwnerActorId(), // Besitzer bleibt vorerst der Initiator
                RoleConstants.PENDING_TRANSFER, // NEU: Temporärer Status
                unit.getTransferLog() // Transfer-Log wird erst bei Bestätigung aktualisiert
        );

        assetService.putAsset(ctx, updatedUnit); // Einheit im Ledger aktualisieren

        // 5. Event loggen
        eventService.logNewEvent(ctx, new TransferInitiatedEvent(
                UUID.randomUUID().toString(), // Event ID
                unitKey,
                unit.getCurrentOwnerActorId(), // Initiator
                intendedRecipientActorId, // Beabsichtigter Empfänger
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
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public MedicationUnit confirmUnitTransfer(final Context ctx, final String unitId) {
        // 1. Berechtigungsprüfungen: Aufrufer muss APPROVED sein
        authService.requireActorApproved(ctx);

        String unitKey = "UNIT_" + unitId;
        MedicationUnit unit = assetService.getAssetByKey(ctx, unitKey, MedicationUnit.class); // Einheit abrufen

        // 2. Spezifische Prüfungen für Empfänger-Bestätigung:
        //    a) Einheit muss im Status PENDING_TRANSFER sein
        authService.requireAssetStatus(unitKey, unit.getUnitStatus(), RoleConstants.PENDING_TRANSFER);
        //    b) Aufrufer muss der beabsichtigte Empfänger sein (ermitteln aus dem letzten Initiated-Event)
        //       Dies erfordert einen Lookup im EventService oder ein Feld in MedicationUnit, das den intendedRecipient speichert.
        //       Für dieses Beispiel: Wir gehen davon aus, dass wir den intendedRecipient aus dem letzten TransferInitiatedEvent abrufen können.
        //       (In einer realen Implementierung würde man den intendedRecipient direkt in die MedicationUnit schreiben, wenn der Transfer initiiert wird.)
        String intendedRecipientActorId = getLatestIntendedRecipientFromLog(unit.getTransferLog());
        if (intendedRecipientActorId == null || !intendedRecipientActorId.equals(actorService.getCallerActorId(ctx))) {
            throw new ChaincodeException(String.format("Zugriff verweigert. Akteur '%s' ist nicht der beabsichtigte Empfänger für Einheit '%s'.", actorService.getCallerActorId(ctx), unitKey), "NOT_INTENDED_RECIPIENT");
        }

        //    c) Einheit darf nicht regulatorisch gesperrt sein (erneute Prüfung zur Sicherheit)
        Batch batch = assetService.getAssetByKey(ctx, unit.getBatchKey(), Batch.class);
        authService.requireAssetNotBlockedByRegulator(ctx, batch.getKey());


        // 3. Besitzer und Status aktualisieren (Immutable-Pattern)
        String oldOwnerActorId = unit.getCurrentOwnerActorId();
        List<TransferRecord> updatedTransferLog = new ArrayList<>(unit.getTransferLog());

        // Neues TransferRecord erstellen und zum Log hinzufügen
        TransferRecord completionRecord = new TransferRecord(
                UUID.randomUUID().toString(), // Record ID
                unitKey,
                oldOwnerActorId,
                intendedRecipientActorId,
                Instant.now().toString(),
                ctx.getStub().getTxId(),
                "COMPLETED" // Phase abgeschlossen
        );
        updatedTransferLog.add(completionRecord);

        MedicationUnit updatedUnit = new MedicationUnit(
                unit.getUnitId(),
                unit.getMedicationKey(),
                unit.getBatchKey(),
                intendedRecipientActorId, // NEU: Besitzer ist jetzt der Empfänger
                RoleConstants.FREIGEGEBEN, // Status zurück auf FREIGEGEBEN (oder IN_TRANSIT zu DELIVERED)
                updatedTransferLog // Aktualisiertes Transfer-Log
        );

        assetService.putAsset(ctx, updatedUnit); // Einheit im Ledger aktualisieren

        // 4. Event loggen
        eventService.logNewEvent(ctx, new TransferCompletedEvent(
                UUID.randomUUID().toString(), // Event ID (separat vom TransferRecord ID)
                unitKey,
                oldOwnerActorId, // Von (vorheriger Besitzer)
                intendedRecipientActorId, // Zu (neuer Besitzer)
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
    @Transaction(intent = Transaction.TYPE.EVALUATE) //
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
    @Transaction(intent = Transaction.TYPE.EVALUATE) //
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
        // Finde den letzten INITIATED Eintrag. TransferRecord hat keinen intendedRecipient direkt.
        // Dies ist ein Design-Problem, das man lösen muss: Entweder TransferRecord erweitern,
        // oder TransferInitiatedEvent/TransferCompletedEvent sind die Dinger im Log.
        // Für dieses Beispiel gehen wir davon aus, dass der *letzte* Eintrag des Logs, der "INITIATED" ist,
        // den toOwnerActorId als intendedRecipient enthält, wenn TransferRecord das Feld hätte.
        // Da TransferRecord nur from/to hat: Dies ist ein TO-DO, um das Konzept zu überarbeiten.
        // Annahme für jetzt: das toOwnerActorId des letzten PENDING_TRANSFER Record ist der intendedRecipient.
        if (!transferLog.isEmpty()) {
            TransferRecord lastRecord = transferLog.get(transferLog.size() - 1);
            if ("INITIATED".equals(lastRecord.getTransferPhase())) {
                return lastRecord.getToOwnerActorId(); // Wenn toOwnerActorId im Initiated-Event der intendedRecipient ist
            }
        }
        return null;
    }

}

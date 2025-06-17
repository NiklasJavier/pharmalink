// src/main/java/de/jklein/fabric/samples/assettransfer/contract/BatchContract.java
package de.jklein.fabric.samples.assettransfer.contract;

import de.jklein.fabric.samples.assettransfer.BaseContract;
import de.jklein.fabric.samples.assettransfer.model.Batch; //
import de.jklein.fabric.samples.assettransfer.model.Medication;
import de.jklein.fabric.samples.assettransfer.model.TransferCompletedEvent;
import de.jklein.fabric.samples.assettransfer.model.TransferInitiatedEvent;
import de.jklein.fabric.samples.assettransfer.permission.RoleConstants; //
import org.hyperledger.fabric.contract.Context; //
import org.hyperledger.fabric.contract.annotation.Contract; //
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction; //
import de.jklein.fabric.samples.assettransfer.permission.ContractPermission;
import org.hyperledger.fabric.shim.ChaincodeException; //

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

/**
 * Smart Contract zur Verwaltung von Batch-Assets (Chargen) in der Lieferkette.
 */
@Contract(
    name = "BatchContract",
    info = @Info(
        title = "Batch Management Contract",
        description = "Verwaltet Chargen-Assets in der Pharma-Lieferkette",
        version = "1.0.0"
    )
) //
public final class BatchContract extends BaseContract {

    public BatchContract() {
        super(); // Initialisiert die Services aus BaseContract
    }

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
}

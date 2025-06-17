// src/main/java/de/jklein/fabric/samples/assettransfer/contract/RegulatoryContract.java
package de.jklein.fabric.samples.assettransfer.contract;

import de.jklein.fabric.samples.assettransfer.BaseContract;
import de.jklein.fabric.samples.assettransfer.model.Batch;
import de.jklein.fabric.samples.assettransfer.model.Medication;
import de.jklein.fabric.samples.assettransfer.model.MedicationUnit;
import de.jklein.fabric.samples.assettransfer.model.RegulatoryAction; //
import de.jklein.fabric.samples.assettransfer.model.Tag;
import de.jklein.fabric.samples.assettransfer.permission.RoleConstants; //
import org.hyperledger.fabric.contract.Context; //
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException; //

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Smart Contract zur Verwaltung regulatorischer Aktionen und des regulatorischen Status von Assets.
 */
@Contract(name = "RegulatoryContract")
public final class RegulatoryContract extends BaseContract {

    public RegulatoryContract() {
        super(); // Initialisiert die Services aus BaseContract
    }

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
    @Transaction(intent = Transaction.TYPE.SUBMIT) //
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
}

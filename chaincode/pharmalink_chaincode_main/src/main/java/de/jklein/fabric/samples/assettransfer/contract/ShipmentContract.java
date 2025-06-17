// src/main/java/de/jklein/fabric/samples/assettransfer/contract/ShipmentContract.java
package de.jklein.fabric.samples.assettransfer.contract;

import de.jklein.fabric.samples.assettransfer.BaseContract;
import de.jklein.fabric.samples.assettransfer.model.MedicationUnit;
import de.jklein.fabric.samples.assettransfer.model.Shipment; //
import de.jklein.fabric.samples.assettransfer.model.TransferInitiatedEvent;
import de.jklein.fabric.samples.assettransfer.permission.RoleConstants;
import org.hyperledger.fabric.contract.Context; //
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException; //

import java.time.Instant;
import java.util.List; //
import java.util.UUID;

/**
 * Smart Contract zur Verwaltung von Sendungen in der Lieferkette.
 */
@Contract(
    name = "ShipmentContract",
    info = @Info(
        title = "Shipment Management Contract",
        description = "Verwaltet Lieferungen und Sendungen in der Pharma-Lieferkette",
        version = "1.0.0"
    )
)
public final class ShipmentContract extends BaseContract {

    public ShipmentContract() {
        super(); // Initialisiert die Services aus BaseContract
    }

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
    @Transaction(intent = Transaction.TYPE.SUBMIT) //
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
    @Transaction(intent = Transaction.TYPE.EVALUATE) //
    public Shipment getShipment(final Context ctx, final String shipmentId) {
        // Evaluate-Transaktionen brauchen keine requireActorApproved()
        String shipmentKey = "SHIP_" + shipmentId; //
        return assetService.getAssetByKey(ctx, shipmentKey, Shipment.class); //
    }
}

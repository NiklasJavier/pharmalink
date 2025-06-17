// src/main/java/de/jklein/fabric/samples/assettransfer/model/TransferRecord.java
package de.jklein.fabric.samples.assettransfer.model;

import com.owlike.genson.annotation.JsonProperty;
import java.util.Objects; //

/**
 * Repräsentiert einen einzelnen Eintrag im TransferLog einer MedicationUnit.
 * Dokumentiert einen Besitzwechsel.
 */
public final class TransferRecord implements IEvent {

    private final String transferRecordId; // Eindeutige ID für diesen Log-Eintrag
    private final String unitKey; // Schlüssel der betroffenen MedicationUnit
    private final String fromOwnerActorId; // Akteur-ID des vorherigen Besitzers
    private final String toOwnerActorId; // Akteur-ID des neuen Besitzers (nach Bestätigung)
    private final String transferTimestamp; // Zeitstempel des Transfers
    private final String transactionId; // ID der Fabric Transaktion, die diesen Transfer ausgelöst hat
    private final String transferPhase; // Phase des Transfers (z.B. "INITIATED", "COMPLETED")

    public TransferRecord(@JsonProperty("transferRecordId") final String transferRecordId,
                          @JsonProperty("unitKey") final String unitKey,
                          @JsonProperty("fromOwnerActorId") final String fromOwnerActorId,
                          @JsonProperty("toOwnerActorId") final String toOwnerActorId,
                          @JsonProperty("transferTimestamp") final String transferTimestamp,
                          @JsonProperty("transactionId") final String transactionId,
                          @JsonProperty("transferPhase") final String transferPhase) {
        this.transferRecordId = transferRecordId;
        this.unitKey = unitKey;
        this.fromOwnerActorId = fromOwnerActorId;
        this.toOwnerActorId = toOwnerActorId;
        this.transferTimestamp = transferTimestamp;
        this.transactionId = transactionId;
        this.transferPhase = transferPhase;
    }

    @Override
    public String getEventId() {
        return transferRecordId;
    }

    @Override
    public String getTimestamp() {
        return transferTimestamp;
    }

    @Override
    public String getRelatedAssetKey() {
        return unitKey;
    }

    @Override
    public String getTriggeringActorId() {
        // Je nach Phase ist der auslösende Akteur unterschiedlich.
        // Für INITIATED: fromOwnerActorId. Für COMPLETED: toOwnerActorId.
        // Hier müsste man eine Logik einbauen, um den korrekten zu ermitteln.
        // Für den Zweck des Interfaces geben wir den fromOwner zurück, um eine Referenz zu haben.
        return fromOwnerActorId;
    }

    @Override
    public String getEventType() {
        return "TransferRecord_" + transferPhase;
    }

    public String getTransferRecordId() {
        return transferRecordId;
    }

    public String getUnitKey() {
        return unitKey;
    }

    public String getFromOwnerActorId() {
        return fromOwnerActorId;
    }

    public String getToOwnerActorId() {
        return toOwnerActorId;
    }

    public String getTransferTimestamp() {
        return transferTimestamp;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getTransferPhase() {
        return transferPhase;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        TransferRecord other = (TransferRecord) obj;
        return Objects.deepEquals(
                new String[]{getTransferRecordId(), getUnitKey(), getFromOwnerActorId(), getToOwnerActorId(),
                        getTransferTimestamp(), getTransactionId(), getTransferPhase()},
                new String[]{other.getTransferRecordId(), other.getUnitKey(), other.getFromOwnerActorId(), other.getToOwnerActorId(),
                        other.getTransferTimestamp(), other.getTransactionId(), other.getTransferPhase()}
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTransferRecordId(), getUnitKey(), getFromOwnerActorId(), getToOwnerActorId(),
                getTransferTimestamp(), getTransactionId(), getTransferPhase());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [transferRecordId=" + transferRecordId
                + ", unitKey=" + unitKey + ", fromOwnerActorId=" + fromOwnerActorId + ", toOwnerActorId=" + toOwnerActorId
                + ", transferTimestamp=" + transferTimestamp + ", transactionId=" + transactionId + ", transferPhase=" + transferPhase + "]";
    }
}

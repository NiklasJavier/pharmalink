// src/main/java/de/jklein/fabric/samples/assettransfer/model/TransferInitiatedEvent.java
package de.jklein.fabric.samples.assettransfer.model;

import com.owlike.genson.annotation.JsonProperty;
import java.util.Objects; //

/**
 * Repräsentiert ein Event, das geloggt wird, wenn ein Transfer einer Einheit initiiert wird.
 */
public final class TransferInitiatedEvent implements IEvent {

    private final String eventId; // Eindeutige ID für dieses Event
    private final String unitKey; // Schlüssel der betroffenen MedicationUnit
    private final String fromOwnerActorId; // Akteur-ID des Initiators (aktueller Besitzer)
    private final String intendedRecipientActorId; // Akteur-ID des beabsichtigten Empfängers
    private final String eventTimestamp; // Zeitstempel des Events
    private final String transactionId; // ID der Fabric Transaktion, die das Event ausgelöst hat

    public TransferInitiatedEvent(
            @JsonProperty("eventId") final String eventId,
            @JsonProperty("unitKey") final String unitKey,
            @JsonProperty("fromOwnerActorId") final String fromOwnerActorId,
            @JsonProperty("intendedRecipientActorId") final String intendedRecipientActorId,
            @JsonProperty("eventTimestamp") final String eventTimestamp,
            @JsonProperty("transactionId") final String transactionId) {
        this.eventId = eventId;
        this.unitKey = unitKey;
        this.fromOwnerActorId = fromOwnerActorId;
        this.intendedRecipientActorId = intendedRecipientActorId;
        this.eventTimestamp = eventTimestamp;
        this.transactionId = transactionId;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getTimestamp() {
        return eventTimestamp;
    }

    @Override
    public String getRelatedAssetKey() {
        return unitKey;
    }

    @Override
    public String getTriggeringActorId() {
        return fromOwnerActorId; // Der Initiator ist der auslösende Akteur
    }

    @Override
    public String getEventType() {
        return "TransferInitiated";
    }

    public String getUnitKey() {
        return unitKey;
    }

    public String getFromOwnerActorId() {
        return fromOwnerActorId;
    }

    public String getIntendedRecipientActorId() {
        return intendedRecipientActorId;
    }

    public String getEventTimestamp() {
        return eventTimestamp;
    }

    public String getTransactionId() {
        return transactionId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        TransferInitiatedEvent other = (TransferInitiatedEvent) obj;
        return Objects.deepEquals(
                new String[]{getEventId(), getUnitKey(), getFromOwnerActorId(), getIntendedRecipientActorId(), getEventTimestamp(), getTransactionId()},
                new String[]{other.getEventId(), other.getUnitKey(), other.getFromOwnerActorId(), other.getIntendedRecipientActorId(), other.getEventTimestamp(), other.getTransactionId()}
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEventId(), getUnitKey(), getFromOwnerActorId(), getIntendedRecipientActorId(), getEventTimestamp(), getTransactionId());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [eventId=" + eventId
                + ", unitKey=" + unitKey + ", fromOwnerActorId=" + fromOwnerActorId + ", intendedRecipientActorId=" + intendedRecipientActorId
                + ", eventTimestamp=" + eventTimestamp + ", transactionId=" + transactionId + "]";
    }
}


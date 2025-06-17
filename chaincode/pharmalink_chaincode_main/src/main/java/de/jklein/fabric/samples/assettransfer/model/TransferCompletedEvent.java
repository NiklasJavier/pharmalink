// src/main/java/de/jklein/fabric/samples/assettransfer/model/TransferCompletedEvent.java
package de.jklein.fabric.samples.assettransfer.model;

import com.owlike.genson.annotation.JsonProperty;
import java.util.Objects; //
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

/**
 * Repräsentiert ein Event, das geloggt wird, wenn ein Transfer einer Einheit erfolgreich abgeschlossen wurde.
 */
@DataType()
public final class TransferCompletedEvent implements IEvent {

    @Property()
    private final String eventId; // Eindeutige ID für dieses Event
    @Property()
    private final String unitKey; // Schlüssel der betroffenen MedicationUnit
    @Property()
    private final String fromOwnerActorId; // Akteur-ID des vorherigen Besitzers (der den Transfer initiiert hat)
    @Property()
    private final String toOwnerActorId; // Akteur-ID des neuen Besitzers (der den Transfer bestätigt hat)
    @Property()
    private final String eventTimestamp; // Zeitstempel des Events
    @Property()
    private final String transactionId; // ID der Fabric Transaktion, die das Event ausgelöst hat (Bestätigungstransaktion)

    public TransferCompletedEvent(
            @JsonProperty("eventId") final String eventId,
            @JsonProperty("unitKey") final String unitKey,
            @JsonProperty("fromOwnerActorId") final String fromOwnerActorId,
            @JsonProperty("toOwnerActorId") final String toOwnerActorId,
            @JsonProperty("eventTimestamp") final String eventTimestamp,
            @JsonProperty("transactionId") final String transactionId) {
        this.eventId = eventId;
        this.unitKey = unitKey;
        this.fromOwnerActorId = fromOwnerActorId;
        this.toOwnerActorId = toOwnerActorId;
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
        return toOwnerActorId; // Der Empfänger ist der auslösende Akteur für den Abschluss
    }

    @Override
    public String getEventType() {
        return "TransferCompleted";
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
        TransferCompletedEvent other = (TransferCompletedEvent) obj;
        return Objects.deepEquals(
                new String[]{getEventId(), getUnitKey(), getFromOwnerActorId(), getToOwnerActorId(), getEventTimestamp(), getTransactionId()},
                new String[]{other.getEventId(), other.getUnitKey(), other.getFromOwnerActorId(), other.getToOwnerActorId(), other.getEventTimestamp(), other.getTransactionId()}
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEventId(), getUnitKey(), getFromOwnerActorId(), getToOwnerActorId(), getEventTimestamp(), getTransactionId());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [eventId=" + eventId
                + ", unitKey=" + unitKey + ", fromOwnerActorId=" + fromOwnerActorId + ", toOwnerActorId=" + toOwnerActorId
                + ", eventTimestamp=" + eventTimestamp + ", transactionId=" + transactionId + "]";
    }
}

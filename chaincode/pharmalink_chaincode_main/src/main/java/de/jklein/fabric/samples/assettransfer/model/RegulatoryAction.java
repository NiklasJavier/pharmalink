// src/main/java/de/jklein/fabric/samples/assettransfer/model/RegulatoryAction.java
package de.jklein.fabric.samples.assettransfer.model;

import com.owlike.genson.annotation.JsonProperty;
import java.util.Objects;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

/**
 * Repräsentiert eine regulatorische Maßnahme, die von einer Behörde durchgeführt wird.
 * Dient auch als Event-Objekt, das im Ledger geloggt wird.
 */
@DataType()
public final class RegulatoryAction implements IAsset, IEvent { // Implementiert jetzt IEvent

    @Property()
    private final String actionId; // Eindeutige ID der Aktion
    @Property()
    private final String targetAssetKey; // Schlüssel des betroffenen Assets (Medication, Batch, Unit)
    @Property()
    private final String actionType; // Art der Aktion (z.B. "PRODUKT_FREIGABE", "PRODUKT_SPERRUNG")
    @Property()
    private final String actionDescription; // Beschreibung des Grundes/der Details der Aktion
    @Property()
    private final String issuingRegulatorActorId; // Akteur-ID der ausstellenden Behörde
    @Property()
    private final String actionTimestamp; // Zeitstempel der Aktion

    public RegulatoryAction(
            @JsonProperty("actionId") final String actionId,
            @JsonProperty("targetAssetKey") final String targetAssetKey,
            @JsonProperty("actionType") final String actionType,
            @JsonProperty("actionDescription") final String actionDescription,
            @JsonProperty("issuingRegulatorActorId") final String issuingRegulatorActorId,
            @JsonProperty("actionTimestamp") final String actionTimestamp) {
        this.actionId = actionId;
        this.targetAssetKey = targetAssetKey;
        this.actionType = actionType;
        this.actionDescription = actionDescription;
        this.issuingRegulatorActorId = issuingRegulatorActorId;
        this.actionTimestamp = actionTimestamp;
    }

    // --- IAsset Implementierung ---
    @Override
    public String getKey() {
        return "REG_ACTION_" + actionId; // Präfix für regulatorische Aktionen im Ledger
    }

    @Override
    public String getStatus() {
        // Regulatorische Aktionen haben selbst keinen "Status" im Lebenszyklus-Sinn;
        // sie sind Befehle. Der Typ der Aktion kann als "Status" interpretiert werden.
        return actionType;
    }

    @Override
    public String getRegulatoryStatus() {
        return actionType; // Die Aktion selbst ist der regulatorische Status
    }

    @Override
    public boolean isLockedByRegulator() {
        // Eine RegulatoryAction selbst ist kein Asset, das gesperrt wird,
        // sondern eine Aktion, die andere Assets sperren kann.
        return false;
    }

    @Override
    public String getAssociatedActorId() {
        return issuingRegulatorActorId;
    }

    // --- IEvent Implementierung ---
    @Override
    public String getEventId() {
        return getKey(); // Der Schlüssel im Ledger dient auch als Event-ID
    }

    @Override
    public String getTimestamp() {
        return actionTimestamp;
    }

    @Override
    public String getRelatedAssetKey() {
        return targetAssetKey;
    }

    @Override
    public String getTriggeringActorId() {
        return issuingRegulatorActorId; // Die ausstellende Behörde ist der auslösende Akteur
    }

    @Override
    public String getEventType() {
        return "RegulatoryAction_" + actionType;
    }

    // --- Getter (bleiben wie zuvor) ---
    public String getActionId() {
        return actionId;
    }

    public String getTargetAssetKey() {
        return targetAssetKey;
    }

    public String getActionType() {
        return actionType;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public String getIssuingRegulatorActorId() {
        return issuingRegulatorActorId;
    }

    public String getActionTimestamp() {
        return actionTimestamp;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        RegulatoryAction other = (RegulatoryAction) obj;
        return Objects.deepEquals(
                new String[]{getActionId(), getTargetAssetKey(), getActionType(), getActionDescription(),
                        getIssuingRegulatorActorId(), getActionTimestamp()},
                new String[]{other.getActionId(), other.getTargetAssetKey(), other.getActionType(), other.getActionDescription(),
                        other.getIssuingRegulatorActorId(), other.getActionTimestamp()});
    }

    @Override
    public int hashCode() {
        return Objects.hash(getActionId(), getTargetAssetKey(), getActionType(), getActionDescription(),
                getIssuingRegulatorActorId(), getActionTimestamp());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [actionId=" + actionId
                + ", targetAssetKey=" + targetAssetKey + ", actionType=" + actionType + ", actionDescription=" + actionDescription
                + ", issuingRegulatorActorId=" + issuingRegulatorActorId + ", actionTimestamp=" + actionTimestamp + "]";
    }
}

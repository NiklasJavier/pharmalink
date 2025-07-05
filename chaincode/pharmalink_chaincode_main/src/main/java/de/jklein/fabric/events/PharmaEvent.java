package de.jklein.fabric.events;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.Objects;

@DataType()
public final class PharmaEvent {

    @Property()
    private String entityType;

    @Property()
    private String entityId;

    @Property()
    private String operation;

    public PharmaEvent() {
    }

    // Parameter im Konstruktor umbenannt, um das Verbergen von Feldern zu vermeiden
    public PharmaEvent(@JsonProperty("entityType") final String newEntityType,
                       @JsonProperty("entityId") final String newEntityId,
                       @JsonProperty("operation") final String newOperation) {
        this.entityType = newEntityType;
        this.entityId = newEntityId;
        this.operation = newOperation;
    }

    // Getter und Setter
    public String getEntityType() {
        return entityType;
    }

    // Parameter im Setter umbenannt, um das Verbergen von Feldern zu vermeiden
    public void setEntityType(final String newEntityType) {
        this.entityType = newEntityType;
    }

    public String getEntityId() {
        return entityId;
    }

    // Parameter im Setter umbenannt, um das Verbergen von Feldern zu vermeiden
    public void setEntityId(final String newEntityId) {
        this.entityId = newEntityId;
    }

    public String getOperation() {
        return operation;
    }

    // Parameter im Setter umbenannt, um das Verbergen von Feldern zu vermeiden
    public void setOperation(final String newOperation) {
        this.operation = newOperation;
    }

    @Override
    public boolean equals(final Object o) {
        // Geschweifte Klammern bei if-Anweisungen hinzugefügt
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PharmaEvent that = (PharmaEvent) o;
        return Objects.equals(getEntityType(), that.getEntityType())
                && Objects.equals(getEntityId(), that.getEntityId())
                && Objects.equals(getOperation(), that.getOperation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEntityType(), getEntityId(), getOperation());
    }

    @Override
    public String toString() {
        return "PharmaEvent{"
                + "entityType='" + entityType + '\''
                + ", entityId='" + entityId + '\''
                + ", operation='" + operation + '\''
                + '}';
    }
}

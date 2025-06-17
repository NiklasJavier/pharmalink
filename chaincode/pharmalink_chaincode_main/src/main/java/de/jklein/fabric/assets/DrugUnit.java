package de.jklein.fabric.assets;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import java.util.List;
import java.util.Objects;

@DataType()
public final class DrugUnit {
    private static final Genson GENSON = new Genson();

    @Property() private final String id;
    @Property() private final String batchId;
    @Property() private final String drugId;
    @Property() private final String owner;
    @Property() private final String manufacturerId;
    @Property() private final String description;
    @Property() private final List<String> tags;
    @Property() private final String currentState;
    @Property() private final String dispensedBy;
    @Property() private final String dispensedTo;
    @Property() private final String dispensingTimestamp;

    public DrugUnit(final String id, final String batchId, final String drugId, final String owner, final String manufacturerId, final String description, final List<String> tags, final String currentState, final String dispensedBy, final String dispensedTo, final String dispensingTimestamp) {
        this.id = id;
        this.batchId = batchId;
        this.drugId = drugId;
        this.owner = owner;
        this.manufacturerId = manufacturerId;
        this.description = description;
        this.tags = tags;
        this.currentState = currentState;
        this.dispensedBy = dispensedBy;
        this.dispensedTo = dispensedTo;
        this.dispensingTimestamp = dispensingTimestamp;
    }

    public String getId() { return id; }
    public String getBatchId() { return batchId; }
    public String getDrugId() { return drugId; }
    public String getOwner() { return owner; }
    public String getManufacturerId() { return manufacturerId; }
    public String getDescription() { return description; }
    public List<String> getTags() { return tags; }
    public String getCurrentState() { return currentState; }
    public String getDispensedBy() { return dispensedBy; }
    public String getDispensedTo() { return dispensedTo; }
    public String getDispensingTimestamp() { return dispensingTimestamp; }

    public String toJSONString() { return GENSON.serialize(this); }
    public static DrugUnit fromJSONString(final String json) { return GENSON.deserialize(json, DrugUnit.class); }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        DrugUnit drugUnit = (DrugUnit) o;
        return Objects.equals(getId(), drugUnit.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getBatchId(), getDrugId(), getOwner(), getManufacturerId(), getDescription(), getTags(), getCurrentState(), getDispensedBy(), getDispensedTo(), getDispensingTimestamp());
    }
}

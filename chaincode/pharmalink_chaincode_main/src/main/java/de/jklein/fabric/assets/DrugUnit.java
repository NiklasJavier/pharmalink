package de.jklein.fabric.assets;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import java.util.ArrayList;
import java.util.List;

@DataType()
public final class DrugUnit {
    private static final Genson GENSON = new Genson();

    @Property() private final String id;
    @Property() private final String batchId;
    @Property() private final String drugId;
    @Property() private String owner;
    @Property() private final String manufacturerId;
    @Property() private final String description;
    @Property() private List<String> tags;

    @Property() private String currentState;
    @Property() private String dispensedBy;
    @Property() private String dispensedTo;
    @Property() private String dispensingTimestamp;

    public DrugUnit(final String id, final String batchId, final String drugId, final String owner, final String manufacturerId, final String description) {
        this.id = id;
        this.batchId = batchId;
        this.drugId = drugId;
        this.owner = owner;
        this.manufacturerId = manufacturerId;
        this.description = description;
        this.tags = new ArrayList<>();
        this.currentState = "Created";
        this.dispensedBy = null;
        this.dispensedTo = null;
        this.dispensingTimestamp = null;
    }

    public String getId() {
        return id;
    }
    public String getBatchId() {
        return batchId;
    }
    public String getDrugId() {
        return drugId;
    }
    public String getOwner() {
        return owner;
    }
    public String getManufacturerId() {
        return manufacturerId;
    }
    public String getDescription() {
        return description;
    }
    public List<String> getTags() {
        return tags;
    }
    public String getCurrentState() {
        return currentState;
    }
    public String getDispensedBy() {
        return dispensedBy;
    }
    public String getDispensedTo() {
        return dispensedTo;
    }
    public String getDispensingTimestamp() {
        return dispensingTimestamp;
    }

    public void setOwner(final String newOwner) {
        this.owner = newOwner;
    }
    public void addTag(final String tag) {
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }
    public void setCurrentState(final String state) {
        this.currentState = state;
    }

    public void dispense(final String newDispensedBy, final String newDispensedTo, final String timestamp) {
        this.owner = "Dispensed";
        this.currentState = "Dispensed";
        this.dispensedBy = newDispensedBy;
        this.dispensedTo = newDispensedTo;
        this.dispensingTimestamp = timestamp;
    }

    public String toJSONString() {
        return GENSON.serialize(this);
    }
    public static DrugUnit fromJSONString(final String json) {
        return GENSON.deserialize(json, DrugUnit.class);
    }
}

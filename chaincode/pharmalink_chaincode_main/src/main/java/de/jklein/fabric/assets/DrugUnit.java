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

    private DrugUnit(final Builder builder) {
        this.id = builder.bId;
        this.batchId = builder.bBatchId;
        this.drugId = builder.bDrugId;
        this.owner = builder.bOwner;
        this.manufacturerId = builder.bManufacturerId;
        this.description = builder.bDescription;
        this.tags = builder.bTags;
        this.currentState = builder.bCurrentState;
        this.dispensedBy = builder.bDispensedBy;
        this.dispensedTo = builder.bDispensedTo;
        this.dispensingTimestamp = builder.bDispensingTimestamp;
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

    public String toJSONString() {
        return GENSON.serialize(this);
    }
    public static DrugUnit fromJSONString(final String json) {
        return GENSON.deserialize(json, DrugUnit.class);
    }

    public static final class Builder {
        private String bId;
        private String bBatchId;
        private String bDrugId;
        private String bOwner;
        private String bManufacturerId;
        private String bDescription;
        private List<String> bTags;
        private String bCurrentState;
        private String bDispensedBy;
        private String bDispensedTo;
        private String bDispensingTimestamp;

        public Builder id(final String id) {
            this.bId = id;
            return this;
        }
        public Builder batchId(final String batchId) {
            this.bBatchId = batchId;
            return this;
        }
        public Builder drugId(final String drugId) {
            this.bDrugId = drugId;
            return this;
        }
        public Builder owner(final String owner) {
            this.bOwner = owner;
            return this;
        }
        public Builder manufacturerId(final String manufacturerId) {
            this.bManufacturerId = manufacturerId;
            return this;
        }
        public Builder description(final String description) {
            this.bDescription = description;
            return this;
        }
        public Builder tags(final List<String> tags) {
            this.bTags = tags;
            return this;
        }
        public Builder currentState(final String currentState) {
            this.bCurrentState = currentState;
            return this;
        }
        public Builder dispensedBy(final String dispensedBy) {
            this.bDispensedBy = dispensedBy;
            return this;
        }
        public Builder dispensedTo(final String dispensedTo) {
            this.bDispensedTo = dispensedTo;
            return this;
        }
        public Builder dispensingTimestamp(final String dispensingTimestamp) {
            this.bDispensingTimestamp = dispensingTimestamp;
            return this;
        }

        public DrugUnit build() {
            return new DrugUnit(this);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DrugUnit drugUnit = (DrugUnit) o;
        return Objects.equals(getId(), drugUnit.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}

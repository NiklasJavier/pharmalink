package de.jklein.fabric.assets;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

@DataType()
public final class DrugInfo {
    private static final Genson GENSON = new Genson();

    @Property()
    private final String id;

    @Property()
    private final String gtin;

    @Property()
    private final String name;

    @Property()
    private final String manufacturerId;

    @Property()
    private final String description;

    @Property()
    private String status;

    public DrugInfo(final String id, final String gtin, final String name, final String manufacturerId, final String description) {
        this.id = id;
        this.gtin = gtin;
        this.name = name;
        this.manufacturerId = manufacturerId;
        this.description = description;
        this.status = "NotApproved";
    }

    public String getId() {
        return id;
    }

    public String getGtin() {
        return gtin;
    }

    public String getName() {
        return name;
    }

    public String getManufacturerId() {
        return manufacturerId;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String newStatus) {
        this.status = newStatus;
    }

    public String toJSONString() {
        return GENSON.serialize(this);
    }

    public static DrugInfo fromJSONString(final String json) {
        return GENSON.deserialize(json, DrugInfo.class);
    }
}

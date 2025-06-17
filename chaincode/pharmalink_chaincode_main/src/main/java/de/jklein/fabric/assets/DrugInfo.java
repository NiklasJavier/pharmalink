package de.jklein.fabric.assets;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import java.util.Objects;

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
    private final String status;

    public DrugInfo(final String id, final String gtin, final String name, final String manufacturerId, final String description, final String status) {
        this.id = id;
        this.gtin = gtin;
        this.name = name;
        this.manufacturerId = manufacturerId;
        this.description = description;
        this.status = status;
    }

    public String getId() { return id; }
    public String getGtin() { return gtin; }
    public String getName() { return name; }
    public String getManufacturerId() { return manufacturerId; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }

    public String toJSONString() { return GENSON.serialize(this); }
    public static DrugInfo fromJSONString(final String json) { return GENSON.deserialize(json, DrugInfo.class); }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        DrugInfo drugInfo = (DrugInfo) o;
        return Objects.equals(getId(), drugInfo.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getGtin(), getName(), getManufacturerId(), getDescription(), getStatus());
    }
}

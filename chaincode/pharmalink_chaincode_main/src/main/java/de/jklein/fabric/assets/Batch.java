package de.jklein.fabric.assets;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@DataType()
public final class Batch {
    private static final Genson GENSON = new Genson();

    @Property()
    private final String id; // UUID

    @Property()
    private final String drugId;

    @Property()
    private final long quantity;

    @Property()
    private final String manufacturerId;

    @Property()
    private final String description; // Beschreibung / Alias der Charge

    @Property()
    private final List<String> tags;

    public Batch() {
        this.id = "";
        this.drugId = "";
        this.quantity = 0;
        this.manufacturerId = "";
        this.description = "";
        this.tags = new ArrayList<>();
    }

    // Konstruktor mit 6 Parametern (konform)
    public Batch(final String id, final String drugId, final long quantity, final String manufacturerId, final String description, final List<String> tags) {
        this.id = id;
        this.drugId = drugId;
        this.quantity = quantity;
        this.manufacturerId = manufacturerId;
        this.description = description;
        this.tags = tags;
    }

    public String getId() {
        return id;
    }

    public String getDrugId() {
        return drugId;
    }

    public long getQuantity() {
        return quantity;
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

    public String toJSONString() {
        return GENSON.serialize(this);
    }

    public static Batch fromJSONString(final String json) {
        return GENSON.deserialize(json, Batch.class);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Batch batch = (Batch) o;
        return Objects.equals(getId(), batch.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getDrugId(), getQuantity(), getManufacturerId(), getDescription(), getTags());
    }

    @Override
    public String toString() {
        return "Batch{"
                + "id='" + id + '\''
                + ", drugId='" + drugId + '\''
                + ", quantity=" + quantity
                + ", manufacturerId='" + manufacturerId + '\''
                + ", description='" + description + '\''
                + ", tags=" + tags
                + '}';
    }
}

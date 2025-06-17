package de.jklein.fabric.assets;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import java.util.ArrayList;
import java.util.List;

@DataType()
public final class Batch {
    private static final Genson GENSON = new Genson();

    @Property() private final String id;
    @Property() private final String drugId;
    @Property() private final long quantity;
    @Property() private final String manufacturerId;
    @Property() private final String description;
    @Property() private List<String> tags;

    public Batch(final String id, final String drugId, final long quantity, final String manufacturerId, final String description) {
        this.id = id;
        this.drugId = drugId;
        this.quantity = quantity;
        this.manufacturerId = manufacturerId;
        this.description = description;
        this.tags = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getDrugId() {
        return drugId;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void addTag(final String tag) {
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }

    public String toJSONString() {
        return GENSON.serialize(this);
    }

    public static Batch fromJSONString(final String json) {
        return GENSON.deserialize(json, Batch.class);
    }
}

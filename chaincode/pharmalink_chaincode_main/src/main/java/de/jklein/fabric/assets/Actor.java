package de.jklein.fabric.assets;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

@DataType()
public final class Actor {
    private static final Genson GENSON = new Genson();

    @Property()
    private final String mspId;

    @Property()
    private String actorId;

    @Property()
    private final String name;

    @Property()
    private final String role;

    @Property()
    private String status;

    public Actor(final String mspId, final String actorId, final String name, final String role, final String status) {
        this.mspId = mspId;
        this.actorId = actorId;
        this.name = name;
        this.role = role;
        this.status = status;
    }

    public String getMspId() {
        return mspId;
    }

    public String getActorId() {
        return actorId;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String newStatus) {
        this.status = newStatus;
    }

    public void setActorId(final String newActorId) {
        this.actorId = newActorId;
    }

    public String toJSONString() {
        return GENSON.serialize(this);
    }

    public static Actor fromJSONString(final String json) {
        return GENSON.deserialize(json, Actor.class);
    }
}

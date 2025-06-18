package de.jklein.fabric.assets;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import java.util.Objects;

@DataType()
public final class Actor {
    private static final Genson GENSON = new Genson();

    @Property()
    private final String certId;
    @Property()
    private final String mspId;
    @Property()
    private final String actorId;
    @Property()
    private final String name;
    @Property()
    private final String role;
    @Property()
    private final String status;

    public Actor() {
        this.certId = "";
        this.mspId = "";
        this.actorId = "";
        this.name = "";
        this.role = "";
        this.status = "";
    }

    public Actor(final String certId, final String mspId, final String actorId, final String name, final String role, final String status) {
        this.certId = certId;
        this.mspId = mspId;
        this.actorId = actorId;
        this.name = name;
        this.role = role;
        this.status = status;
    }

    public String getCertId() {
        return certId;
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

    public String toJSONString() {
        return GENSON.serialize(this);
    }

    public static Actor fromJSONString(final String json) {
        return GENSON.deserialize(json, Actor.class);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Actor actor = (Actor) o;
        return Objects.equals(getActorId(), actor.getActorId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getActorId());
    }
}

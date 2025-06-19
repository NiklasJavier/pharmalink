package de.jklein.fabric.assets;

import com.owlike.genson.Genson;
import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import java.util.Objects;

@DataType()
public final class Actor {
    private static final Genson GENSON = new Genson();

    @Property()
    private final String actorId;

    @Property()
    private final String enrollmentId;

    // 'description' wurde entfernt

    @Property()
    private final String mspId;


    @Property()
    private final String role;


    @Property()
    private final String status;


    @Property()
    private final String approvedBy;


    @Property()
    private final String certId;


    public String getActorId() {
        return actorId;
    }


    public String getEnrollmentId() {
        return enrollmentId;
    }


    public String getMspId() {
        return mspId;
    }


    public String getRole() {
        return role;
    }


    public String getStatus() {
        return status;
    }


    public String getApprovedBy() {
        return approvedBy;
    }


    public String getCertId() {
        return certId;
    }


    public Actor(
            @JsonProperty("actorId") final String actorId,
            @JsonProperty("enrollmentId") final String enrollmentId,
            @JsonProperty("mspId") final String mspId,
            @JsonProperty("role") final String role,
            @JsonProperty("status") final String status,
            @JsonProperty("approvedBy") final String approvedBy,
            @JsonProperty("certId") final String certId) {
        this.actorId = actorId;
        this.enrollmentId = enrollmentId;
        this.mspId = mspId;
        this.role = role;
        this.status = status;
        this.approvedBy = approvedBy;
        this.certId = certId;
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
        Actor actor = (Actor) o;
        return Objects.equals(getActorId(), actor.getActorId());
    }


    @Override
    public int hashCode() {
        return Objects.hash(actorId, enrollmentId, mspId, role, status, approvedBy, certId);
    }


    @Override
    public String toString() {
        // 'description' aus der Ausgabe entfernt
        return "Actor@" + Objects.hash(this) + " [actorId=" + actorId + ", role=" + role + ", status=" + status + "]";
    }
}

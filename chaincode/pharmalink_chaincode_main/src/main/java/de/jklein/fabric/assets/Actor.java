package de.jklein.fabric.assets;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import java.util.Objects;

@DataType()
public final class Actor {
    private static final Genson GENSON = new Genson();

    @Property()
    private final String actorId;

    @Property()
    private final String description; // Alias für den Akteur (ehemals 'name')

    @Property()
    private final String mspId;

    @Property()
    private final String role;

    @Property()
    private final String status;

    @Property()
    private final String approvedBy;

    @Property()
    private final String certId; // Beibehalten für Authentifizierungszwecke

    // Parameterloser Konstruktor für die Deserialisierung
    private Actor() {
        this.actorId = "";
        this.description = "";
        this.mspId = "";
        this.role = "";
        this.status = "";
        this.approvedBy = "";
        this.certId = "";
    }

    // Privater Konstruktor, der nur vom Builder aufgerufen wird (6 Parameter)
    private Actor(final Builder builder) {
        this.actorId = builder.bActorId;
        this.description = builder.bDescription;
        this.mspId = builder.bMspId;
        this.role = builder.bRole;
        this.status = builder.bStatus;
        this.approvedBy = builder.bApprovedBy;
        this.certId = builder.bCertId;
    }

    public String getActorId() {
        return actorId;
    }

    public String getDescription() {
        return description;
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
        return Objects.hash(getActorId(), description, mspId, role, status, approvedBy, certId);
    }

    @Override
    public String toString() {
        return "Actor{"
                + "actorId='" + actorId + '\''
                + ", description='" + description + '\''
                + ", mspId='" + mspId + '\''
                + ", role='" + role + '\''
                + ", status='" + status + '\''
                + ", approvedBy='" + approvedBy + '\''
                + ", certId='" + certId + '\''
                + '}';
    }

    public static final class Builder {
        private String bActorId;
        private String bDescription;
        private String bMspId;
        private String bRole;
        private String bStatus;
        private String bApprovedBy;
        private String bCertId;

        public Builder actorId(final String actorId) {
            this.bActorId = actorId;
            return this;
        }

        public Builder description(final String description) {
            this.bDescription = description;
            return this;
        }

        public Builder mspId(final String mspId) {
            this.bMspId = mspId;
            return this;
        }

        public Builder role(final String role) {
            this.bRole = role;
            return this;
        }

        public Builder status(final String status) {
            this.bStatus = status;
            return this;
        }

        public Builder approvedBy(final String approvedBy) {
            this.bApprovedBy = approvedBy;
            return this;
        }

        public Builder certId(final String certId) {
            this.bCertId = certId;
            return this;
        }

        public Actor build() {
            return new Actor(this);
        }
    }
}

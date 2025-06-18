package de.jklein.fabric.assets;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import java.util.Objects;

@DataType()
public final class Actor {
    private static final Genson GENSON = new Genson();

    @Property()
    private final String actorId; // Eindeutige ID im Ledger (UUID)

    @Property()
    private final String enrollmentId; // Die Enrollment ID aus dem Client-Zertifikat (z.B. user1, hersteller-user1)

    @Property()
    private final String name; // Der beschreibende Name / Alias des Akteurs

    @Property()
    private final String mspId;

    @Property()
    private final String role;

    @Property()
    private final String status; // z.B. "Pending", "Approved"

    @Property()
    private final String approvedBy; // actorId des Akteurs, der genehmigt hat

    @Property()
    private final String certId; // Die volle Zertifikat-ID (z.B. "x509::CN=...")

    // Parameterloser Konstruktor für die Deserialisierung
    private Actor() {
        this.actorId = "";
        this.enrollmentId = "";
        this.name = "";
        this.mspId = "";
        this.role = "";
        this.status = "";
        this.approvedBy = "";
        this.certId = "";
    }

    // Privater Konstruktor, der nur vom Builder aufgerufen wird (7 Parameter)
    private Actor(final Builder builder) {
        this.actorId = builder.bActorId;
        this.enrollmentId = builder.bEnrollmentId;
        this.name = builder.bName;
        this.mspId = builder.bMspId;
        this.role = builder.bRole;
        this.status = builder.bStatus;
        this.approvedBy = builder.bApprovedBy;
        this.certId = builder.bCertId;
    }

    public String getActorId() {
        return actorId;
    }

    public String getEnrollmentId() {
        return enrollmentId;
    }

    public String getName() {
        return name;
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
        return Objects.hash(getActorId(), getEnrollmentId(), getName(), getMspId(), getRole(), getStatus(), getApprovedBy(), getCertId());
    }

    @Override
    public String toString() {
        return "Actor{"
                + "actorId='" + actorId + '\''
                + ", enrollmentId='" + enrollmentId + '\''
                + ", name='" + name + '\''
                + ", mspId='" + mspId + '\''
                + ", role='" + role + '\''
                + ", status='" + status + '\''
                + ", approvedBy='" + approvedBy + '\''
                + ", certId='" + certId + '\''
                + '}';
    }

    public static final class Builder {
        private String bActorId;
        private String bEnrollmentId;
        private String bName;
        private String bMspId;
        private String bRole;
        private String bStatus;
        private String bApprovedBy;
        private String bCertId;

        // Optional: Ein Konstruktor für den Builder, um die primären Identifikatoren zu setzen
        public Builder actorId(final String actorId) {
            this.bActorId = actorId;
            return this;
        }

        public Builder enrollmentId(final String enrollmentId) {
            this.bEnrollmentId = enrollmentId;
            return this;
        }

        public Builder name(final String name) {
            this.bName = name;
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

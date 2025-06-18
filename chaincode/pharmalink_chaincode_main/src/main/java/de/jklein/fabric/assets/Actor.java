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
    private final String enrollmentId;

    @Property()
    private final String name;

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

    // Privater Konstruktor, der nur vom Builder aufgerufen wird
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

    // --- GETTERS ---
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

    // --- Hilfsmethoden ---
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

    // --- Builder-Klasse ---
    public static final class Builder {
        private final String bActorId;
        private final String bEnrollmentId;
        private String bName;
        private String bMspId;
        private String bRole;
        private String bStatus;
        private String bApprovedBy;
        private String bCertId;

        public Builder(final String actorId, final String enrollmentId) {
            this.bActorId = actorId;
            this.bEnrollmentId = enrollmentId;
        }

        /**
         * Setzt den Namen des Akteurs.
         *
         * @param name Der Name des Akteurs
         * @return Die Builder-Instanz für Method-Chaining
         */
        public Builder name(final String name) {
            this.bName = name;
            return this;
        }
        /**
         * Setzt die MSP-ID des Akteurs.
         *
         * @param mspId Die MSP-ID des Akteurs
         * @return Die Builder-Instanz für Method-Chaining
         */
        public Builder mspId(final String mspId) {
            this.bMspId = mspId;
            return this;
        }
        /**
         * Setzt die Rolle des Akteurs.
         *
         * @param role Die Rolle des Akteurs
         * @return Die Builder-Instanz für Method-Chaining
         */
        public Builder role(final String role) {
            this.bRole = role;
            return this;
        }
        /**
         * Setzt den Status des Akteurs.
         *
         * @param status Der Status des Akteurs
         * @return Die Builder-Instanz für Method-Chaining
         */
        public Builder status(final String status) {
            this.bStatus = status;
            return this;
        }
        /**
         * Setzt die ID des genehmigenden Akteurs.
         *
         * @param approvedBy Die ID des genehmigenden Akteurs
         * @return Die Builder-Instanz für Method-Chaining
         */
        public Builder approvedBy(final String approvedBy) {
            this.bApprovedBy = approvedBy;
            return this;
        }
        /**
         * Setzt die Zertifikat-ID des Akteurs.
         *
         * @param certId Die Zertifikat-ID des Akteurs
         * @return Die Builder-Instanz für Method-Chaining
         */
        public Builder certId(final String certId) {
            this.bCertId = certId;
            return this;
        }

        /**
         * Erstellt eine neue Actor-Instanz mit den konfigurierten Werten.
         *
         * @return Die erstellte Actor-Instanz
         */
        public Actor build() {
            return new Actor(this);
        }
    }
}

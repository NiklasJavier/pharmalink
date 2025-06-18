package de.jklein.fabric.assets;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@DataType()
public final class DrugUnit {
    private static final Genson GENSON = new Genson();

    @Property()
    private final String id;
    @Property()
    private final String batchId;
    @Property()
    private final String drugId;
    @Property()
    private final String owner;
    @Property()
    private final String manufacturerId;
    @Property()
    private final String description;
    @Property()
    private final List<String> tags;
    @Property()
    private final String currentState;
    @Property()
    private final String dispensedBy;
    @Property()
    private final String dispensedTo;
    @Property()
    private final String dispensingTimestamp;
    @Property()
    private final List<String> temperatureReadings;

    private DrugUnit() {
        this.id = "";
        this.batchId = "";
        this.drugId = "";
        this.owner = "";
        this.manufacturerId = "";
        this.description = "";
        this.tags = new ArrayList<>();
        this.currentState = "";
        this.dispensedBy = "";
        this.dispensedTo = "";
        this.dispensingTimestamp = "";
        this.temperatureReadings = new ArrayList<>();
    }

    private DrugUnit(final Builder builder) {
        this.id = builder.bId;
        this.batchId = builder.bBatchId;
        this.drugId = builder.bDrugId;
        this.owner = builder.bOwner;
        this.manufacturerId = builder.bManufacturerId;
        this.description = builder.bDescription;
        this.tags = builder.bTags;
        this.currentState = builder.bCurrentState;
        this.dispensedBy = builder.bDispensedBy;
        this.dispensedTo = builder.bDispensedTo;
        this.dispensingTimestamp = builder.bDispensingTimestamp;
        this.temperatureReadings = builder.bTemperatureReadings;
    }

    public String getId() {
        return id;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getDrugId() {
        return drugId;
    }

    public String getOwner() {
        return owner;
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

    public String getCurrentState() {
        return currentState;
    }

    public String getDispensedBy() {
        return dispensedBy;
    }

    public String getDispensedTo() {
        return dispensedTo;
    }

    public String getDispensingTimestamp() {
        return dispensingTimestamp;
    }

    public List<String> getTemperatureReadings() {
        return temperatureReadings;
    }

    public String toJSONString() {
        return GENSON.serialize(this);
    }

    public static DrugUnit fromJSONString(final String json) {
        return GENSON.deserialize(json, DrugUnit.class);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DrugUnit drugUnit = (DrugUnit) o;
        return Objects.equals(id, drugUnit.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static final class Builder {
        private final String bId;
        private final String bBatchId;
        private final String bDrugId;
        private String bOwner;
        private String bManufacturerId;
        private String bDescription;
        private List<String> bTags = new ArrayList<>();
        private String bCurrentState;
        private String bDispensedBy;
        private String bDispensedTo;
        private String bDispensingTimestamp;
        private List<String> bTemperatureReadings = new ArrayList<>();

        public Builder(final String id, final String batchId, final String drugId) {
            this.bId = id;
            this.bBatchId = batchId;
            this.bDrugId = drugId;
        }

        /**
         * Setzt den Eigentümer der Medikamenteneinheit.
         *
         * @param owner Die ID des Eigentümers
         * @return Die Builder-Instanz für Method-Chaining
         */
        public Builder owner(final String owner) {
            this.bOwner = owner;
            return this;
        }

        /**
         * Setzt die Hersteller-ID der Medikamenteneinheit.
         *
         * @param manufacturerId Die ID des Herstellers
         * @return Die Builder-Instanz für Method-Chaining
         */
        public Builder manufacturerId(final String manufacturerId) {
            this.bManufacturerId = manufacturerId;
            return this;
        }

        /**
         * Setzt die Beschreibung der Medikamenteneinheit.
         *
         * @param description Die Beschreibung
         * @return Die Builder-Instanz für Method-Chaining
         */
        public Builder description(final String description) {
            this.bDescription = description;
            return this;
        }

        /**
         * Setzt die Tags der Medikamenteneinheit.
         *
         * @param tags Eine Liste von Tags
         * @return Die Builder-Instanz für Method-Chaining
         */
        public Builder tags(final List<String> tags) {
            this.bTags = tags;
            return this;
        }

        /**
         * Setzt den aktuellen Status der Medikamenteneinheit.
         *
         * @param currentState Der aktuelle Status
         * @return Die Builder-Instanz für Method-Chaining
         */
        public Builder currentState(final String currentState) {
            this.bCurrentState = currentState;
            return this;
        }

        /**
         * Setzt den Abgeber der Medikamenteneinheit.
         *
         * @param dispensedBy Die ID des Abgebers
         * @return Die Builder-Instanz für Method-Chaining
         */
        public Builder dispensedBy(final String dispensedBy) {
            this.bDispensedBy = dispensedBy;
            return this;
        }

        /**
         * Setzt den Empfänger der Medikamenteneinheit.
         *
         * @param dispensedTo Die ID des Empfängers
         * @return Die Builder-Instanz für Method-Chaining
         */
        public Builder dispensedTo(final String dispensedTo) {
            this.bDispensedTo = dispensedTo;
            return this;
        }

        /**
         * Setzt den Zeitstempel der Abgabe der Medikamenteneinheit.
         *
         * @param dispensingTimestamp Der Zeitstempel der Abgabe
         * @return Die Builder-Instanz für Method-Chaining
         */
        public Builder dispensingTimestamp(final String dispensingTimestamp) {
            this.bDispensingTimestamp = dispensingTimestamp;
            return this;
        }

        /**
         * Setzt die Temperaturmessungen der Medikamenteneinheit.
         *
         * @param temperatureReadings Eine Liste von Temperaturmessungen
         * @return Die Builder-Instanz für Method-Chaining
         */
        public Builder temperatureReadings(final List<String> temperatureReadings) {
            this.bTemperatureReadings = temperatureReadings;
            return this;
        }

        /**
         * Fügt eine Temperaturmessung zur Liste der Messungen hinzu.
         *
         * @param reading Die neue Temperaturmessung
         * @return Die Builder-Instanz für Method-Chaining
         */
        public Builder addTemperatureReading(final String reading) {
            if (this.bTemperatureReadings == null) {
                this.bTemperatureReadings = new ArrayList<>();
            }
            this.bTemperatureReadings.add(reading);
            return this;
        }

        /**
         * Erstellt eine neue DrugUnit-Instanz mit den konfigurierten Werten.
         *
         * @return Die erstellte DrugUnit-Instanz
         */
        public DrugUnit build() {
            return new DrugUnit(this);
        }
    }
}

package de.jklein.fabric.models;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@DataType()
public final class Unit {
    @Property()
    private String unitId;

    @Property()
    private String medId;

    @Property()
    private String chargeBezeichnung;

    @Property()
    private String ipfsLink;

    @Property()
    private String currentOwnerActorId;

    @Property()
    private List<TransferEntry> transferHistory;

    @Property()
    private List<TemperatureReading> temperatureReadings;

    @Property()
    private boolean isConsumed;

    @Property()
    private String consumedRefId;

    @Property()
    private String docType;

    public Unit() {
        this.transferHistory = new ArrayList<>();
        this.temperatureReadings = new ArrayList<>();
        this.isConsumed = false;
        this.consumedRefId = "";
        this.docType = "unit";
    }

    public Unit(@JsonProperty("unitId") final String unitId,
                @JsonProperty("medId") final String medId,
                @JsonProperty("chargeBezeichnung") final String chargeBezeichnung,
                @JsonProperty("ipfsLink") final String ipfsLink,
                @JsonProperty("currentOwnerActorId") final String currentOwnerActorId) {
        this.unitId = unitId;
        this.medId = medId;
        this.chargeBezeichnung = chargeBezeichnung;
        this.ipfsLink = ipfsLink;
        this.currentOwnerActorId = currentOwnerActorId;
        this.transferHistory = new ArrayList<>();
        this.temperatureReadings = new ArrayList<>();
        this.docType = "unit";
        this.isConsumed = false;
        this.consumedRefId = "";
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(final String newUnitId) {
        this.unitId = newUnitId;
    }

    public String getMedId() {
        return medId;
    }

    public void setMedId(final String newMedId) {
        this.medId = newMedId;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(final String newDocType) {
        this.docType = newDocType;
    }

    public String getChargeBezeichnung() {
        return chargeBezeichnung;
    }

    public void setChargeBezeichnung(final String newChargeBezeichnung) {
        this.chargeBezeichnung = newChargeBezeichnung;
    }

    public String getIpfsLink() {
        return ipfsLink;
    }

    public void setIpfsLink(final String newIpfsLink) {
        this.ipfsLink = newIpfsLink;
    }

    public String getCurrentOwnerActorId() {
        return currentOwnerActorId;
    }

    public void setCurrentOwnerActorId(final String newCurrentOwnerActorId) {
        this.currentOwnerActorId = newCurrentOwnerActorId;
    }

    public List<TransferEntry> getTransferHistory() {
        return Collections.unmodifiableList(transferHistory);
    }

    public void addTransferEntry(final String fromActorId, final String toActorId, final String timestamp) {
        this.transferHistory.add(new TransferEntry(fromActorId, toActorId, timestamp));
    }

    public List<TemperatureReading> getTemperatureReadings() {
        return Collections.unmodifiableList(temperatureReadings);
    }

    public void addTemperatureReading(final String timestamp, final String temperature) {
        this.temperatureReadings.add(new TemperatureReading(timestamp, temperature));
    }

    public boolean getIsConsumed() {
        return isConsumed;
    }

    public void setIsConsumed(final boolean newIsConsumed) {
        isConsumed = newIsConsumed;
    }

    public String getConsumedRefId() {
        return consumedRefId;
    }

    public void setConsumedRefId(final String newConsumedRefId) {
        this.consumedRefId = newConsumedRefId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Unit unit = (Unit) o;
        return getIsConsumed() == unit.getIsConsumed()
                && Objects.equals(getUnitId(), unit.getUnitId())
                && Objects.equals(getMedId(), unit.getMedId())
                && Objects.equals(getChargeBezeichnung(), unit.getChargeBezeichnung())
                && Objects.equals(getIpfsLink(), unit.getIpfsLink())
                && Objects.equals(getCurrentOwnerActorId(), unit.getCurrentOwnerActorId())
                && Objects.equals(getTransferHistory(), unit.getTransferHistory())
                && Objects.equals(getTemperatureReadings(), unit.getTemperatureReadings())
                && Objects.equals(getConsumedRefId(), unit.getConsumedRefId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUnitId(), getMedId(), getChargeBezeichnung(), getIpfsLink(),
                getCurrentOwnerActorId(), transferHistory, temperatureReadings, getIsConsumed(), getConsumedRefId());
    }

    @Override
    public String toString() {
        return "Unit{"
                + "unitId='" + unitId + '\''
                + ", medId='" + medId + '\''
                + ", chargeBezeichnung='" + chargeBezeichnung + '\''
                + ", ipfsLink='" + ipfsLink + '\''
                + ", currentOwnerActorId='" + currentOwnerActorId + '\''
                + ", transferHistory=" + transferHistory
                + ", temperatureReadings=" + temperatureReadings
                + ", isConsumed=" + isConsumed
                + ", consumedRefId='" + consumedRefId + '\''
                + '}';
    }

    @DataType()
    public static final class TransferEntry {
        @Property()
        private String fromActorId;
        @Property()
        private String toActorId;
        @Property()
        private String timestamp;

        public TransferEntry() {
        }

        public TransferEntry(@JsonProperty("fromActorId") final String fromActorId,
                             @JsonProperty("toActorId") final String toActorId,
                             @JsonProperty("timestamp") final String timestamp) {
            this.fromActorId = fromActorId;
            this.toActorId = toActorId;
            this.timestamp = timestamp;
        }

        public String getFromActorId() {
            return fromActorId;
        }

        public void setFromActorId(final String newFromActorId) {
            this.fromActorId = newFromActorId;
        }

        public String getToActorId() {
            return toActorId;
        }

        public void setToActorId(final String newToActorId) {
            this.toActorId = newToActorId;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(final String newTimestamp) {
            this.timestamp = newTimestamp;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TransferEntry that = (TransferEntry) o;
            return Objects.equals(getFromActorId(), that.getFromActorId())
                    && Objects.equals(getToActorId(), that.getToActorId())
                    && Objects.equals(getTimestamp(), that.getTimestamp());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getFromActorId(), getToActorId(), getTimestamp());
        }

        @Override
        public String toString() {
            return "TransferEntry{"
                    + "fromActorId='" + fromActorId + '\''
                    + ", toActorId='" + toActorId + '\''
                    + ", timestamp='" + timestamp + '\''
                    + '}';
        }
    }

    @DataType()
    public static final class TemperatureReading {
        @Property()
        private String timestamp;
        @Property()
        private String temperature;

        public TemperatureReading() {
        }

        public TemperatureReading(@JsonProperty("timestamp") final String timestamp,
                                  @JsonProperty("temperature") final String temperature) {
            this.timestamp = timestamp;
            this.temperature = temperature;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(final String newTimestamp) {
            this.timestamp = newTimestamp;
        }

        public String getTemperature() {
            return temperature;
        }

        public void setTemperature(final String newTemperature) {
            this.temperature = newTemperature;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TemperatureReading that = (TemperatureReading) o;
            return Objects.equals(getTimestamp(), that.getTimestamp())
                    && Objects.equals(getTemperature(), that.getTemperature());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getTimestamp(), getTemperature());
        }

        @Override
        public String toString() {
            return "TemperatureReading{"
                    + "timestamp='" + timestamp + '\''
                    + ", temperature='" + temperature + '\''
                    + '}';
        }
    }
}

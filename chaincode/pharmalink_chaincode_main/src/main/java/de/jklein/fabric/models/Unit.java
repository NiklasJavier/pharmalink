package de.jklein.fabric.models;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.TreeMap;

@DataType()
public final class Unit {

    @Property()
    private String unitId; // Eindeutige ID der Einheit (MedikamentID-Charge-Zähler)

    @Property()
    private String medId; // Referenz zur Medikamenten-ID

    @Property()
    private String chargeBezeichnung; // Bezeichnung der Charge

    @Property()
    private String ipfsLink; // IPFS Link für die Einheit

    @Property()
    private String currentOwnerActorId; // Aktueller Eigentümer der Einheit

    @Property()
    private List<Map<String, String>> temperatureReadings; // Liste von Temperaturmesswerten (Timestamp, Temperatur)

    @Property()
    private List<Map<String, String>> transferHistory; // Liste zur Nachverfolgung von Besitzwechseln (from, to, timestamp)

    @Property()
    private String docType; // Hinzugefügt für CouchDB Abfragen zur Typisierung

    // Leerer Konstruktor für Genson Deserialisierung
    public Unit() {
        this.temperatureReadings = new ArrayList<>();
        this.transferHistory = new ArrayList<>();
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
        this.temperatureReadings = new ArrayList<>();
        this.transferHistory = new ArrayList<>();
        this.docType = "unit";
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

    public List<Map<String, String>> getTemperatureReadings() {
        return temperatureReadings;
    }

    public void addTemperatureReading(final String timestamp, final String temperature) {
        Map<String, String> reading = new TreeMap<>();
        reading.put("timestamp", timestamp);
        reading.put("temperature", temperature);
        this.temperatureReadings.add(reading);
    }

    public List<Map<String, String>> getTransferHistory() {
        return transferHistory;
    }

    public void addTransferEntry(final String fromActorId, final String toActorId, final String transferTimestamp) {
        Map<String, String> transferEntry = new TreeMap<>();
        transferEntry.put("from", fromActorId);
        transferEntry.put("to", toActorId);
        transferEntry.put("timestamp", transferTimestamp);
        this.transferHistory.add(transferEntry);
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(final String newDocType) {
        this.docType = newDocType;
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
        return Objects.equals(getUnitId(), unit.getUnitId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUnitId());
    }

    @Override
    public String toString() {
        return "Unit{"
                + "unitId='" + unitId + '\''
                + ", medId='" + medId + '\''
                + ", chargeBezeichnung='" + chargeBezeichnung + '\''
                + ", ipfsLink='" + ipfsLink + '\''
                + ", currentOwnerActorId='" + currentOwnerActorId + '\''
                + ", temperatureReadings=" + temperatureReadings
                + ", transferHistory=" + transferHistory
                + ", docType='" + docType + '\''
                + '}';
    }
}

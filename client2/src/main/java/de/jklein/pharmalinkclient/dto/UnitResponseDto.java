package de.jklein.pharmalinkclient.dto;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UnitResponseDto {
    private String unitId;
    private String medId;
    private String chargeBezeichnung;
    private String ipfsLink;
    private String currentOwnerActorId;
    private List<Map<String, String>> temperatureReadings;
    private List<Map<String, String>> transferHistory;
    private Map<String, Object> ipfsData;

    private boolean consumed;
    private String consumedRefId;

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public String getMedId() {
        return medId;
    }

    public void setMedId(String medId) {
        this.medId = medId;
    }

    public String getChargeBezeichnung() {
        return chargeBezeichnung;
    }

    public void setChargeBezeichnung(String chargeBezeichnung) {
        this.chargeBezeichnung = chargeBezeichnung;
    }

    public String getIpfsLink() {
        return ipfsLink;
    }

    public void setIpfsLink(String ipfsLink) {
        this.ipfsLink = ipfsLink;
    }

    public String getCurrentOwnerActorId() {
        return currentOwnerActorId;
    }

    public void setCurrentOwnerActorId(String currentOwnerActorId) {
        this.currentOwnerActorId = currentOwnerActorId;
    }

    public List<Map<String, String>> getTemperatureReadings() {
        return temperatureReadings;
    }

    public void setTemperatureReadings(List<Map<String, String>> temperatureReadings) {
        this.temperatureReadings = temperatureReadings;
    }

    public List<Map<String, String>> getTransferHistory() {
        return transferHistory;
    }

    public void setTransferHistory(List<Map<String, String>> transferHistory) {
        this.transferHistory = transferHistory;
    }

    public Map<String, Object> getIpfsData() {
        return ipfsData;
    }

    public void setIpfsData(Map<String, Object> ipfsData) {
        this.ipfsData = ipfsData;
    }
    
    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    public String getConsumedRefId() {
        return consumedRefId;
    }

    public void setConsumedRefId(String consumedRefId) {
        this.consumedRefId = consumedRefId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnitResponseDto that = (UnitResponseDto) o;
        return Objects.equals(unitId, that.unitId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unitId);
    }
}
package de.jklein.pharmalinkclient.dto;

public class TransferUnitRangeRequestDto {
    private String medId;
    private String chargeBezeichnung;
    private int startCounter;
    private int endCounter;
    private String newOwnerId;
    private String transferTimestamp;

    public TransferUnitRangeRequestDto() {
    }

    public TransferUnitRangeRequestDto(String medId, String chargeBezeichnung, int startCounter, int endCounter, String newOwnerId) {
        this.medId = medId;
        this.chargeBezeichnung = chargeBezeichnung;
        this.startCounter = startCounter;
        this.endCounter = endCounter;
        this.newOwnerId = newOwnerId;
    }

    public String getMedId() { return medId; }
    public void setMedId(String medId) { this.medId = medId; }
    public String getChargeBezeichnung() { return chargeBezeichnung; }
    public void setChargeBezeichnung(String chargeBezeichnung) { this.chargeBezeichnung = chargeBezeichnung; }
    public int getStartCounter() { return startCounter; }
    public void setStartCounter(int startCounter) { this.startCounter = startCounter; }
    public int getEndCounter() { return endCounter; }
    public void setEndCounter(int endCounter) { this.endCounter = endCounter; }
    public String getNewOwnerId() { return newOwnerId; }
    public void setNewOwnerId(String newOwnerId) { this.newOwnerId = newOwnerId; }

    public String getTransferTimestamp() { return transferTimestamp; }
    public void setTransferTimestamp(String transferTimestamp) { this.transferTimestamp = transferTimestamp; }
}
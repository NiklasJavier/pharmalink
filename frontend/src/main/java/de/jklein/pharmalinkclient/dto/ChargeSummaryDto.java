package de.jklein.pharmalinkclient.dto;

public class ChargeSummaryDto {
    private String chargeBezeichnung;
    private long anzahl;

    public ChargeSummaryDto() {
    }

    public ChargeSummaryDto(String chargeBezeichnung, long anzahl) {
        this.chargeBezeichnung = chargeBezeichnung;
        this.anzahl = anzahl;
    }

    public String getChargeBezeichnung() {
        return chargeBezeichnung;
    }

    public void setChargeBezeichnung(String chargeBezeichnung) {
        this.chargeBezeichnung = chargeBezeichnung;
    }

    public long getAnzahl() {
        return anzahl;
    }

    public void setAnzahl(long anzahl) {
        this.anzahl = anzahl;
    }
}
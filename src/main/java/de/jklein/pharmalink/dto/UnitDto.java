package de.jklein.pharmalink.dto;

import java.util.List;
import java.util.Objects;

/**
 * (chaincode/pharmalink_chaincode_main/src/main/java/de/jklein/fabric/models/Unit.java).
 */
public class UnitDto {

    private String unitId;
    private String medId;
    private String chargeBezeichnung;
    private int anzahl;
    private String ipfsLink;
    private String lagerort;
    private String status;
    private String letzteAenderung;
    private List<String> transportHistorie;
    public UnitDto() {
    }

    public UnitDto(String unitId, String medId, String chargeBezeichnung, int anzahl, String ipfsLink,
                   String lagerort, String status, String letzteAenderung, List<String> transportHistorie) {
        this.unitId = unitId;
        this.medId = medId;
        this.chargeBezeichnung = chargeBezeichnung;
        this.anzahl = anzahl;
        this.ipfsLink = ipfsLink;
        this.lagerort = lagerort;
        this.status = status;
        this.letzteAenderung = letzteAenderung;
        this.transportHistorie = transportHistorie;
    }

    public String getUnitId() { return unitId; }
    public String getMedId() { return medId; }
    public String getChargeBezeichnung() { return chargeBezeichnung; }
    public int getAnzahl() { return anzahl; }
    public String getIpfsLink() { return ipfsLink; }
    public String getLagerort() { return lagerort; }
    public String getStatus() { return status; }
    public String getLetzteAenderung() { return letzteAenderung; }
    public List<String> getTransportHistorie() { return transportHistorie; }

    public void setUnitId(String unitId) { this.unitId = unitId; }
    public void setMedId(String medId) { this.medId = medId; }
    public void setChargeBezeichnung(String chargeBezeichnung) { this.chargeBezeichnung = chargeBezeichnung; }
    public void setAnzahl(int anzahl) { this.anzahl = anzahl; }
    public void setIpfsLink(String ipfsLink) { this.ipfsLink = ipfsLink; }
    public void setLagerort(String lagerort) { this.lagerort = lagerort; }
    public void setStatus(String status) { this.status = status; }
    public void setLetzteAenderung(String letzteAenderung) { this.letzteAenderung = letzteAenderung; }
    public void setTransportHistorie(List<String> transportHistorie) { this.transportHistorie = transportHistorie; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnitDto unitDto = (UnitDto) o;
        return anzahl == unitDto.anzahl &&
                Objects.equals(unitId, unitDto.unitId) &&
                Objects.equals(medId, unitDto.medId) &&
                Objects.equals(chargeBezeichnung, unitDto.chargeBezeichnung) &&
                Objects.equals(ipfsLink, unitDto.ipfsLink) &&
                Objects.equals(lagerort, unitDto.lagerort) &&
                Objects.equals(status, unitDto.status) &&
                Objects.equals(letzteAenderung, unitDto.letzteAenderung) &&
                Objects.equals(transportHistorie, unitDto.transportHistorie);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unitId, medId, chargeBezeichnung, anzahl, ipfsLink, lagerort, status, letzteAenderung, transportHistorie); // docType hinzufügen
    }

    @Override
    public String toString() {
        return "UnitDto{" +
                "unitId='" + unitId + '\'' +
                ", medId='" + medId + '\'' +
                ", chargeBezeichnung='" + chargeBezeichnung + '\'' +
                ", anzahl=" + anzahl +
                ", ipfsLink='" + ipfsLink + '\'' +
                ", lagerort='" + lagerort + '\'' +
                ", status='" + status + '\'' +
                ", letzteAenderung='" + letzteAenderung + '\'' +
                ", transportHistorie=" + transportHistorie +
                '}';
    }
}
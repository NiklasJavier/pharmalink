package de.jklein.pharmalink.dto;

import java.util.Objects;

/**
 * (chaincode/pharmalink_chaincode_main/src/main/java/de/jklein/fabric/models/Medikament.java).
 */
public class MedikamentDto {

    private String medId;
    private String bezeichnung;
    private String infoblattHash;
    private String ipfsLink;
    private String herstellerId;

    public MedikamentDto() {
    }

    public MedikamentDto(String medId, String bezeichnung, String infoblattHash, String ipfsLink, String herstellerId) {
        this.medId = medId;
        this.bezeichnung = bezeichnung;
        this.infoblattHash = infoblattHash;
        this.ipfsLink = ipfsLink;
        this.herstellerId = herstellerId;
    }

    public String getMedId() {
        return medId;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public String getInfoblattHash() {
        return infoblattHash;
    }

    public String getIpfsLink() {
        return ipfsLink;
    }

    public String getHerstellerId() {
        return herstellerId;
    }

    public void setMedId(String medId) {
        this.medId = medId;
    }

    public void setBezeichnung(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    public void setInfoblattHash(String infoblattHash) {
        this.infoblattHash = infoblattHash;
    }

    public void setIpfsLink(String ipfsLink) {
        this.ipfsLink = ipfsLink;
    }

    public void setHerstellerId(String herstellerId) {
        this.herstellerId = herstellerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MedikamentDto that = (MedikamentDto) o;
        return Objects.equals(medId, that.medId) &&
                Objects.equals(bezeichnung, that.bezeichnung) &&
                Objects.equals(infoblattHash, that.infoblattHash) &&
                Objects.equals(ipfsLink, that.ipfsLink) &&
                Objects.equals(herstellerId, that.herstellerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(medId, bezeichnung, infoblattHash, ipfsLink, herstellerId);
    }

    @Override
    public String toString() {
        return "MedikamentDto{" +
                "medId='" + medId + '\'' +
                ", bezeichnung='" + bezeichnung + '\'' +
                ", infoblattHash='" + infoblattHash + '\'' +
                ", ipfsLink='" + ipfsLink + '\'' +
                ", herstellerId='" + herstellerId + '\'' +
                '}';
    }
}
package de.jklein.pharmalinkclient.dto;

import java.util.Map;
import java.util.Objects;

public class MedikamentResponseDto {
    private String medId;
    private String herstellerId;
    private String bezeichnung;
    private String ipfsLink;
    private String status;
    private Map<String, Object> ipfsData;
    
    public String getMedId() {
        return medId;
    }

    public void setMedId(String medId) {
        this.medId = medId;
    }

    public String getHerstellerId() {
        return herstellerId;
    }

    public void setHerstellerId(String herstellerId) {
        this.herstellerId = herstellerId;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public void setBezeichnung(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    public String getIpfsLink() {
        return ipfsLink;
    }

    public void setIpfsLink(String ipfsLink) {
        this.ipfsLink = ipfsLink;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getIpfsData() {
        return ipfsData;
    }

    public void setIpfsData(Map<String, Object> ipfsData) {
        this.ipfsData = ipfsData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MedikamentResponseDto that = (MedikamentResponseDto) o;
        return Objects.equals(medId, that.medId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(medId);
    }
}
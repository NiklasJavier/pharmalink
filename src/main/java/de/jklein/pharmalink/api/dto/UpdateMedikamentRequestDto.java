package de.jklein.pharmalink.api.dto;

import java.util.Map;

public class UpdateMedikamentRequestDto {

    private String bezeichnung;
    private String infoblattHash;
    private Map<String, Object> ipfsData;

    public String getBezeichnung() {
        return bezeichnung;
    }

    public void setBezeichnung(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    public String getInfoblattHash() {
        return infoblattHash;
    }

    public void setInfoblattHash(String infoblattHash) {
        this.infoblattHash = infoblattHash;
    }

    public Map<String, Object> getIpfsData() {
        return ipfsData;
    }

    public void setIpfsData(Map<String, Object> ipfsData) {
        this.ipfsData = ipfsData;
    }
}
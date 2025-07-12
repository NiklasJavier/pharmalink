package de.jklein.pharmalinkclient.dto;

import java.util.Map;

public class UpdateMedikamentRequestDto {

    private String bezeichnung;
    private String infoblattHash;
    private Map<String, Object> ipfsData;

    public UpdateMedikamentRequestDto() {
    }

    public UpdateMedikamentRequestDto(String bezeichnung, String infoblattHash, Map<String, Object> ipfsData) {
        this.bezeichnung = bezeichnung;
        this.infoblattHash = infoblattHash;
        this.ipfsData = ipfsData;
    }
    
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
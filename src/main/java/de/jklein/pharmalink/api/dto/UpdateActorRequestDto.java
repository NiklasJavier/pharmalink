package de.jklein.pharmalink.api.dto;

import java.util.Map;

public class UpdateActorRequestDto {

    private String name;
    private Map<String, Object> ipfsData;

    // Getter and Setter

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getIpfsData() {
        return ipfsData;
    }

    public void setIpfsData(Map<String, Object> ipfsData) {
        this.ipfsData = ipfsData;
    }
}
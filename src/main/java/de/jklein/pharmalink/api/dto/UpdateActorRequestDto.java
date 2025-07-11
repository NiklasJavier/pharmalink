package de.jklein.pharmalink.api.dto;

import java.util.Map;

public class UpdateActorRequestDto {

    private String name;
    private String email;
    private Map<String, Object> ipfsData;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
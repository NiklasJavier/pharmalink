package de.jklein.pharmalinkclient.dto;

import java.util.Map;

public class ActorUpdateRequestDto {
    private String name;
    private String email;
    private Map<String, Object> ipfsData;

    public ActorUpdateRequestDto() {
    }

    public ActorUpdateRequestDto(String name, String email, Map<String, Object> ipfsData) {
        this.name = name;
        this.email = email;
        this.ipfsData = ipfsData;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {this.email = email;}

    public String getEmail() {return email;}

    public Map<String, Object> getIpfsData() {
        return ipfsData;
    }

    public void setIpfsData(Map<String, Object> ipfsData) {
        this.ipfsData = ipfsData;
    }
}
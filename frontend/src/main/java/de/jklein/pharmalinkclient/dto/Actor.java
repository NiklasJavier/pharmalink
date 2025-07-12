package de.jklein.pharmalinkclient.dto;

import java.util.Map;

public class Actor {
    private String actorId;
    private String bezeichnung;
    private String role;
    private String email;
    private String ipfsLink;
    private String docType;
    private Map<String, Object> ipfsData;

    public Actor() {}

    public Actor(String actorId, String bezeichnung, String role, String email, String ipfsLink, String docType, Map<String, Object> ipfsData) {
        this.actorId = actorId;
        this.bezeichnung = bezeichnung;
        this.role = role;
        this.email = email;
        this.ipfsLink = ipfsLink;
        this.docType = docType;
        this.ipfsData = ipfsData;
    }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getBezeichnung() { return bezeichnung; }
    public void setBezeichnung(String bezeichnung) { this.bezeichnung = bezeichnung; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getIpfsLink() { return ipfsLink; }
    public void setIpfsLink(String ipfsLink) { this.ipfsLink = ipfsLink; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public Map<String, Object> getIpfsData() { return ipfsData; }
    public void setIpfsData(Map<String, Object> ipfsData) { this.ipfsData = ipfsData; }
}
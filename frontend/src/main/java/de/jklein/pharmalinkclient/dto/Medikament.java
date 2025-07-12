package de.jklein.pharmalinkclient.dto;

import java.util.Map;

public class Medikament {
    private String medId;
    private String herstellerId;
    private String bezeichnung;
    private String infoblattHash;
    private String ipfsLink;
    private String status;
    private String approvedById;
    private Map<String, String> tags;
    private String docType;
    private Map<String, Object> ipfsData;

    public Medikament() {}

    public Medikament(String medId, String herstellerId, String bezeichnung, String infoblattHash, String ipfsLink, String status, String approvedById, Map<String, String> tags, String docType, Map<String, Object> ipfsData) {
        this.medId = medId;
        this.herstellerId = herstellerId;
        this.bezeichnung = bezeichnung;
        this.infoblattHash = infoblattHash;
        this.ipfsLink = ipfsLink;
        this.status = status;
        this.approvedById = approvedById;
        this.tags = tags;
        this.docType = docType;
        this.ipfsData = ipfsData;
    }

    public String getMedId() { return medId; }
    public void setMedId(String medId) { this.medId = medId; }
    public String getHerstellerId() { return herstellerId; }
    public void setHerstellerId(String herstellerId) { this.herstellerId = herstellerId; }
    public String getBezeichnung() { return bezeichnung; }
    public void setBezeichnung(String bezeichnung) { this.bezeichnung = bezeichnung; }
    public String getInfoblattHash() { return infoblattHash; }
    public void setInfoblattHash(String infoblattHash) { this.infoblattHash = infoblattHash; }
    public String getIpfsLink() { return ipfsLink; }
    public void setIpfsLink(String ipfsLink) { this.ipfsLink = ipfsLink; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getApprovedById() { return approvedById; }
    public void setApprovedById(String approvedById) { this.approvedById = approvedById; }
    public Map<String, String> getTags() { return tags; }
    public void setTags(Map<String, String> tags) { this.tags = tags; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public Map<String, Object> getIpfsData() { return ipfsData; }
    public void setIpfsData(Map<String, Object> ipfsData) { this.ipfsData = ipfsData; }
}
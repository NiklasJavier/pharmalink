package de.jklein.pharmalinkclient.dto; 

import java.time.LocalDateTime;
import java.util.Map;

public class Unit {
    private String unitId;
    private String medId;
    private String chargeBezeichnung;
    private String ownerId;
    private String currentOwnerId;
    private String ipfsLink;
    private String status;
    private LocalDateTime createdAt;
    private String createdById;
    private String docType;
    private Map<String, Object> ipfsData;

    public Unit() {}

    public Unit(String unitId, String medId, String chargeBezeichnung, String ownerId, String currentOwnerId, String ipfsLink, String status, LocalDateTime createdAt, String createdById, String docType, Map<String, Object> ipfsData) {
        this.unitId = unitId;
        this.medId = medId;
        this.chargeBezeichnung = chargeBezeichnung;
        this.ownerId = ownerId;
        this.currentOwnerId = currentOwnerId;
        this.ipfsLink = ipfsLink;
        this.status = status;
        this.createdAt = createdAt;
        this.createdById = createdById;
        this.docType = docType;
        this.ipfsData = ipfsData;
    }

    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public String getMedId() { return medId; }
    public void setMedId(String medId) { this.medId = medId; }
    public String getChargeBezeichnung() { return chargeBezeichnung; }
    public void setChargeBezeichnung(String chargeBezeichnung) { this.chargeBezeichnung = chargeBezeichnung; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getCurrentOwnerId() { return currentOwnerId; }
    public void setCurrentOwnerId(String currentOwnerId) { this.currentOwnerId = currentOwnerId; }
    public String getIpfsLink() { return ipfsLink; }
    public void setIpfsLink(String ipfsLink) { this.ipfsLink = ipfsLink; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedById() { return createdById; }
    public void setCreatedById(String createdById) { this.createdById = createdById; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public Map<String, Object> getIpfsData() { return ipfsData; }
    public void setIpfsData(Map<String, Object> ipfsData) { this.ipfsData = ipfsData; }
}
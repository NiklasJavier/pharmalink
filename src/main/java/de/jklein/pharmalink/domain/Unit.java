package de.jklein.pharmalink.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap; // Hinzugefügt für HashMap Initialisierung

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Unit {
    private String unitId;
    private String medId;
    private String chargeBezeichnung;
    private String ownerId;
    private String currentOwnerId; // Kann für detailliertere Nachverfolgung verwendet werden
    private String ipfsLink;
    private String status; // z.B. "erstellt", "im_transport", "angekommen", "ausgegeben"
    private LocalDateTime createdAt;
    private String createdById;
    private String docType;

    // NEU: Feld für die angereicherten IPFS-Daten
    private Map<String, Object> ipfsData;

    // Beispiel-Konstruktor (falls nicht durch Lombok @AllArgsConstructor abgedeckt und benötigt)
    public Unit(String unitId, String medId, String chargeBezeichnung, String ownerId, String currentOwnerId, String ipfsLink, String status, LocalDateTime createdAt, String createdById) {
        this.unitId = unitId;
        this.medId = medId;
        this.chargeBezeichnung = chargeBezeichnung;
        this.ownerId = ownerId;
        this.currentOwnerId = currentOwnerId;
        this.ipfsLink = ipfsLink;
        this.status = status;
        this.createdAt = createdAt;
        this.createdById = createdById;
        this.docType = "unit";
        this.ipfsData = new HashMap<>(); // Standardinitialisierung
    }
}
package de.jklein.pharmalink.domain;

import java.util.List;

public class Shipment extends TrackableAsset {

    private final List<String> medicationAssetIds;
    private String status;

    public Shipment(String assetId, String owner, List<String> medicationAssetIds, String status) {
        super(assetId, owner);
        this.medicationAssetIds = medicationAssetIds;
        this.status = status;
    }

    @Override
    public String getAssetType() {
        return "SHIPMENT"; // Muss mit dem Namen in @JsonSubTypes übereinstimmen
    }

    // Spezifische Getter/Setter für diese Klasse
    public List<String> getMedicationAssetIds() { return medicationAssetIds; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
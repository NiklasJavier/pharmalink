package de.jklein.pharmalink.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// Diese Annotationen ermöglichen die dynamische Umwandlung von JSON in die korrekte Unterklasse.
// Es wird das Feld "assetType" im JSON erwartet, um den Typ zu bestimmen.
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "assetType"
)
@JsonSubTypes({
        // Hier registrieren wir alle konkreten Unterklassen mit ihrem eindeutigen Namen.
        @JsonSubTypes.Type(value = Medication.class, name = "MEDICATION"),
        @JsonSubTypes.Type(value = Shipment.class, name = "SHIPMENT")
})
public abstract class TrackableAsset {

    protected final String assetId;
    protected String owner;

    protected TrackableAsset(String assetId, String owner) {
        this.assetId = assetId;
        this.owner = owner;
    }

    // Jede Unterklasse MUSS diese Methode implementieren, um ihren Typ zurückzugeben.
    public abstract String getAssetType();

    // Gemeinsame Getter für alle Assets.
    public String getAssetId() {
        return assetId;
    }
    public String getOwner() {
        return owner;
    }
}
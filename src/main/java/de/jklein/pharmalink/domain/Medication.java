package de.jklein.pharmalink.domain;

public class Medication extends TrackableAsset {

    private final String name;
    private final String dosage;
    private final String manufacturer;
    private final double appraisedValue;

    public Medication(String assetId, String owner, String name, String dosage, String manufacturer, double appraisedValue) {
        super(assetId, owner); // Ruft den Konstruktor der Basisklasse auf
        this.name = name;
        this.dosage = dosage;
        this.manufacturer = manufacturer;
        this.appraisedValue = appraisedValue;
    }

    @Override
    public String getAssetType() {
        return "MEDICATION"; // Muss mit dem Namen in @JsonSubTypes übereinstimmen
    }

    // Spezifische Getter für diese Klasse
    public String getName() { return name; }
    public String getDosage() { return dosage; }
    public String getManufacturer() { return manufacturer; }
    public double getAppraisedValue() { return appraisedValue; }
}
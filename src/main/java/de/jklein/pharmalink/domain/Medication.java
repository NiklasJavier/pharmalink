package de.jklein.pharmalink.domain;

public class Medication extends TrackableAsset {

    private final String name;
    private final String dosage;
    private final String manufacturer;
    private double appraisedValue;

    public Medication(String assetId, String owner, String name, String dosage, String manufacturer, double appraisedValue) {
        super(assetId, owner); // Ruft Konstruktor der Basisklasse auf
        this.name = name;
        this.dosage = dosage;
        this.manufacturer = manufacturer;
        this.appraisedValue = appraisedValue;
    }

    @Override
    public String getAssetType() {
        return "Medication";
    }

    // Spezifische Getter/Setter für Medication
    public String getName() { return name; }
    // ... weitere Getter/Setter
}
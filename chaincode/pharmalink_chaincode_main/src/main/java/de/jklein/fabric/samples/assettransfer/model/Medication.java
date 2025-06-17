// src/main/java/de/jklein/fabric/samples/assettransfer/model/Medication.java
package de.jklein.fabric.samples.assettransfer.model;

import com.owlike.genson.annotation.JsonProperty;
import de.jklein.fabric.samples.assettransfer.permission.RoleConstants;
import java.util.Collections;
import java.util.List;
import java.util.Objects; //

/**
 * Repräsentiert ein Medikament in der Lieferkette (Produktdefinition).
 */
public final class Medication implements IAsset { //

    private final String medicationId; //
    private final String gtin; //
    private final String productName; // Umbenannt von 'name' zu 'productName' für Klarheit
    private final String productManufacturerOrgId; // MSP ID des Herstellers der Organisation
    private final String creatorActorId; // NEU: Die Akteur-ID des erstellenden Herstellers
    private final String status; //
    private final List<Tag> classificationTags; // NEU: Liste von Klassifizierungstags (verwende 'Tag' statt 'AssetTag')

    public Medication(
            @JsonProperty("medicationId") final String medicationId,
            @JsonProperty("gtin") final String gtin,
            @JsonProperty("productName") final String productName,
            @JsonProperty("productManufacturerOrgId") final String productManufacturerOrgId,
            @JsonProperty("creatorActorId") final String creatorActorId,
            @JsonProperty("status") final String status,
            @JsonProperty("classificationTags") final List<Tag> classificationTags) {
        this.medicationId = medicationId;
        this.gtin = gtin;
        this.productName = productName;
        this.productManufacturerOrgId = productManufacturerOrgId;
        this.creatorActorId = creatorActorId;
        this.status = status;
        this.classificationTags = classificationTags != null ? List.copyOf(classificationTags) : Collections.emptyList();
    }

    @Override
    public String getKey() {
        return "MED_" + medicationId; // Annahme: Präfix für Medikamente
    }

    @Override
    public String getStatus() {
        return status; //
    }

    @Override
    public String getRegulatoryStatus() {
        // Der regulatorische Status wird primär durch die isLockedByRegulator() Methode und die Tags abgebildet.
        // Falls ein "Freigegeben" Status hier regulatorisch ist, kann dieser zurückgegeben werden.
        return status; // Hier könnte man den internen Status als "regulatorisch relevant" zurückgeben
    }

    @Override
    public boolean isLockedByRegulator() {
        // Ein Medikament ist gesperrt, wenn sein Status GESPERRT ist oder
        // wenn ein Klassifizierungstag eine blockierende Eigenschaft hat.
        if (RoleConstants.GESPERRT.equals(this.status)) { //
            return true;
        }
        for (Tag tag : classificationTags) {
            if (tag.isBlocking()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getAssociatedActorId() {
        return creatorActorId; // Der Ersteller ist der primär assoziierte Akteur
    }

    public String getMedicationId() {
        return medicationId; //
    }

    public String getGtin() {
        return gtin; //
    }

    public String getProductName() {
        return productName;
    }

    public String getProductManufacturerOrgId() {
        return productManufacturerOrgId;
    }

    public String getCreatorActorId() {
        return creatorActorId;
    }

    public List<Tag> getClassificationTags() {
        return classificationTags;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        Medication other = (Medication) obj;
        return Objects.deepEquals(
                new String[]{getMedicationId(), getGtin(), getProductName(), getProductManufacturerOrgId(), getCreatorActorId(), getStatus()},
                new String[]{other.getMedicationId(), other.getGtin(), other.getProductName(), other.getProductManufacturerOrgId(), other.getCreatorActorId(), other.getStatus()}
        ) && Objects.equals(getClassificationTags(), other.getClassificationTags());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMedicationId(), getGtin(), getProductName(), getProductManufacturerOrgId(), getCreatorActorId(), getStatus(), getClassificationTags());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [medicationId=" + medicationId
                + ", gtin=" + gtin + ", productName=" + productName + ", productManufacturerOrgId=" + productManufacturerOrgId
                + ", creatorActorId=" + creatorActorId + ", status=" + status + ", tags=" + classificationTags + "]";
    }
}

// src/main/java/de/jklein/fabric/samples/assettransfer/model/Batch.java
package de.jklein.fabric.samples.assettransfer.model;

import com.owlike.genson.annotation.JsonProperty;
import de.jklein.fabric.samples.assettransfer.permission.RoleConstants;
import java.util.Collections;
import java.util.List;
import java.util.Objects; //
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

/**
 * Repräsentiert eine einzelne Charge (Produktionslos) eines Medikaments.
 */
@DataType()
public final class Batch implements IAsset { //

    @Property()
    private final String batchId; //

    @Property()
    private final String medicationKey; // Verweis auf das übergeordnete Medikament

    @Property()
    private final String productionDate; //

    @Property()
    private final String expiryDate; //

    @Property()
    private final int quantity; // Aktuelle Menge in dieser Charge

    @Property()
    private final String currentBatchStatus; // Interner Status der Charge (z.B. ERSTELLT, FREIGEGEBEN)

    @Property()
    private final List<Tag> regulatoryTags; // NEU: Liste von regulatorischen Tags (verwende 'Tag' statt 'AssetTag')

    public Batch(
            @JsonProperty("batchId") final String batchId,
            @JsonProperty("medicationKey") final String medicationKey,
            @JsonProperty("productionDate") final String productionDate,
            @JsonProperty("expiryDate") final String expiryDate,
            @JsonProperty("quantity") final int quantity,
            @JsonProperty("currentBatchStatus") final String currentBatchStatus,
            @JsonProperty("regulatoryTags") final List<Tag> regulatoryTags) {
        this.batchId = batchId;
        this.medicationKey = medicationKey;
        this.productionDate = productionDate;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
        this.currentBatchStatus = currentBatchStatus;
        this.regulatoryTags = regulatoryTags != null ? List.copyOf(regulatoryTags) : Collections.emptyList();
    }

    @Override
    public String getKey() {
        return "BATCH_" + batchId; // Annahme: Präfix für Chargen
    }

    @Override
    public String getStatus() {
        return currentBatchStatus;
    }

    @Override
    public String getRegulatoryStatus() {
        // Der regulatorische Status wird primär durch die regulatoryTags abgebildet.
        // Ein allgemeiner String könnte von den Tags abgeleitet werden oder einfach den internen Status widerspiegeln.
        if (isLockedByRegulator()) {
            return RoleConstants.GESPERRT; //
        }
        return currentBatchStatus; // Oder basierend auf dem internen Status
    }

    @Override
    public boolean isLockedByRegulator() {
        for (Tag tag : regulatoryTags) {
            if (tag.isBlocking()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getAssociatedActorId() {
        // Eine Charge hat keinen direkten "Besitzer" mehr.
        // Der assoziierte Akteur wäre der Ersteller des Medikaments, das sie produziert.
        // Dies müsste über die Medication-Referenz abgerufen werden.
        return null; // Muss dynamisch über Medication abgerufen werden, da es keine direkte Besitzbeziehung gibt
    }

    public String getBatchId() {
        return batchId; //
    }

    public String getMedicationKey() {
        return medicationKey;
    }

    public String getProductionDate() {
        return productionDate; //
    }

    public String getExpiryDate() {
        return expiryDate; //
    }

    public int getQuantity() {
        return quantity; //
    }

    public String getCurrentBatchStatus() {
        return currentBatchStatus;
    }

    public List<Tag> getRegulatoryTags() {
        return regulatoryTags;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        Batch other = (Batch) obj;
        return quantity == other.quantity
                && Objects.deepEquals(
                        new String[]{getBatchId(), getMedicationKey(), getProductionDate(), getExpiryDate(), getCurrentBatchStatus()},
                        new String[]{other.getBatchId(), other.getMedicationKey(), other.getProductionDate(), other.getExpiryDate(), other.getCurrentBatchStatus()}
                )
                && Objects.equals(getRegulatoryTags(), other.getRegulatoryTags());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBatchId(), getMedicationKey(), getProductionDate(), getExpiryDate(), quantity, getCurrentBatchStatus(), getRegulatoryTags());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [batchId=" + batchId
                + ", medicationKey=" + medicationKey + ", productionDate=" + productionDate + ", expiryDate=" + expiryDate
                + ", quantity=" + quantity + ", currentBatchStatus=" + currentBatchStatus + ", regulatoryTags=" + regulatoryTags + "]";
    }
}

// src/main/java/de/jklein/fabric/samples/assettransfer/model/MedicationUnit.java
package de.jklein.fabric.samples.assettransfer.model;

import com.owlike.genson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.Objects; //

/**
 * Repräsentiert eine einzelne Medikamenteneinheit mit eindeutiger ID.
 * Dies ist die kleinste nachverfolgbare Einheit in der Lieferkette.
 */
public final class MedicationUnit implements IAsset { //

    private final String unitId; //
    private final String medicationKey; // Verweis auf das übergeordnete Medikament
    private final String batchKey; // Verweis auf die übergeordnete Charge
    private final String currentOwnerActorId; // Die Akteur-ID des aktuellen Besitzers der Einheit
    private final String unitStatus; // Aktueller Status der Einheit (z.B. FREIGEGEBEN, PENDING_TRANSFER)
    private final List<TransferRecord> transferLog; // Chronologisches Log aller Besitzwechsel

    public MedicationUnit(
            @JsonProperty("unitId") final String unitId,
            @JsonProperty("medicationKey") final String medicationKey,
            @JsonProperty("batchKey") final String batchKey,
            @JsonProperty("currentOwnerActorId") final String currentOwnerActorId,
            @JsonProperty("unitStatus") final String unitStatus,
            @JsonProperty("transferLog") final List<TransferRecord> transferLog) {
        this.unitId = unitId;
        this.medicationKey = medicationKey;
        this.batchKey = batchKey;
        this.currentOwnerActorId = currentOwnerActorId;
        this.unitStatus = unitStatus;
        this.transferLog = transferLog != null ? List.copyOf(transferLog) : Collections.emptyList();
    }

    @Override
    public String getKey() {
        return "UNIT_" + unitId; // Annahme: Präfix für Einheiten
    }

    @Override
    public String getStatus() {
        return unitStatus; //
    }

    @Override
    public String getRegulatoryStatus() {
        // Regulatorischer Status der Einheit wird von der Batch geerbt.
        // Diese Methode müsste den BatchKey verwenden, um den Batch abzurufen
        // und dessen isLockedByRegulator() oder getRegulatoryTags() zu prüfen.
        // Da die IAsset-Schnittstelle dies erfordert, könnte hier ein Platzhalter stehen oder eine Abhängigkeit zum AssetService
        // Wenn man es nicht direkt abrufen will, muss man es beim Erstellen der Unit von der Batch erben oder bei jeder Abfrage dynamisch nachschlagen.
        // Für den Zweck der Modellklasse belassen wir es bei einem Platzhalter, da dies eine Geschäftslogik-Prüfung ist.
        return null; // Muss dynamisch über die Batch abgerufen werden.
    }

    @Override
    public boolean isLockedByRegulator() {
        // Muss die regulatorischen Tags des übergeordneten Batches prüfen.
        // Diese Logik gehört eigentlich in einen Service, der den BatchKey auflöst.
        // Für das Modell-Interface ist dies ein Hinweis auf die erforderliche Funktionalität.
        return false; // Muss dynamisch über die Batch abgerufen werden.
    }

    @Override
    public String getAssociatedActorId() {
        return currentOwnerActorId; // Der aktuelle Besitzer ist der primär assoziierte Akteur
    }

    public String getUnitId() {
        return unitId; //
    }

    public String getMedicationKey() {
        return medicationKey;
    }

    public String getBatchKey() {
        return batchKey;
    }

    public String getCurrentOwnerActorId() {
        return currentOwnerActorId; //
    }

    public String getUnitStatus() {
        return unitStatus;
    }

    public List<TransferRecord> getTransferLog() {
        return transferLog;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        MedicationUnit other = (MedicationUnit) obj;
        return Objects.deepEquals(
                new String[]{getUnitId(), getMedicationKey(), getBatchKey(), getCurrentOwnerActorId(), getUnitStatus()},
                new String[]{other.getUnitId(), other.getMedicationKey(), other.getBatchKey(), other.getCurrentOwnerActorId(), other.getUnitStatus()}
        ) && Objects.equals(getTransferLog(), other.getTransferLog());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUnitId(), getMedicationKey(), getBatchKey(), getCurrentOwnerActorId(), getUnitStatus(), getTransferLog());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [unitId=" + unitId
                + ", medicationKey=" + medicationKey + ", batchKey=" + batchKey + ", currentOwnerActorId=" + currentOwnerActorId
                + ", unitStatus=" + unitStatus + ", transferLogSize=" + transferLog.size() + "]";
    }
}

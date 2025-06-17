// src/main/java/de/jklein/fabric/samples/assettransfer/model/Shipment.java
package de.jklein.fabric.samples.assettransfer.model;

import com.owlike.genson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List; //
import java.util.Objects; //

/**
 * Repräsentiert eine Sendung von Medikamenteneinheiten.
 */
public final class Shipment implements IAsset { //

    private final String shipmentId; //
    private final String shipperActorId; // Akteur-ID des Absenders
    private final String receiverActorId; // Akteur-ID des Empfängers
    private final List<String> medicationUnitKeys; // Liste der Keys der versendeten MedicationUnits
    private final String shipmentInitiationDate; // Zeitstempel der Initiierung der Sendung
    private final String currentShipmentStatus; // Status der Sendung (z.B. "IN_TRANSIT", "DELIVERED")
    private final String expectedDeliveryDate; // Erwartetes Lieferdatum (optional)
    private final String actualDeliveryDate; // Tatsächliches Lieferdatum (optional)

    // Konstruktor mit reduzierter Parameteranzahl, nutzt ShipmentData-Objekt
    public Shipment(@JsonProperty("shipmentData") final ShipmentData data) {
        this.shipmentId = data.getShipmentId();
        this.shipperActorId = data.getShipperActorId();
        this.receiverActorId = data.getReceiverActorId();
        this.medicationUnitKeys = data.getMedicationUnitKeys() != null ? List.copyOf(data.getMedicationUnitKeys()) : Collections.emptyList();
        this.shipmentInitiationDate = data.getShipmentInitiationDate();
        this.currentShipmentStatus = data.getCurrentShipmentStatus();
        this.expectedDeliveryDate = data.getExpectedDeliveryDate();
        this.actualDeliveryDate = data.getActualDeliveryDate();
    }

    // Hilfsklasse für die JSON-Deserialisierung
    public static final class ShipmentData {
        @JsonProperty("shipmentId")
        private String shipmentId;
        @JsonProperty("shipperActorId")
        private String shipperActorId;
        @JsonProperty("receiverActorId")
        private String receiverActorId;
        @JsonProperty("medicationUnitKeys")
        private List<String> medicationUnitKeys;
        @JsonProperty("shipmentInitiationDate")
        private String shipmentInitiationDate;
        @JsonProperty("currentShipmentStatus")
        private String currentShipmentStatus;
        @JsonProperty("expectedDeliveryDate")
        private String expectedDeliveryDate;
        @JsonProperty("actualDeliveryDate")
        private String actualDeliveryDate;

        /**
         * Gibt die ID der Sendung zurück.
         * @return Die ID der Sendung
         */
        public String getShipmentId() {
            return shipmentId;
        }

        /**
         * Setzt die ID der Sendung.
         * @param id Die ID der Sendung
         */
        public void setShipmentId(final String id) {
            this.shipmentId = id;
        }

        /**
         * Gibt die Akteur-ID des Absenders zurück.
         * @return Die Akteur-ID des Absenders
         */
        public String getShipperActorId() {
            return shipperActorId;
        }

        /**
         * Setzt die Akteur-ID des Absenders.
         * @param id Die Akteur-ID des Absenders
         */
        public void setShipperActorId(final String id) {
            this.shipperActorId = id;
        }

        /**
         * Gibt die Akteur-ID des Empfängers zurück.
         * @return Die Akteur-ID des Empfängers
         */
        public String getReceiverActorId() {
            return receiverActorId;
        }

        /**
         * Setzt die Akteur-ID des Empfängers.
         * @param id Die Akteur-ID des Empfängers
         */
        public void setReceiverActorId(final String id) {
            this.receiverActorId = id;
        }

        /**
         * Gibt die Liste der Keys der versendeten MedicationUnits zurück.
         * @return Liste der Keys der versendeten MedicationUnits
         */
        public List<String> getMedicationUnitKeys() {
            return medicationUnitKeys;
        }

        /**
         * Setzt die Liste der Keys der versendeten MedicationUnits.
         * @param keys Liste der Keys der versendeten MedicationUnits
         */
        public void setMedicationUnitKeys(final List<String> keys) {
            this.medicationUnitKeys = keys;
        }

        /**
         * Gibt den Zeitstempel der Initiierung der Sendung zurück.
         * @return Zeitstempel der Initiierung der Sendung
         */
        public String getShipmentInitiationDate() {
            return shipmentInitiationDate;
        }

        /**
         * Setzt den Zeitstempel der Initiierung der Sendung.
         * @param date Zeitstempel der Initiierung der Sendung
         */
        public void setShipmentInitiationDate(final String date) {
            this.shipmentInitiationDate = date;
        }

        /**
         * Gibt den aktuellen Status der Sendung zurück.
         * @return Status der Sendung (z.B. "IN_TRANSIT", "DELIVERED")
         */
        public String getCurrentShipmentStatus() {
            return currentShipmentStatus;
        }

        /**
         * Setzt den aktuellen Status der Sendung.
         * @param status Status der Sendung (z.B. "IN_TRANSIT", "DELIVERED")
         */
        public void setCurrentShipmentStatus(final String status) {
            this.currentShipmentStatus = status;
        }

        /**
         * Gibt das erwartete Lieferdatum zurück.
         * @return Erwartetes Lieferdatum
         */
        public String getExpectedDeliveryDate() {
            return expectedDeliveryDate;
        }

        /**
         * Setzt das erwartete Lieferdatum.
         * @param date Erwartetes Lieferdatum
         */
        public void setExpectedDeliveryDate(final String date) {
            this.expectedDeliveryDate = date;
        }

        /**
         * Gibt das tatsächliche Lieferdatum zurück.
         * @return Tatsächliches Lieferdatum
         */
        public String getActualDeliveryDate() {
            return actualDeliveryDate;
        }

        /**
         * Setzt das tatsächliche Lieferdatum.
         * @param date Tatsächliches Lieferdatum
         */
        public void setActualDeliveryDate(final String date) {
            this.actualDeliveryDate = date;
        }
    }

    /**
     * Hilfsklasse zur Gruppierung von Shipment-Informationen und Reduzierung der Parameteranzahl
     */
    public static final class ShipmentInfo {
        private final String shipmentId;
        private final String shipperActorId;
        private final String receiverActorId;
        private final List<String> medicationUnitKeys;
        private final String expectedDeliveryDate;

        public ShipmentInfo(final String shipmentId, final String shipperActorId, final String receiverActorId,
                           final List<String> medicationUnitKeys, final String expectedDeliveryDate) {
            this.shipmentId = shipmentId;
            this.shipperActorId = shipperActorId;
            this.receiverActorId = receiverActorId;
            this.medicationUnitKeys = medicationUnitKeys;
            this.expectedDeliveryDate = expectedDeliveryDate;
        }

        /**
         * Gibt die ID der Sendung zurück.
         * @return Die ID der Sendung
         */
        public String getShipmentId() {
            return shipmentId;
        }

        /**
         * Gibt die Akteur-ID des Absenders zurück.
         * @return Die Akteur-ID des Absenders
         */
        public String getShipperActorId() {
            return shipperActorId;
        }

        /**
         * Gibt die Akteur-ID des Empfängers zurück.
         * @return Die Akteur-ID des Empfängers
         */
        public String getReceiverActorId() {
            return receiverActorId;
        }

        /**
         * Gibt die Liste der Keys der versendeten MedicationUnits zurück.
         * @return Liste der Keys der versendeten MedicationUnits
         */
        public List<String> getMedicationUnitKeys() {
            return medicationUnitKeys;
        }

        /**
         * Gibt das erwartete Lieferdatum zurück.
         * @return Erwartetes Lieferdatum
         */
        public String getExpectedDeliveryDate() {
            return expectedDeliveryDate;
        }
    }

    /**
     * Factory-Methode für die Erstellung einer neuen Sendung mit reduzierter Parameteranzahl
     * @param info Grundlegende Informationen zur Sendung
     * @param shipmentInitiationDate Initialisierungsdatum
     * @param status Status der Sendung
     * @return Die neue Shipment-Instanz
     */
    public static Shipment create(final ShipmentInfo info, final String shipmentInitiationDate, final String status) {
        ShipmentData data = new ShipmentData();
        data.setShipmentId(info.getShipmentId());
        data.setShipperActorId(info.getShipperActorId());
        data.setReceiverActorId(info.getReceiverActorId());
        data.setMedicationUnitKeys(info.getMedicationUnitKeys());
        data.setShipmentInitiationDate(shipmentInitiationDate);
        data.setCurrentShipmentStatus(status);
        data.setExpectedDeliveryDate(info.getExpectedDeliveryDate());
        data.setActualDeliveryDate(null); // Wird später gesetzt, wenn die Lieferung erfolgt
        return new Shipment(data);
    }

    @Override
    public String getKey() {
        return "SHIP_" + shipmentId; // Annahme: Präfix für Sendungen
    }

    @Override
    public String getStatus() {
        return currentShipmentStatus;
    }

    @Override
    public String getRegulatoryStatus() {
        // Sendungen haben keinen direkten regulatorischen Status in diesem Konzept.
        // Der Status der enthaltenen Einheiten ist relevanter.
        return null;
    }

    @Override
    public boolean isLockedByRegulator() {
        // Eine Sendung ist gesperrt, wenn ihre enthaltenen Einheiten gesperrt sind.
        // Dies müsste dynamisch geprüft werden, indem die Units abgerufen werden.
        return false;
    }

    @Override
    public String getAssociatedActorId() {
        return shipperActorId; // Der Absender ist der primär assoziierte Akteur für die Sendung
    }

    public String getShipmentId() {
        return shipmentId; //
    }

    public String getShipperActorId() {
        return shipperActorId;
    }

    public String getReceiverActorId() {
        return receiverActorId;
    }

    public List<String> getMedicationUnitKeys() {
        return medicationUnitKeys;
    }

    public String getShipmentInitiationDate() {
        return shipmentInitiationDate;
    }

    public String getCurrentShipmentStatus() {
        return currentShipmentStatus;
    }

    public String getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public String getActualDeliveryDate() {
        return actualDeliveryDate;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        Shipment other = (Shipment) obj;
        return Objects.deepEquals(
                new String[]{getShipmentId(), getShipperActorId(), getReceiverActorId(), getShipmentInitiationDate(),
                        getCurrentShipmentStatus(), getExpectedDeliveryDate(), getActualDeliveryDate()},
                new String[]{other.getShipmentId(), other.getShipperActorId(), other.getReceiverActorId(), other.getShipmentInitiationDate(),
                        other.getCurrentShipmentStatus(), other.getExpectedDeliveryDate(), other.getActualDeliveryDate()})
                && Objects.equals(getMedicationUnitKeys(), other.getMedicationUnitKeys());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getShipmentId(), getShipperActorId(), getReceiverActorId(), getMedicationUnitKeys(),
                getShipmentInitiationDate(), getCurrentShipmentStatus(), getExpectedDeliveryDate(), getActualDeliveryDate());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [shipmentId=" + shipmentId
                + ", shipperActorId=" + shipperActorId + ", receiverActorId=" + receiverActorId
                + ", medicationUnitKeys=" + medicationUnitKeys + ", shipmentInitiationDate=" + shipmentInitiationDate
                + ", currentShipmentStatus=" + currentShipmentStatus + ", expectedDeliveryDate=" + expectedDeliveryDate
                + ", actualDeliveryDate=" + actualDeliveryDate + "]";
    }
}

package de.jklein.pharmalink.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Repräsentiert eine einzelne Einheit (Unit) im Domänenmodell des Backends.
 * Dieses Modell ist exakt an die Datenstruktur angepasst, die vom Hyperledger Fabric Chaincode geliefert wird,
 * um eine korrekte Deserialisierung der JSON-Daten zu gewährleisten.
 */
@Data
@NoArgsConstructor
public class Unit {

    // Felder, die direkt aus dem Chaincode-Modell übernommen werden
    private String unitId;
    private String medId;
    private String chargeBezeichnung;
    private String ipfsLink;
    private String currentOwnerActorId; // Umbenannt von currentOwnerId, um dem Chaincode zu entsprechen
    private List<TransferEntry> transferHistory;
    private List<TemperatureReading> temperatureReadings;
    private boolean isConsumed;
    private String consumedRefId;
    private String docType;

    // Dieses Feld ist backend-spezifisch und wird nach dem Abruf von IPFS befüllt.
    private Map<String, Object> ipfsData;

    /**
     * Innere Klasse zur Abbildung der Transfer-Historie aus dem Chaincode.
     */
    @Data
    @NoArgsConstructor
    public static class TransferEntry {
        private String fromActorId;
        private String toActorId;
        private String timestamp;
    }

    /**
     * Innere Klasse zur Abbildung der Temperaturmessungen aus dem Chaincode.
     */
    @Data
    @NoArgsConstructor
    public static class TemperatureReading {
        private String timestamp;
        private String temperature;
    }
}
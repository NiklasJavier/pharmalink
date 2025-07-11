package de.jklein.pharmalink.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class Unit {

    private String unitId;
    private String medId;
    private String chargeBezeichnung;
    private String ipfsLink;
    private String currentOwnerActorId;
    private List<TransferEntry> transferHistory;
    private List<TemperatureReading> temperatureReadings;
    private boolean isConsumed;
    private String consumedRefId;
    private String docType;

    private Map<String, Object> ipfsData;

    @Data
    @NoArgsConstructor
    public static class TransferEntry {
        private String fromActorId;
        private String toActorId;
        private String timestamp;
    }
    
    @Data
    @NoArgsConstructor
    public static class TemperatureReading {
        private String timestamp;
        private String temperature;
    }
}
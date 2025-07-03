package de.jklein.pharmalink.api.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class UnitResponseDto {
    private String unitId;
    private String medId;
    private String chargeBezeichnung;
    private String ipfsLink;
    private String currentOwnerActorId;
    private List<Map<String, String>> temperatureReadings;
    private List<Map<String, String>> transferHistory;

    // NEU: Feld für die aufgelösten IPFS-Daten
    private Map<String, Object> ipfsData;
}
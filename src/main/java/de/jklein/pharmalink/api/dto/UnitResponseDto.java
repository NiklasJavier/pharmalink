package de.jklein.pharmalink.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnitResponseDto {
    private String unitId;
    private String medId;
    private String chargeBezeichnung;
    private String ipfsLink;
    private String currentOwnerActorId;
    private List<Map<String, String>> temperatureReadings;
    private List<Map<String, String>> transferHistory;

    private boolean isConsumed;
    private String consumedRefId;

    private Map<String, Object> ipfsData;
}
package de.jklein.pharmalink.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Unit {
    private String unitId;
    private String medId;
    private String chargeBezeichnung;
    private String ipfsLink;
    private String currentOwnerActorId;
    private List<Map<String, String>> temperatureReadings = new ArrayList<>();
    private List<Map<String, String>> transferHistory = new ArrayList<>();
    private String docType;

    private boolean isConsumed;
    private String consumedRefId;

    private Map<String, Object> ipfsData;
}
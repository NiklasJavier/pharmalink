package de.jklein.pharmalink.domain;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class Unit {

    private String unitId;
    private String medId;
    private String chargeBezeichnung;
    private String ipfsLink;
    private String currentOwnerActorId;
    private List<Map<String, String>> temperatureReadings;
    private List<Map<String, String>> transferHistory;
    private String docType; // Internes Feld für die Datenbankabfrage

}
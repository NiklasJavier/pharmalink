package de.jklein.pharmalink.client.fabric.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnitDto {
    private String unitId;
    private String medId;
    private String chargeBezeichnung;
    private int anzahl;
    private String ipfsLink;
    private String lagerort;
    private String status;
    private String letzteAenderung;
    private List<String> transportHistorie;
}
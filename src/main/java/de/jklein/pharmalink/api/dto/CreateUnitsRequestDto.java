package de.jklein.pharmalink.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Map;

@Data
public class CreateUnitsRequestDto {

    @NotBlank(message = "Die Chargenbezeichnung darf nicht leer sein.")
    private String chargeBezeichnung;

    @Min(value = 1, message = "Es muss mindestens eine Einheit erstellt werden.")
    private int anzahl;

    private Map<String, Object> ipfsData;
}
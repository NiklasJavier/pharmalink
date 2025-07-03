package de.jklein.pharmalink.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Map;

/**
 * DTO für die Anfrage zur Erstellung von Einheiten (Units).
 * Erlaubt die Übergabe eines dynamischen JSON-Objekts für IPFS.
 */
@Data
public class CreateUnitsRequestDto {

    @NotBlank(message = "Die Chargenbezeichnung darf nicht leer sein.")
    private String chargeBezeichnung;

    @Min(value = 1, message = "Es muss mindestens eine Einheit erstellt werden.")
    private int anzahl;

    /**
     * Ein optionales, beliebiges JSON-Objekt, das in IPFS gespeichert
     * und mit diesen Units verknüpft werden soll.
     */
    private Map<String, Object> ipfsData;
}
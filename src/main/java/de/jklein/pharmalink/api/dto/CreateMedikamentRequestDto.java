package de.jklein.pharmalink.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Map;

/**
 * DTO für die Anfrage zum Erstellen eines neuen Medikaments.
 * Erlaubt entweder die Übergabe eines dynamischen JSON-Objekts (ipfsData)
 * oder eines bereits existierenden Hashes (infoblattHash).
 */
@Data
public class CreateMedikamentRequestDto {

    @NotBlank(message = "Die Bezeichnung darf nicht leer sein.")
    private String bezeichnung;

    /**
     * Ein optionaler, bereits existierender IPFS-Hash. Dieses Feld wird nur verwendet,
     * wenn 'ipfsData' nicht zur Verfügung gestellt wird.
     */
    private String infoblattHash;

    /**
     * Ein optionales, beliebiges JSON-Objekt. Wenn dieses Feld gesetzt ist,
     * wird es priorisiert, in IPFS gespeichert und der resultierende Hash verwendet.
     */
    private Map<String, Object> ipfsData;
}
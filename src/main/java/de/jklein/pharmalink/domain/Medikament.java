package de.jklein.pharmalink.domain;

import lombok.Data;
import java.util.Map;

/**
 * Domain-Objekt, das ein Medikament im Kern der Anwendung repräsentiert.
 * Diese Klasse enthält alle internen Felder und die Geschäftslogik, die zu einem Medikament gehört.
 * Sie wird von der Service-Schicht verwendet.
 */
@Data
public class Medikament {

    private String medId;
    private String herstellerId;
    private String bezeichnung;
    private String infoblattHash;
    private String ipfsLink;
    private String status;
    private Map<String, String> tags;
    private String docType; // Internes Feld, wird nicht in der API gezeigt
    private String approvedById;

}
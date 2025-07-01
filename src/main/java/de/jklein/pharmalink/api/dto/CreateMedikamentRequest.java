package de.jklein.pharmalink.api.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO für die Erstellung eines Medikaments über die v1 API.
 * Dank Lombok (@Data) sind Getter, Setter, toString, etc. automatisch generiert.
 */
@Data
public class CreateMedikamentRequest {

    private String medId;
    private String bezeichnung;
    private String herstellerId;
    private String infoblattHash;
    private String ipfsLink;
    private List<String> tags;
}
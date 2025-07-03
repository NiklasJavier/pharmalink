package de.jklein.pharmalink.api.dto;

import lombok.Data;
import java.util.Map;

@Data
public class MedikamentResponseDto {
    private String medId;
    private String herstellerId;
    private String bezeichnung;
    private String ipfsLink; // Der Hash, der aufgelöst wird
    private String status;

    // NEUES FELD: Hier kommt der aufgelöste JSON-Inhalt aus IPFS hinein.
    private Map<String, Object> ipfsData;
}
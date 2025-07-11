package de.jklein.pharmalink.api.dto;

import lombok.Data;
import java.util.Map;

@Data
public class MedikamentResponseDto {
    private String medId;
    private String herstellerId;
    private String bezeichnung;
    private String ipfsLink;
    private String status;

    private Map<String, Object> ipfsData;
}
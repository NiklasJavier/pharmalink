package de.jklein.pharmalink.client.fabric.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedikamentDto {
    private String medId;
    private String bezeichnung;
    private String infoblattHash;
    private String ipfsLink;
    private String herstellerId;
}
package de.jklein.pharmalink.domain;

import lombok.Data;
import java.util.Map;

@Data
public class Medikament {

    private String medId;
    private String herstellerId;
    private String bezeichnung;
    private String infoblattHash;
    private String ipfsLink;
    private String status;
    private Map<String, String> tags;
    private String docType;
    private String approvedById;

}
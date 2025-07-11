package de.jklein.pharmalink.domain;

import com.fasterxml.jackson.annotation.JsonFilter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Medikament {
    private String medId;
    private String herstellerId;
    private String bezeichnung;
    private String infoblattHash;
    private String ipfsLink;
    private String status;
    private String approvedById;
    private Map<String, String> tags;
    private String docType;

    private Map<String, Object> ipfsData;

    public Medikament(String medId, String herstellerId, String bezeichnung, String ipfsLink) {
        this.medId = medId;
        this.herstellerId = herstellerId;
        this.bezeichnung = bezeichnung;
        this.ipfsLink = ipfsLink;
        this.status = "angelegt";
        this.tags = new HashMap<>();
        this.docType = "medikament";
        this.ipfsData = new HashMap<>();
    }
}
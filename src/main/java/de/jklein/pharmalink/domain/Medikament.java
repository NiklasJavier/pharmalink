package de.jklein.pharmalink.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "pharmalink.medikamente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Medikament {
    @Id
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
}
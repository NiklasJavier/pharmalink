package de.jklein.pharmalink.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "pharmalink.actors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Actor {
    @Id
    private String actorId;

    private String bezeichnung;
    private String role;
    private String email;
    private String ipfsLink;
    private String docType;
    private Map<String, Object> ipfsData;
}
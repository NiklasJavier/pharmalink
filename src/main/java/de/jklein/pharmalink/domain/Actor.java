package de.jklein.pharmalink.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

@Document(collection = "actors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Actor {
    @Id
    private String id;
    private String actorId;
    private String bezeichnung;
    private String role;
    private String email;
    private String ipfsLink;
    private String docType;
    private Map<String, Object> ipfsData;

    public Actor(String actorId, String bezeichnung, String role, String email, String ipfsLink) {
        this.actorId = actorId;
        this.bezeichnung = bezeichnung;
        this.role = role;
        this.email = email;
        this.ipfsLink = ipfsLink;
        this.docType = "actor";
        this.ipfsData = new HashMap<>();
    }
}
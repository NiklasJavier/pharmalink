package de.jklein.pharmalink.domain;

import lombok.Data;

@Data
public class Actor {
    private String actorId;
    private String role;
    private String email;
    private String ipfsLink;
    private String docType;
}

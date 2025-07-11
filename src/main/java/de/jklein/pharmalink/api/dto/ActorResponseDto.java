package de.jklein.pharmalink.api.dto;

import lombok.Data;
import java.util.Map; // Import für Map

@Data
public class ActorResponseDto {
    private String actorId;
    private String bezeichnung;
    private String role;
    private String email;
    private String ipfsLink;

    private Map<String, Object> ipfsData;

    public String getRolle() {
        if (this.actorId == null || this.actorId.isBlank()) {
            return "unbekannt";
        }

        if (actorId.startsWith("hersteller-")) {
            return "hersteller";
        } else if (actorId.startsWith("grosshaendler-")) {
            return "grosshaendler";
        } else if (actorId.startsWith("apotheke-")) {
            return "apotheke";
        } else if (actorId.startsWith("behoerde-")) {
            return "behoerde";
        } else {
            return "unbekannt";
        }
    }
}
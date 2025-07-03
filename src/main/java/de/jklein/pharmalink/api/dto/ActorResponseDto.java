package de.jklein.pharmalink.api.dto;

import lombok.Data;
import java.util.Map; // Import für Map

@Data
public class ActorResponseDto {
    private String actorId;
    private String role;
    private String email;
    private String ipfsLink;

    // NEU: Feld für die aufgelösten IPFS-Daten
    private Map<String, Object> ipfsData;
}
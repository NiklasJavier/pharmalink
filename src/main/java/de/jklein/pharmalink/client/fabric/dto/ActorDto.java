package de.jklein.pharmalink.client.fabric.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActorDto {
    private String actorId;
    private String email;
    private String role;
    private String ipfsLink;
}
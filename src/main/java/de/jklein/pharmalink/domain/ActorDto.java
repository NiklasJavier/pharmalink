package de.jklein.pharmalink.domain;

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
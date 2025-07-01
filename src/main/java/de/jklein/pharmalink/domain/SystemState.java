package de.jklein.pharmalink.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Eine Entity, die einen globalen Zustand der Applikation repräsentiert,
 * z.B. die ID des initialisierten Akteurs aus dem Chaincode.
 * Es sollte immer nur einen einzigen Eintrag dieser Art in der DB geben.
 */
@Entity
@Data
@NoArgsConstructor
public class SystemState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String initialActorId;

    public SystemState(String initialActorId) {
        this.initialActorId = initialActorId;
    }
}
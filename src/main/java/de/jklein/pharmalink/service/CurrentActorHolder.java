package de.jklein.pharmalink.service;

import de.jklein.pharmalink.dto.ActorDto;
import org.springframework.stereotype.Component;

/**
 * Hält die Informationen des aktuell initialisierten Akteurs für die Laufzeit.
 * Dies ist eine einfache In-Memory-Lösung für das Beispiel.
 */
@Component
public class CurrentActorHolder {

    private ActorDto currentActor;

    public ActorDto getCurrentActor() {
        return currentActor;
    }

    public void setCurrentActor(ActorDto currentActor) {
        this.currentActor = currentActor;
    }

    public boolean isActorInitialized() {
        return this.currentActor != null;
    }
}
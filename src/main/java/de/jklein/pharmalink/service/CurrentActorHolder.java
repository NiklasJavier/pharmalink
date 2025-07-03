package de.jklein.pharmalink.service;

import de.jklein.pharmalink.domain.Actor;
import org.springframework.stereotype.Component;

/**
 * Hält die Informationen des aktuell initialisierten Akteurs für die Laufzeit.
 * Dies ist eine einfache In-Memory-Lösung für das Beispiel.
 */
@Component
public class CurrentActorHolder {

    private Actor currentActor;

    public Actor getCurrentActor() {
        return currentActor;
    }

    public void setCurrentActor(Actor currentActor) {
        this.currentActor = currentActor;
    }

    public boolean isActorInitialized() {
        return this.currentActor != null;
    }
}
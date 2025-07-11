package de.jklein.pharmalink.runner;

import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.service.state.SystemStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AppDataInitializer implements CommandLineRunner {

    private final FabricClient fabricClient;
    private final SystemStateService systemStateService;

    @Override
    public void run(String... args) {
        log.info("Starte Initialisierung der Anwendungsdaten...");
        try {
            log.info("Führe 'initCall' am Chaincode aus, um den aktuellen Akteur zu identifizieren...");

            String actorJson = fabricClient.submitGenericTransaction("initCall");
            Actor initializedActor = fabricClient.getGson().fromJson(actorJson, Actor.class);
            String actorIdFromChaincode = initializedActor.getActorId();

            systemStateService.reconcileAndCacheActorId(actorIdFromChaincode);

            log.info("Initialisierung der Anwendungsdaten abgeschlossen. Aktueller Akteur: {}", actorIdFromChaincode);

        } catch (Exception e) {
            log.error("Konnte den aktuellen Akteur nicht über 'initCall' vom Chaincode identifizieren. Die Anwendung wird ohne spezifischen Akteur-Kontext fortgesetzt.", e);
        }
    }
}
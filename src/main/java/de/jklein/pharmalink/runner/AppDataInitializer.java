package de.jklein.pharmalink.runner;

import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.service.SystemStateService; // Import des neuen Service
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AppDataInitializer implements CommandLineRunner {

    private final FabricClient fabricClient;
    private final SystemStateService systemStateService; // Injizierung des neuen Service

    @Value("${ipfs.email}")
    private String userEmail;

    @Value("${actor.bezeichnung}")
    private String actorDescription;

    @Value("${ipfs.ipfs-link}")
    private String userIpfsLink;

    @Override
    public void run(String... args) {
        log.info("Starting application data initialization...");
        try {
            log.info("Performing init call to chaincode to get initial actor...");
            String actorJson = fabricClient.submitGenericTransaction("initCall", actorDescription, userEmail, userIpfsLink);
            Actor initializedActor = fabricClient.getGson().fromJson(actorJson, Actor.class);
            String actorIdFromChaincode = initializedActor.getActorId();
            log.info("Chaincode initialization returned Actor ID: {}", actorIdFromChaincode);

            // Delegiere die gesamte Logik an den State Service
            systemStateService.reconcileAndCacheActorId(actorIdFromChaincode);

        } catch (Exception e) {
            log.error("Failed to initialize application data from chaincode.", e);
            // Rufe die Fallback-Methode im Service auf
            systemStateService.loadFromDatabaseOnFailure();
        }
    }
}
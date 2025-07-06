package de.jklein.pharmalink.runner;

import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.domain.Actor; // Import Domain-Objekt
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;

import de.jklein.pharmalink.service.system.SystemStateService;
import de.jklein.pharmalink.service.ActorService;
import de.jklein.pharmalink.service.MedicationService;
import de.jklein.pharmalink.service.UnitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
@RequiredArgsConstructor
public class AppDataInitializer implements CommandLineRunner {

    private final FabricClient fabricClient;
    private final SystemStateService systemStateService;
    private final ActorService actorService;
    private final MedicationService medicationService;
    private final UnitService unitService;


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
            systemStateService.reconcileAndCacheActorId(actorIdFromChaincode);

            log.info("Loading initial application data...");

            // 1. Einheiten, die der aktuelle Akteur hält
            log.info("Loading units for current actor: {}", actorIdFromChaincode);
            List<Unit> myUnits = unitService.getEnrichedUnitsByOwner(actorIdFromChaincode);
            systemStateService.updateMyUnits(myUnits);
            log.info("Loaded {} units for current actor.", myUnits.size());

            // 2. Alle Akteure (Hersteller, Behörde, Großhändler, Apotheken)
            log.info("Loading all actors by role...");
            List<Actor> allHersteller = actorService.getActorsByRole("hersteller"); // Rückgabetyp geändert
            List<Actor> allGrosshaendler = actorService.getActorsByRole("grosshaendler"); // Rückgabetyp geändert
            List<Actor> allApotheken = actorService.getActorsByRole("apotheke"); // Rückgabetyp geändert
            List<Actor> allBehoerden = actorService.getActorsByRole("behoerde"); // Rückgabetyp geändert

            List<Actor> combinedActors = Stream.of(allHersteller, allGrosshaendler, allApotheken, allBehoerden)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            systemStateService.updateAllActors(combinedActors); // NEU: Direkte Übergabe der Domain-Objekte
            log.info("Loaded {} total actors ({} Hersteller, {} Grosshaendler, {} Apotheken, {} Behörden).",
                    combinedActors.size(), allHersteller.size(), allGrosshaendler.size(), allApotheken.size(), allBehoerden.size());


            // 3. Alle Medikamente
            log.info("Loading all medications using direct chaincode query...");
            List<Medikament> allMedikamente = medicationService.getAllMedikamente();
            systemStateService.updateAllMedikamente(allMedikamente);
            log.info("Loaded {} medications in total.", allMedikamente.size());

            log.info("Application data initialization completed and cached in SystemState.");

        } catch (Exception e) {
            log.error("Failed to initialize application data from chaincode.", e);
            systemStateService.loadFromDatabaseOnFailure();
        }
    }
}
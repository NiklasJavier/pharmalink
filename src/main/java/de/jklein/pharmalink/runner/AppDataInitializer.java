package de.jklein.pharmalink.runner;

import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.service.fabric.ActorFabricService;
import de.jklein.pharmalink.service.fabric.MedicationFabricService;
import de.jklein.pharmalink.service.fabric.UnitFabricService;
import de.jklein.pharmalink.service.state.SystemStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
@RequiredArgsConstructor
public class AppDataInitializer implements CommandLineRunner {

    private final FabricClient fabricClient;
    private final SystemStateService systemStateService;
    private final ActorFabricService actorFabricService;
    private final MedicationFabricService medicationFabricService;
    private final UnitFabricService unitFabricService;

    @Override
    public void run(String... args) {
        log.info("Starting application data initialization...");
        try {
            log.info("Performing init call to chaincode to get initial actor...");
            String actorJson = fabricClient.submitGenericTransaction("initCall");
            Actor initializedActor = fabricClient.getGson().fromJson(actorJson, Actor.class);
            String actorIdFromChaincode = initializedActor.getActorId();
            systemStateService.reconcileAndCacheActorId(actorIdFromChaincode);

            log.info("Loading initial application data...");

            log.info("Loading units for current actor: {}", actorIdFromChaincode);
            List<Unit> myUnits = unitFabricService.getUnitsByOwner(actorIdFromChaincode);
            systemStateService.updateMyUnits(myUnits);
            log.info("Loaded {} units for current actor.", myUnits.size());

            log.info("Loading all actors by role...");
            List<Actor> allHersteller = actorFabricService.getActorsByRole("hersteller");
            List<Actor> allGrosshaendler = actorFabricService.getActorsByRole("grosshaendler");
            List<Actor> allApotheken = actorFabricService.getActorsByRole("apotheke");
            List<Actor> allBehoerden = actorFabricService.getActorsByRole("behoerde");

            List<Actor> combinedActors = Stream.of(allHersteller, allGrosshaendler, allApotheken, allBehoerden)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            systemStateService.updateAllActors(combinedActors);
            log.info("Loaded {} total actors ({} Hersteller, {} Grosshaendler, {} Apotheken, {} Behörden).",
                    combinedActors.size(), allHersteller.size(), allGrosshaendler.size(), allApotheken.size(), allBehoerden.size());


            log.info("Loading all medications...");
            List<Medikament> allMedikamente = medicationFabricService.getAllMedikamente();
            systemStateService.updateAllMedikamente(allMedikamente);
            log.info("Loaded {} medications in total.", allMedikamente.size());

            log.info("Application data initialization completed and cached in SystemState.");

        } catch (Exception e) {
            log.error("Failed to initialize application data from chaincode.", e);
            systemStateService.loadFromDatabaseOnFailure();
        }
    }
}
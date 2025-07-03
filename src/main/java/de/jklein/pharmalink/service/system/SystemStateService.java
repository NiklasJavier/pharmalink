package de.jklein.pharmalink.service.system;

import de.jklein.pharmalink.domain.system.SystemState;
import de.jklein.pharmalink.repository.system.SystemStateRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Ein von Spring verwalteter Singleton-Service, der den globalen Zustand der Anwendung hält.
 * Dieser Service ist die einzige Quelle der Wahrheit für die initialisierte Actor-ID.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemStateService {

    private final SystemStateRepository systemStateRepository;

    @Getter
    private String initialActorId;

    /**
     * Gleicht die vom Chaincode erhaltene ID mit der in der Datenbank gespeicherten ab.
     * Diese Methode wird vom AppDataInitializer beim Start aufgerufen.
     *
     * @param actorIdFromChaincode Die "offizielle" ID aus dem Init-Aufruf des Chaincodes.
     */
    public void reconcileAndCacheActorId(String actorIdFromChaincode) {
        Optional<SystemState> existingStateOpt = systemStateRepository.findFirstByOrderByIdAsc();

        if (existingStateOpt.isEmpty()) {
            // Fall 1: Kein Zustand in der DB -> neuen Zustand speichern
            log.info("No system state found in database. Creating new state...");
            SystemState newState = new SystemState(actorIdFromChaincode);
            systemStateRepository.save(newState);
            log.info("Successfully saved initial actor ID '{}' to database.", actorIdFromChaincode);
            this.initialActorId = actorIdFromChaincode;
        } else {
            // Fall 2: Zustand in der DB gefunden -> vergleichen und ggf. updaten
            SystemState existingState = existingStateOpt.get();
            if (!existingState.getInitialActorId().equals(actorIdFromChaincode)) {
                log.warn("Actor ID from chaincode ('{}') differs from stored ID ('{}'). Updating database...",
                        actorIdFromChaincode, existingState.getInitialActorId());
                existingState.setInitialActorId(actorIdFromChaincode);
                systemStateRepository.save(existingState);
                this.initialActorId = actorIdFromChaincode;
            } else {
                log.info("Stored actor ID matches chaincode ID. No update needed.");
                this.initialActorId = existingState.getInitialActorId();
            }
        }
        log.info("SystemStateService is now initialized with Actor ID: {}", this.initialActorId);
    }

    /**
     * Fallback-Methode, falls der Chaincode beim Start nicht erreichbar ist.
     * Versucht, den letzten bekannten Wert aus der DB zu laden.
     */
    public void loadFromDatabaseOnFailure() {
        if (this.initialActorId != null) {
            log.info("Actor ID is already cached. No fallback needed.");
            return;
        }
        systemStateRepository.findFirstByOrderByIdAsc()
                .ifPresentOrElse(
                        state -> {
                            this.initialActorId = state.getInitialActorId();
                            log.warn("Loaded cached Actor ID '{}' from database due to an initialization error.", this.initialActorId);
                        },
                        () -> log.error("FATAL: Chaincode is unreachable and no state is cached in the database. Application might not work correctly.")
                );
    }
}
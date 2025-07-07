package de.jklein.pharmalink.service.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.domain.audit.ChaincodeEventLog; // Angenommener Import
import de.jklein.pharmalink.domain.system.SystemState;
import de.jklein.pharmalink.repository.audit.ChaincodeEventLogRepository; // Angenommener Import
import de.jklein.pharmalink.repository.system.SystemStateRepository;
import de.jklein.pharmalink.service.ActorService;
import de.jklein.pharmalink.service.MedicationService;
import de.jklein.pharmalink.service.UnitService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.hyperledger.fabric.client.ChaincodeEvent;
import org.hyperledger.fabric.client.CloseableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Getter
public class SystemStateService {

    private static final Logger logger = LoggerFactory.getLogger(SystemStateService.class);
    private static final String SYSTEM_STATE_ID = "pharmalink-system-state";
    private static final String GENERIC_EVENT_NAME = "PharmalinkDataEvent";

    private final SystemStateRepository systemStateRepository;
    private final FabricClient fabricClient;
    private final ActorService actorService;
    private final MedicationService medicationService;
    private final UnitService unitService;
    private final ObjectMapper objectMapper;
    private final ChaincodeEventLogRepository eventLogRepository; // NEU: Repository für Audit-Logs

    @Value("${fabric.chaincode-name}")
    private String chaincodeName;

    private final AtomicReference<String> currentActorId = new AtomicReference<>();
    private final List<Actor> allActors = Collections.synchronizedList(new ArrayList<>());
    private final List<Medikament> allMedikamente = Collections.synchronizedList(new ArrayList<>());
    private final List<Unit> myUnits = Collections.synchronizedList(new ArrayList<>());

    private CloseableIterator<ChaincodeEvent> chaincodeEventIterator;

    // ANGEPASSTER KONSTRUKTOR
    public SystemStateService(SystemStateRepository systemStateRepository, FabricClient fabricClient,
                              ActorService actorService, MedicationService medicationService,
                              UnitService unitService, ObjectMapper objectMapper,
                              ChaincodeEventLogRepository eventLogRepository) { // NEU
        this.systemStateRepository = systemStateRepository;
        this.fabricClient = fabricClient;
        this.actorService = actorService;
        this.medicationService = medicationService;
        this.unitService = unitService;
        this.objectMapper = objectMapper;
        this.eventLogRepository = eventLogRepository; // NEU
    }

    @PostConstruct
    public void init() {
        logger.info("Initializing SystemStateService...");
        loadStateFromDatabase();
        startEventListening();
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down SystemStateService. Saving final state to database...");
        saveStateToDatabase();
        if (chaincodeEventIterator != null) {
            try {
                chaincodeEventIterator.close();
                logger.info("Chaincode event listener closed.");
            } catch (Exception e) {
                logger.error("Failed to close chaincode event iterator: {}", e.getMessage(), e);
            }
        }
        fabricClient.shutdownEventExecutor();
    }

    private void startEventListening() {
        try {
            chaincodeEventIterator = fabricClient.startChaincodeEventListening(chaincodeName, this::handleChaincodeEvent);
            logger.info("Successfully started chaincode event listening for chaincode: {}", chaincodeName);
        } catch (Exception e) {
            logger.error("Failed to start chaincode event listening: {}", e.getMessage(), e);
        }
    }

    /**
     * ANGEPASST: Loggt jedes Event zuerst für Auditzwecke und verarbeitet es dann.
     */
    private void handleChaincodeEvent(ChaincodeEvent event) {
        // Schritt 1: Event für Audit-Zwecke loggen
        logEventForAudit(event);

        // Schritt 2: Bestehende Logik zur Cache-Aktualisierung
        if (!GENERIC_EVENT_NAME.equals(event.getEventName())) {
            logger.debug("Received unhandled or legacy chaincode event: {}", event.getEventName());
            return;
        }

        try {
            if (event.getPayload() == null || event.getPayload().length == 0) {
                logger.warn("Received event '{}' with empty payload. Skipping.", event.getEventName());
                return;
            }
            JsonNode payload = objectMapper.readTree(event.getPayload());
            String entityType = payload.path("entityType").asText();
            String entityId = payload.path("entityId").asText();

            if (entityId.isEmpty() || entityType.isEmpty()) {
                logger.error("Event payload is missing 'entityId' or 'entityType'. Payload: {}", payload);
                return;
            }

            switch (entityType) {
                case "Actor":
                    handleActorUpdate(entityId);
                    break;
                case "Medikament":
                    handleMedikamentUpdate(entityId);
                    break;
                case "Unit":
                    handleUnitUpdate(entityId);
                    break;
                default:
                    logger.warn("Received event for unhandled entityType: {}", entityType);
                    break;
            }
        } catch (IOException e) {
            logger.error("Failed to parse event payload for event '{}' due to IOException: {}", event.getEventName(), e.getMessage());
        }
    }

    /**
     * NEUE METHODE: Erstellt und speichert einen Audit-Log-Eintrag.
     */
    private void logEventForAudit(ChaincodeEvent event) {
        try {
            String payloadAsString = new String(event.getPayload(), StandardCharsets.UTF_8);
            ChaincodeEventLog logEntry = new ChaincodeEventLog(
                    event.getEventName(),
                    event.getTransactionId(),
                    event.getBlockNumber(),
                    payloadAsString
            );
            eventLogRepository.save(logEntry);
            logger.info("Chaincode event with TxId '{}' successfully audited.", event.getTransactionId());
        } catch (Exception e) {
            logger.error("!!! FAILED TO AUDIT CHAINCODE EVENT !!! TxId: {}. Reason: {}",
                    event.getTransactionId(), e.getMessage());
        }
    }

    private void handleActorUpdate(String actorId) {
        actorService.getEnrichedActorById(actorId).ifPresent(updatedActor -> {
            synchronized (allActors) {
                allActors.removeIf(a -> a.getActorId().equals(actorId));
                allActors.add(updatedActor);
            }
            logger.info("Actor {} cache updated due to chaincode event.", actorId);
        });
    }

    private void handleMedikamentUpdate(String medId) {
        medicationService.getEnrichedMedikamentById(medId).ifPresent(updatedMedikament -> {
            synchronized (allMedikamente) {
                allMedikamente.removeIf(m -> m.getMedId().equals(medId));
                allMedikamente.add(updatedMedikament);
            }
            logger.info("Medikament {} cache updated due to chaincode event.", medId);
        });
    }

    private void handleUnitUpdate(String unitId) {
        unitService.getEnrichedUnitById(unitId).ifPresent(updatedUnit -> {
            synchronized (myUnits) {
                myUnits.removeIf(u -> u.getUnitId().equals(unitId));
                if (Objects.equals(updatedUnit.getOwnerId(), currentActorId.get())) {
                    myUnits.add(updatedUnit);
                    logger.info("Unit {} (owned by current actor) updated in cache.", unitId);
                } else {
                    logger.info("Unit {} is not owned by current actor, removed from 'myUnits' cache.", unitId);
                }
            }
        });
    }

    public void reconcileAndCacheActorId(String actorId) {
        if (!Objects.equals(currentActorId.get(), actorId)) {
            logger.info("Current actor ID changed from {} to {}. Updating SystemState.", currentActorId.get(), actorId);
            currentActorId.set(actorId);
            saveStateToDatabase();
        }
    }

    public void updateAllActors(List<Actor> actors) {
        synchronized (allActors) {
            allActors.clear();
            allActors.addAll(actors);
        }
        logger.info("Full actor cache refresh with {} entries.", actors.size());
    }

    public void updateAllMedikamente(List<Medikament> medikamente) {
        synchronized (allMedikamente) {
            allMedikamente.clear();
            allMedikamente.addAll(medikamente);
        }
        logger.info("Full medication cache refresh with {} entries.", medikamente.size());
    }

    public void updateMyUnits(List<Unit> units) {
        synchronized (myUnits) {
            myUnits.clear();
            myUnits.addAll(units);
        }
        logger.info("My units cache was refreshed with {} entries.", units.size());
    }

    private void saveStateToDatabase() {
        SystemState state = new SystemState();
        state.setId(SYSTEM_STATE_ID);
        state.setCurrentActorId(currentActorId.get());
        state.setAllActors(new ArrayList<>(allActors));
        state.setAllMedikamente(new ArrayList<>(allMedikamente));
        state.setMyUnits(new ArrayList<>(myUnits));

        try {
            systemStateRepository.save(state);
            logger.debug("SystemState successfully saved to database.");
        } catch (Exception e) {
            logger.error("Failed to save SystemState to database: {}", e.getMessage(), e);
        }
    }

    private void loadStateFromDatabase() {
        systemStateRepository.findById(SYSTEM_STATE_ID).ifPresent(state -> {
            currentActorId.set(state.getCurrentActorId());
            Optional.ofNullable(state.getAllActors()).ifPresent(allActors::addAll);
            Optional.ofNullable(state.getAllMedikamente()).ifPresent(allMedikamente::addAll);
            Optional.ofNullable(state.getMyUnits()).ifPresent(myUnits::addAll);
            logger.info("SystemState loaded from database. Current Actor ID: {}", currentActorId.get());
        });
    }

    public void loadFromDatabaseOnFailure() {
        logger.warn("Initial data load from chaincode failed. Attempting to load SystemState from database as a fallback.");
        loadStateFromDatabase();
    }

    public List<Actor> getAllActors() {
        return Collections.unmodifiableList(allActors);
    }

    public List<Medikament> getAllMedikamente() {
        return Collections.unmodifiableList(allMedikamente);
    }

    public List<Unit> getMyUnits() {
        return Collections.unmodifiableList(myUnits);
    }
}
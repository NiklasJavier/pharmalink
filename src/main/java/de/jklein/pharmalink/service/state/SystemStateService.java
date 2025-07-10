package de.jklein.pharmalink.service.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.domain.audit.ChaincodeEventLog;
import de.jklein.pharmalink.domain.system.SystemState;
import de.jklein.pharmalink.repository.audit.ChaincodeEventLogRepository;
import de.jklein.pharmalink.repository.system.SystemStateRepository;
import de.jklein.pharmalink.service.fabric.ActorFabricService;
import de.jklein.pharmalink.service.fabric.MedicationFabricService;
import de.jklein.pharmalink.service.fabric.UnitFabricService;
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
    private static final String SYSTEM_STATE_ID = "pharmalink-state-state";

    // --- Event Name Constants ---
    private static final String GENERIC_EVENT_NAME = "PharmalinkDataEvent";
    private static final String LEGACY_ACTOR_INIT_EVENT = "ActorInitialized";
    private static final String LEGACY_ACTOR_UPDATE_EVENT = "ActorUpdated";
    private static final String LEGACY_MED_UPDATE_EVENT = "MedikamentUpdated"; // ANGEPASST: Von "MedicationUpdated" zu "MedikamentUpdated"
    private static final String MEDIKAMENT_CREATED_EVENT = "MedikamentCreated"; // NEU hinzugefügt

    private final SystemStateRepository systemStateRepository;
    private final FabricClient fabricClient;
    private final ActorFabricService actorFabricService;
    private final MedicationFabricService medicationFabricService;
    private final UnitFabricService unitFabricService;
    private final ObjectMapper objectMapper;
    private final ChaincodeEventLogRepository eventLogRepository;

    @Value("${fabric.chaincode-name}")
    private String chaincodeName;

    private final AtomicReference<String> currentActorId = new AtomicReference<>();
    private final List<Actor> allActors = Collections.synchronizedList(new ArrayList<>());
    private final List<Medikament> allMedikamente = Collections.synchronizedList(new ArrayList<>());
    private final List<Unit> myUnits = Collections.synchronizedList(new ArrayList<>());

    private CloseableIterator<ChaincodeEvent> chaincodeEventIterator;

    public SystemStateService(SystemStateRepository systemStateRepository, FabricClient fabricClient,
                              ActorFabricService actorFabricService, MedicationFabricService medicationFabricService,
                              UnitFabricService unitFabricService, ObjectMapper objectMapper,
                              ChaincodeEventLogRepository eventLogRepository) {
        this.systemStateRepository = systemStateRepository;
        this.fabricClient = fabricClient;
        this.actorFabricService = actorFabricService;
        this.medicationFabricService = medicationFabricService;
        this.unitFabricService = unitFabricService;
        this.objectMapper = objectMapper;
        this.eventLogRepository = eventLogRepository;
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
     * ## HYBRID EVENT HANDLER ##
     * Handles both new generic events and legacy specific events.
     */
    private void handleChaincodeEvent(ChaincodeEvent event) {
        logEventForAudit(event);

        try {
            if (event.getPayload() == null || event.getPayload().length == 0) {
                logger.warn("Received event '{}' with empty payload. Skipping.", event.getEventName());
                return;
            }

            JsonNode payload = objectMapper.readTree(event.getPayload());

            // Route based on the event name
            switch (event.getEventName()) {
                // --- New Generic Event Handling ---
                case GENERIC_EVENT_NAME:
                    processGenericEvent(payload);
                    break;

                // --- Legacy Event Handling ---
                case LEGACY_ACTOR_INIT_EVENT:
                case LEGACY_ACTOR_UPDATE_EVENT:
                    String actorId = payload.path("actorId").asText();
                    if (!actorId.isEmpty()) {
                        logger.debug("Handling legacy actor event for ID: {}", actorId);
                        handleActorUpdate(actorId);
                    }
                    break;

                case LEGACY_MED_UPDATE_EVENT: // Hier wird jetzt "MedikamentUpdated" abgefangen
                case MEDIKAMENT_CREATED_EVENT:
                    // Assuming the payload contains a field like "medId" or "id"
                    String medId = payload.has("medId") ? payload.path("medId").asText() : payload.path("id").asText();
                    if (!medId.isEmpty()) {
                        logger.debug("Handling medication event for ID: {}", medId);
                        handleMedikamentUpdate(medId);
                    }
                    break;

                default:
                    logger.warn("Received unhandled event name: {}", event.getEventName());
                    break;
            }
        } catch (IOException e) {
            logger.error("Failed to parse event payload for event '{}' due to IOException: {}", event.getEventName(), e.getMessage(), e);
        }
    }

    /**
     * Processes the new, generic "PharmalinkDataEvent".
     */
    private void processGenericEvent(JsonNode payload) {
        String entityType = payload.path("entityType").asText();
        String entityId = payload.path("entityId").asText();
        String operation = payload.path("operation").asText("UPDATED").toUpperCase();

        if (entityId.isEmpty() || entityType.isEmpty()) {
            logger.error("Generic event payload is missing 'entityId' or 'entityType'. Payload: {}", payload);
            return;
        }

        if ("DELETED".equals(operation)) {
            handleDeletion(entityType, entityId);
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
                logger.warn("Received generic event for unhandled entityType: {}", entityType);
                break;
        }
    }


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
        } catch (Exception e) {
            logger.error("!!! FAILED TO AUDIT CHAINCODE EVENT !!! TxId: {}. Reason: {}",
                    event.getTransactionId(), e.getMessage(), e);
        }
    }

    private void handleDeletion(String entityType, String entityId) {
        switch (entityType) {
            case "Actor":
                synchronized (allActors) {
                    if (allActors.removeIf(a -> a.getActorId().equals(entityId))) {
                        logger.info("Actor {} removed from cache due to DELETED event.", entityId);
                    }
                }
                break;
            case "Medikament":
                synchronized (allMedikamente) {
                    if (allMedikamente.removeIf(m -> m.getMedId().equals(entityId))) {
                        logger.info("Medikament {} removed from cache due to DELETED event.", entityId);
                    }
                }
                break;
            case "Unit":
                synchronized (myUnits) {
                    if (myUnits.removeIf(u -> u.getUnitId().equals(entityId))) {
                        logger.info("Unit {} removed from 'myUnits' cache due to DELETED event.", entityId);
                    }
                }
                break;
            default:
                logger.warn("Received DELETED event for unhandled entityType: {}", entityType);
                break;
        }
    }

    private void handleActorUpdate(String actorId) {
        actorFabricService.getEnrichedActorById(actorId).ifPresent(updatedActor -> {
            synchronized (allActors) {
                allActors.removeIf(a -> a.getActorId().equals(actorId));
                allActors.add(updatedActor);
                logger.info("Actor {} CREATED/UPDATED in cache.", actorId);
            }
        });
    }

    private void handleMedikamentUpdate(String medId) {
        medicationFabricService.getEnrichedMedikamentById(medId).ifPresent(updatedMedikament -> {
            synchronized (allMedikamente) {
                allMedikamente.removeIf(m -> m.getMedId().equals(medId));
                allMedikamente.add(updatedMedikament);
                logger.info("Medikament {} CREATED/UPDATED in cache.", medId);
            }
        });
    }

    private void handleUnitUpdate(String unitId) {
        Optional<Unit> updatedUnitOptional = unitFabricService.getEnrichedUnitById(unitId);

        if (updatedUnitOptional.isPresent()) {
            Unit updatedUnit = updatedUnitOptional.get();
            synchronized (myUnits) {
                myUnits.removeIf(u -> u.getUnitId().equals(unitId));
                if (Objects.equals(updatedUnit.getOwnerId(), currentActorId.get())) {
                    myUnits.add(updatedUnit);
                    logger.info("Unit {} now owned by current actor. Added/Updated in 'myUnits' cache.", unitId);
                } else {
                    logger.info("Unit {} is no longer owned by current actor. Removed from 'myUnits' cache.", unitId);
                }
            }
        } else {
            handleDeletion("Unit", unitId);
        }
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

    public void saveStateToDatabase() {
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

    public List<Actor> getAllActors() {
        return Collections.unmodifiableList(allActors);
    }

    public List<Medikament> getAllMedikamente() {
        return Collections.unmodifiableList(allMedikamente);
    }

    public List<Unit> getMyUnits() {
        return Collections.unmodifiableList(myUnits);
    }

    public String getAllActorsAsJsonString() throws JsonProcessingException {
        return objectMapper.writeValueAsString(this.allActors);
    }

    public String getAllMedikamenteAsJsonString() throws JsonProcessingException {
        return objectMapper.writeValueAsString(this.allMedikamente);
    }

    public String getMyUnitsAsJsonString() throws JsonProcessingException {
        return objectMapper.writeValueAsString(this.myUnits);
    }

    public void loadFromDatabaseOnFailure() {
        logger.warn("Initial data load from chaincode failed. Attempting to load SystemState from database as a fallback.");
        loadStateFromDatabase();
    }
}
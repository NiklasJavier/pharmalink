package de.jklein.pharmalink.service.system;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.domain.system.SystemState;
import de.jklein.pharmalink.repository.system.SystemStateRepository;
import de.jklein.pharmalink.service.ActorService;
import de.jklein.pharmalink.service.MedicationService;
import de.jklein.pharmalink.service.UnitService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hyperledger.fabric.client.ChaincodeEvent;
import org.hyperledger.fabric.client.CloseableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException; // Import hinzugefügt
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Getter
public class SystemStateService {

    private static final Logger logger = LoggerFactory.getLogger(SystemStateService.class);
    private static final String SYSTEM_STATE_ID = "pharmalink-system-state";

    private final SystemStateRepository systemStateRepository;
    private final FabricClient fabricClient;
    private final ActorService actorService;
    private final MedicationService medicationService;
    private final UnitService unitService;
    private final ObjectMapper objectMapper;

    @Value("${fabric.chaincode-name}")
    private String chaincodeName;

    private final AtomicReference<String> currentActorId = new AtomicReference<>();
    private final List<Actor> allActors = Collections.synchronizedList(new ArrayList<>());
    private final List<Medikament> allMedikamente = Collections.synchronizedList(new ArrayList<>());
    private final List<Unit> myUnits = Collections.synchronizedList(new ArrayList<>());

    private CloseableIterator<ChaincodeEvent> chaincodeEventIterator;


    @PostConstruct
    public void init() {
        logger.info("Initializing SystemStateService...");
        systemStateRepository.findById(SYSTEM_STATE_ID).ifPresent(state -> {
            currentActorId.set(state.getCurrentActorId());
            Optional.ofNullable(state.getAllActors()).ifPresent(allActors::addAll);
            Optional.ofNullable(state.getAllMedikamente()).ifPresent(allMedikamente::addAll);
            Optional.ofNullable(state.getMyUnits()).ifPresent(myUnits::addAll);
            logger.info("SystemState loaded from database. Current Actor ID: {}", currentActorId.get());
        });

        startEventListening();
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down SystemStateService. Saving state to database...");
        saveSystemState();
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

    private void handleChaincodeEvent(ChaincodeEvent event) {
        logger.debug("Processing Chaincode Event: EventName={}, TxId={}, Payload={}",
                event.getEventName(), event.getTransactionId(), new String(event.getPayload(), StandardCharsets.UTF_8));

        String eventName = event.getEventName();
        JsonNode payloadNode;
        try {
            // NEU: Überprüfen, ob die Payload leer ist
            if (event.getPayload() == null || event.getPayload().length == 0) {
                logger.warn("Received event '{}' with empty payload. Skipping payload parsing.", eventName);
                return;
            }
            payloadNode = objectMapper.readTree(event.getPayload());
        } catch (IOException e) { // NEU: Fange IOException, die JsonProcessingException ist eine Unterklasse davon
            logger.error("Failed to parse event payload for event '{}' due to IOException: {}", eventName, e.getMessage());
            return;
        }

        switch (eventName) {
            case "ActorUpdated":
            case "ActorCreated":
                handleActorEvent(payloadNode);
                break;
            case "MedikamentUpdated":
            case "MedikamentCreated":
                handleMedikamentEvent(payloadNode);
                break;
            case "UnitUpdated":
            case "UnitCreated":
            case "UnitTransferred":
                handleUnitEvent(payloadNode);
                break;
            default:
                logger.warn("Received unhandled chaincode event: {}", eventName);
                break;
        }
        saveSystemState();
    }

    private void handleActorEvent(JsonNode payloadNode) {
        try {
            String actorId = payloadNode.has("actorId") ? payloadNode.get("actorId").asText() : null;
            if (actorId == null) {
                logger.warn("Actor event payload missing 'actorId'. Skipping update.");
                return;
            }

            Optional<Actor> updatedActorOptional = actorService.getEnrichedActorById(actorId);
            updatedActorOptional.ifPresent(updatedActor -> {
                synchronized (allActors) {
                    allActors.removeIf(a -> a.getActorId().equals(updatedActor.getActorId()));
                    allActors.add(updatedActor);
                }
                logger.info("Actor {} updated in cache due to chaincode event.", actorId);
            });
            if (updatedActorOptional.isEmpty()) {
                logger.warn("Could not re-fetch actor {} after chaincode event. Cache might be stale.", actorId);
            }
        } catch (Exception e) {
            logger.error("Error processing Actor event: {}", e.getMessage(), e);
        }
    }

    private void handleMedikamentEvent(JsonNode payloadNode) {
        try {
            String medId = payloadNode.has("medId") ? payloadNode.get("medId").asText() : null;
            if (medId == null) {
                logger.warn("Medikament event payload missing 'medId'. Skipping update.");
                return;
            }

            Optional<Medikament> updatedMedikamentOptional = medicationService.getEnrichedMedikamentById(medId);

            updatedMedikamentOptional.ifPresent(updatedMedikament -> {
                synchronized (allMedikamente) {
                    allMedikamente.removeIf(m -> m.getMedId().equals(updatedMedikament.getMedId()));
                    allMedikamente.add(updatedMedikament);
                }
                logger.info("Medikament {} updated in cache due to chaincode event.", medId);
            });
            if (updatedMedikamentOptional.isEmpty()) {
                logger.warn("Could not re-fetch Medikament {} after chaincode event. Cache might be stale.", medId);
            }
        } catch (Exception e) {
            logger.error("Error processing Medikament event: {}", e.getMessage(), e);
        }
    }

    private void handleUnitEvent(JsonNode payloadNode) {
        try {
            String unitId = payloadNode.has("unitId") ? payloadNode.get("unitId").asText() : null;
            if (unitId == null) {
                logger.warn("Unit event payload missing 'unitId'. Skipping update.");
                return;
            }

            Optional<Unit> updatedUnitOptional = unitService.getEnrichedUnitById(unitId);

            updatedUnitOptional.ifPresent(updatedUnit -> {
                synchronized (myUnits) {
                    myUnits.removeIf(u -> u.getUnitId().equals(updatedUnit.getUnitId()));
                    if (updatedUnit.getOwnerId() != null && updatedUnit.getOwnerId().equals(currentActorId.get())) {
                        myUnits.add(updatedUnit);
                        logger.info("Unit {} (owned by current actor) updated in cache due to chaincode event.", unitId);
                    } else {
                        logger.info("Unit {} not owned by current actor, removed from myUnits cache.", unitId);
                    }
                }
            });
            if (updatedUnitOptional.isEmpty()) {
                logger.warn("Could not re-fetch Unit {} after chaincode event. Cache might be stale.", unitId);
            }
        } catch (Exception e) {
            logger.error("Error processing Unit event: {}", e.getMessage(), e);
        }
    }

    public void reconcileAndCacheActorId(String actorId) {
        if (!Objects.equals(currentActorId.get(), actorId)) {
            logger.info("Current actor ID changed from {} to {}. Updating SystemState.", currentActorId.get(), actorId);
            currentActorId.set(actorId);
            saveSystemState();
        }
    }

    public void updateAllActors(List<Actor> actors) {
        synchronized (allActors) {
            allActors.clear();
            allActors.addAll(actors);
        }
        saveSystemState();
        logger.info("All actors cache updated with {} entries.", actors.size());
    }

    public void updateAllMedikamente(List<Medikament> medikamente) {
        synchronized (allMedikamente) {
            allMedikamente.clear();
            allMedikamente.addAll(medikamente);
        }
        saveSystemState();
        logger.info("All medications cache updated with {} entries.", medikamente.size());
    }

    public void updateMyUnits(List<Unit> units) {
        synchronized (myUnits) {
            myUnits.clear();
            myUnits.addAll(units);
        }
        saveSystemState();
        logger.info("My units cache updated with {} entries.", units.size());
    }

    private void saveSystemState() {
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

    public void loadFromDatabaseOnFailure() {
        logger.warn("Initial data load from chaincode failed. Attempting to load SystemState from database.");
        systemStateRepository.findById(SYSTEM_STATE_ID).ifPresentOrElse(state -> {
            currentActorId.set(state.getCurrentActorId());
            Optional.ofNullable(state.getAllActors()).ifPresent(allActors::addAll);
            Optional.ofNullable(state.getAllMedikamente()).ifPresent(allMedikamente::addAll);
            Optional.ofNullable(state.getMyUnits()).ifPresent(myUnits::addAll);
            logger.info("SystemState successfully loaded from database after initial chaincode failure. Current Actor ID: {}", currentActorId.get());
        }, () -> logger.error("No SystemState found in database after initial chaincode failure. Application state will be empty."));
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
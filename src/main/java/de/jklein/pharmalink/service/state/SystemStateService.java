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
import java.util.concurrent.atomic.AtomicReference;

@Service
@Getter
public class SystemStateService {

    private static final Logger logger = LoggerFactory.getLogger(SystemStateService.class);
    private static final String SYSTEM_STATE_ID = "pharmalink-state-state";

    private static final String ACTOR_INITIALIZED_EVENT = "ActorInitialized";
    private static final String ACTOR_CREATED_EVENT = "ActorCreated";
    private static final String ACTOR_UPDATED_EVENT = "ActorUpdated";
    private static final String ACTOR_IPFS_LINK_UPDATED_EVENT = "ActorIpfsLinkUpdated";
    private static final String ACTOR_DELETED_EVENT = "ActorDeleted";
    private static final String MEDIKAMENT_CREATED_EVENT = "MedikamentCreated";
    private static final String MEDIKAMENT_STATUS_UPDATED_EVENT = "MedikamentStatusUpdated";
    private static final String MEDIKAMENT_UPDATED_EVENT = "MedikamentUpdated";
    private static final String MEDIKAMENT_TAG_ADDED_EVENT = "MedikamentTagAdded";
    private static final String MEDIKAMENT_DELETED_EVENT = "MedikamentDeleted";
    private static final String UNIT_CREATED_EVENT = "UnitCreated";
    private static final String UNIT_TEMPERATURE_ADDED_EVENT = "UnitTemperatureAdded";
    private static final String UNIT_TRANSFERRED_EVENT = "UnitTransferred";
    private static final String UNIT_DELETED_EVENT = "UnitDeleted";

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
        logger.info("Initialisiere System-Status-Dienst...");
        loadStateFromChaincode();
        startEventListening();
    }

    private void loadStateFromChaincode() {
        logger.info("Versuche, den Anfangszustand vom Hyperledger Fabric Chaincode zu laden...");
        try {
            List<Actor> actors = actorFabricService.getAllActors();
            updateAllActors(actors);
            logger.info("{} Akteure erfolgreich vom Chaincode geladen.", actors.size());

            List<Medikament> medikamente = medicationFabricService.getAllMedikamente();
            updateAllMedikamente(medikamente);
            logger.info("{} Medikamente erfolgreich vom Chaincode geladen.", medikamente.size());

            updateMyUnits(new ArrayList<>());
            logger.info("Initialer Akteur noch nicht bekannt. Der Zwischenspeicher für 'meine Einheiten' ist leer.");

        } catch (Exception e) {
            logger.error("KRITISCH: Initialer Zustand konnte nicht vom Chaincode geladen werden. Der lokale Zwischenspeicher ist möglicherweise leer oder unvollständig. Grund: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Fahre System-Status-Dienst herunter. Speichere finalen Zustand in der Datenbank...");
        saveStateToDatabase();
        if (chaincodeEventIterator != null) {
            chaincodeEventIterator.close();
        }
        fabricClient.shutdownEventExecutor();
    }

    private void startEventListening() {
        try {
            chaincodeEventIterator = fabricClient.startChaincodeEventListening(chaincodeName, this::handleChaincodeEvent);
            logger.info("Chaincode-Ereignisüberwachung für Chaincode '{}' erfolgreich gestartet.", chaincodeName);
        } catch (Exception e) {
            logger.error("Fehler beim Starten der Chaincode-Ereignisüberwachung: {}", e.getMessage(), e);
        }
    }

    private void handleChaincodeEvent(ChaincodeEvent event) {
        logEventForAudit(event);
        try {
            if (event.getPayload() == null || event.getPayload().length == 0) {
                logger.warn("Ereignis '{}' mit leerem Inhalt empfangen. Wird übersprungen.", event.getEventName());
                return;
            }
            JsonNode payload = objectMapper.readTree(event.getPayload());
            String id = extractIdFromPayload(payload);
            if (id.isEmpty()) {
                logger.warn("Ereignis '{}' mit unbekannter Struktur oder fehlender ID empfangen. Inhalt: {}", event.getEventName(), payload.toString());
                return;
            }
            switch (event.getEventName()) {
                case ACTOR_INITIALIZED_EVENT, ACTOR_CREATED_EVENT, ACTOR_UPDATED_EVENT, ACTOR_IPFS_LINK_UPDATED_EVENT -> handleActorUpdate(id);
                case ACTOR_DELETED_EVENT -> handleDeletion("Actor", id);
                case MEDIKAMENT_CREATED_EVENT, MEDIKAMENT_STATUS_UPDATED_EVENT, MEDIKAMENT_UPDATED_EVENT, MEDIKAMENT_TAG_ADDED_EVENT -> handleMedikamentUpdate(id);
                case MEDIKAMENT_DELETED_EVENT -> handleDeletion("Medikament", id);
                case UNIT_CREATED_EVENT, UNIT_TEMPERATURE_ADDED_EVENT, UNIT_TRANSFERRED_EVENT -> handleUnitUpdate(id);
                case UNIT_DELETED_EVENT -> handleDeletion("Unit", id);
                default -> logger.warn("Unbehandeltes Ereignis empfangen: {}. Inhalt: {}", event.getEventName(), payload.toString());
            }
        } catch (IOException e) {
            logger.error("Fehler beim Verarbeiten des Ereignisinhalts für Ereignis '{}': {}", event.getEventName(), e.getMessage(), e);
        }
    }

    private String extractIdFromPayload(JsonNode payload) {
        if (payload.has("actorId")) return payload.path("actorId").asText();
        if (payload.has("medId")) return payload.path("medId").asText();
        if (payload.has("unitId")) return payload.path("unitId").asText();
        return "";
    }

    private void logEventForAudit(ChaincodeEvent event) {
        try {
            String payloadAsString = new String(event.getPayload(), StandardCharsets.UTF_8);
            ChaincodeEventLog logEntry = new ChaincodeEventLog(event.getEventName(), event.getTransactionId(), event.getBlockNumber(), payloadAsString);
            eventLogRepository.save(logEntry);
        } catch (Exception e) {
            logger.error("!!! FEHLER BEI DER PRÜFUNG DES CHAINCODE-EREIGNISSES !!! Tx-ID: {}. Grund: {}", event.getTransactionId(), e.getMessage(), e);
        }
    }

    private void handleDeletion(String entityType, String entityId) {
        switch (entityType) {
            case "Actor" -> allActors.removeIf(a -> a.getActorId().equals(entityId));
            case "Medikament" -> allMedikamente.removeIf(m -> m.getMedId().equals(entityId));
            case "Unit" -> myUnits.removeIf(u -> u.getUnitId().equals(entityId));
            default -> logger.warn("LÖSCH-Ereignis für unbekannten Objekttyp '{}' empfangen.", entityType);
        }
        logger.info("{} {} aufgrund eines LÖSCH-Ereignisses aus dem Zwischenspeicher entfernt.", entityType, entityId);
    }

    private void handleActorUpdate(String actorId) {
        actorFabricService.getEnrichedActorById(actorId).ifPresent(updatedActor -> {
            allActors.removeIf(a -> a.getActorId().equals(actorId));
            allActors.add(updatedActor);
            logger.info("Akteur {} im Zwischenspeicher erstellt/aktualisiert.", actorId);
        });
    }

    private void handleMedikamentUpdate(String medId) {
        medicationFabricService.getEnrichedMedikamentById(medId).ifPresent(updatedMedikament -> {
            allMedikamente.removeIf(m -> m.getMedId().equals(medId));
            allMedikamente.add(updatedMedikament);
            logger.info("Medikament {} im Zwischenspeicher erstellt/aktualisiert.", medId);
        });
    }

    private void handleUnitUpdate(String unitId) {
        unitFabricService.getEnrichedUnitById(unitId).ifPresent(updatedUnit -> {
            if (Objects.equals(updatedUnit.getCurrentOwnerActorId(), currentActorId.get())) {
                myUnits.removeIf(u -> u.getUnitId().equals(unitId));
                myUnits.add(updatedUnit);
                logger.info("Einheit {} gehört nun dem aktuellen Akteur. Im Zwischenspeicher für 'meine Einheiten' hinzugefügt/aktualisiert.", unitId);
            } else {
                boolean removed = myUnits.removeIf(u -> u.getUnitId().equals(unitId));
                if (removed) {
                    logger.info("Einheit {} gehört nicht mehr dem aktuellen Akteur. Aus dem Zwischenspeicher für 'meine Einheiten' entfernt.", unitId);
                }
            }
        });
    }

    public void reconcileAndCacheActorId(String actorId) {
        if (!Objects.equals(currentActorId.get(), actorId)) {
            logger.info("Aktuelle Akteur-ID von {} auf {} geändert. Aktualisiere System-Status und hole zugehörige Einheiten.", currentActorId.get(), actorId);
            currentActorId.set(actorId);

            if (actorId != null) {
                try {
                    List<Unit> units = unitFabricService.getUnitsByOwner(actorId);
                    updateMyUnits(units);
                    logger.info("{} Einheiten für neue Akteur-ID {} erfolgreich geladen.", units.size(), actorId);
                } catch (Exception e) {
                    logger.error("Fehler beim Abrufen der Einheiten für Akteur {}: {}", actorId, e.getMessage(), e);
                    updateMyUnits(new ArrayList<>());
                }
            } else {
                updateMyUnits(new ArrayList<>());
                logger.info("Aktuelle Akteur-ID auf null gesetzt. Zwischenspeicher für 'meine Einheiten' geleert.");
            }
            saveStateToDatabase();
        }
    }

    public void updateAllActors(List<Actor> actors) {
        synchronized (allActors) {
            allActors.clear();
            allActors.addAll(actors);
        }
    }

    public void updateAllMedikamente(List<Medikament> medikamente) {
        synchronized (allMedikamente) {
            allMedikamente.clear();
            allMedikamente.addAll(medikamente);
        }
    }

    public void updateMyUnits(List<Unit> units) {
        synchronized (myUnits) {
            myUnits.clear();
            myUnits.addAll(units);
        }
    }

    public void saveStateToDatabase() {
        SystemState state = new SystemState();
        state.setId(SYSTEM_STATE_ID);
        state.setCurrentActorId(currentActorId.get());
        state.setAllActors(new ArrayList<>(allActors));
        state.setAllMedikamente(new ArrayList<>(allMedikamente));
        state.setMyUnits(new ArrayList<>(myUnits));
        systemStateRepository.save(state);
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
}
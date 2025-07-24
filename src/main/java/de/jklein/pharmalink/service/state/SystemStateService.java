package de.jklein.pharmalink.service.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.domain.audit.ChaincodeEventLog;
import de.jklein.pharmalink.domain.system.SystemState;
import de.jklein.pharmalink.repository.ActorRepository;
import de.jklein.pharmalink.repository.MedikamentRepository;
import de.jklein.pharmalink.repository.UnitRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Getter
public class SystemStateService {

    private static final Logger logger = LoggerFactory.getLogger(SystemStateService.class);
    private static final String SYSTEM_STATE_ID = "pharmalink-system-state";

    // Event-Namen bleiben unverändert
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

    // Repositories und Services bleiben unverändert
    private final SystemStateRepository systemStateRepository;
    private final ActorRepository actorRepository;
    private final MedikamentRepository medikamentRepository;
    private final UnitRepository unitRepository;
    private final ChaincodeEventLogRepository eventLogRepository;
    private final FabricClient fabricClient;
    private final ActorFabricService actorFabricService;
    private final MedicationFabricService medicationFabricService;
    private final UnitFabricService unitFabricService;
    private final ObjectMapper objectMapper;

    @Value("${fabric.chaincode-name}")
    private String chaincodeName;

    private final AtomicReference<String> currentActorId = new AtomicReference<>();
    private CloseableIterator<ChaincodeEvent> chaincodeEventIterator;

    public SystemStateService(SystemStateRepository systemStateRepository, FabricClient fabricClient,
                              ActorFabricService actorFabricService, MedicationFabricService medicationFabricService,
                              UnitFabricService unitFabricService, ObjectMapper objectMapper,
                              ChaincodeEventLogRepository eventLogRepository, ActorRepository actorRepository,
                              MedikamentRepository medikamentRepository, UnitRepository unitRepository) {
        this.systemStateRepository = systemStateRepository;
        this.fabricClient = fabricClient;
        this.actorFabricService = actorFabricService;
        this.medicationFabricService = medicationFabricService;
        this.unitFabricService = unitFabricService;
        this.objectMapper = objectMapper;
        this.eventLogRepository = eventLogRepository;
        this.actorRepository = actorRepository;
        this.medikamentRepository = medikamentRepository;
        this.unitRepository = unitRepository;
    }

    @PostConstruct
    public void init() {
        logger.info("Initialisiere System-Status-Dienst...");
        loadStateFromDatabase();
        synchronizeWithChaincode();
        startEventListening();
    }

    private void loadStateFromDatabase() {
        systemStateRepository.findById(SYSTEM_STATE_ID).ifPresent(state -> {
            currentActorId.set(state.getCurrentActorId());
            logger.info("Gespeicherten Systemzustand aus der Datenbank geladen. Aktuelle Akteur-ID: {}", state.getCurrentActorId());
        });
    }

    @Transactional
    public void synchronizeWithChaincode() {
        logger.info("Synchronisiere globalen Zustand (Akteure, Medikamente) mit dem Chaincode...");
        try {
            List<Actor> actorsFromChaincode = actorFabricService.getAllActors();
            actorRepository.deleteAll();
            actorRepository.saveAll(actorsFromChaincode);
            logger.info("{} Akteure erfolgreich mit der Datenbank synchronisiert.", actorsFromChaincode.size());

            List<Medikament> medikamenteFromChaincode = medicationFabricService.getAllMedikamente();
            medikamentRepository.deleteAll();
            medikamentRepository.saveAll(medikamenteFromChaincode);
            logger.info("{} Medikamente erfolgreich mit der Datenbank synchronisiert.", medikamenteFromChaincode.size());

            if (StringUtils.hasText(currentActorId.get())) {
                synchronizeUnitsForActor(currentActorId.get());
            }

        } catch (Exception e) {
            logger.error("KRITISCH: Globaler Zustand konnte nicht mit dem Chaincode synchronisiert werden. Grund: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Fahre System-Status-Dienst herunter...");
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

    @Transactional
    public void handleChaincodeEvent(ChaincodeEvent event) {
        logEventForAudit(event);
        try {
            if (event.getPayload() == null || event.getPayload().length == 0) {
                logger.warn("Ereignis '{}' mit leerem Inhalt empfangen. Wird übersprungen.", event.getEventName());
                return;
            }

            JsonNode payload = objectMapper.readTree(event.getPayload());

            switch (event.getEventName()) {
                case ACTOR_INITIALIZED_EVENT, ACTOR_CREATED_EVENT, ACTOR_UPDATED_EVENT, ACTOR_IPFS_LINK_UPDATED_EVENT ->
                        handleActorUpdate(getIdFromPayload(payload, "actorId"));
                case ACTOR_DELETED_EVENT ->
                        handleActorDelete(getIdFromPayload(payload, "actorId"));
                case MEDIKAMENT_CREATED_EVENT, MEDIKAMENT_STATUS_UPDATED_EVENT, MEDIKAMENT_UPDATED_EVENT, MEDIKAMENT_TAG_ADDED_EVENT ->
                        handleMedikamentUpdate(getIdFromPayload(payload, "medId"));
                case MEDIKAMENT_DELETED_EVENT ->
                        handleMedikamentDelete(getIdFromPayload(payload, "medId"));
                case UNIT_CREATED_EVENT ->
                        handleUnitBatchCreation(payload);
                case UNIT_TEMPERATURE_ADDED_EVENT ->
                        handleUnitUpdate(getIdFromPayload(payload, "unitId"));
                case UNIT_TRANSFERRED_EVENT ->
                        handleUnitTransfer(payload);
                case UNIT_DELETED_EVENT ->
                        handleUnitDelete(getIdFromPayload(payload, "unitId"));
                default -> logger.warn("Unbehandeltes Ereignis empfangen: {}. Inhalt: {}", event.getEventName(), payload.toString());
            }
        } catch (IOException e) {
            logger.error("Fehler beim Verarbeiten des Ereignisinhalts für Ereignis '{}': {}", event.getEventName(), e.getMessage(), e);
        }
    }

    private Optional<String> getIdFromPayload(JsonNode payload, String fieldName) {
        return payload.has(fieldName) ? Optional.of(payload.get(fieldName).asText()) : Optional.empty();
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

    private void handleActorUpdate(Optional<String> actorIdOpt) {
        actorIdOpt.ifPresent(actorId -> actorFabricService.getEnrichedActorById(actorId).ifPresent(actorFromChaincode -> {
            Optional<Actor> existingActorOpt = actorRepository.findByActorId(actorId);
            Actor actorToSave = existingActorOpt.orElse(actorFromChaincode);
            if (existingActorOpt.isPresent()) {
                actorToSave.setBezeichnung(actorFromChaincode.getBezeichnung());
                actorToSave.setRole(actorFromChaincode.getRole());
                actorToSave.setEmail(actorFromChaincode.getEmail());
                actorToSave.setIpfsLink(actorFromChaincode.getIpfsLink());
                actorToSave.setIpfsData(actorFromChaincode.getIpfsData());
            }
            actorRepository.save(actorToSave);
            logger.info("Akteur {} in der Datenbank erstellt/aktualisiert.", actorId);
        }));
    }

    private void handleActorDelete(Optional<String> actorIdOpt) {
        actorIdOpt.ifPresent(actorId -> {
            actorRepository.deleteByActorId(actorId);
            logger.info("Akteur {} aus der Datenbank entfernt.", actorId);
        });
    }

    private void handleMedikamentUpdate(Optional<String> medIdOpt) {
        medIdOpt.ifPresent(medId -> medicationFabricService.getEnrichedMedikamentById(medId).ifPresent(medikamentFromChaincode -> {
            Optional<Medikament> existingMedikamentOpt = medikamentRepository.findByMedId(medId);
            Medikament medikamentToSave = existingMedikamentOpt.orElse(medikamentFromChaincode);
            if(existingMedikamentOpt.isPresent()){
                medikamentToSave.setBezeichnung(medikamentFromChaincode.getBezeichnung());
                medikamentToSave.setStatus(medikamentFromChaincode.getStatus());
                medikamentToSave.setInfoblattHash(medikamentFromChaincode.getInfoblattHash());
                medikamentToSave.setIpfsLink(medikamentFromChaincode.getIpfsLink());
                medikamentToSave.setApprovedById(medikamentFromChaincode.getApprovedById());
                medikamentToSave.setTags(medikamentFromChaincode.getTags());
                medikamentToSave.setIpfsData(medikamentFromChaincode.getIpfsData());
            }
            medikamentRepository.save(medikamentToSave);
            logger.info("Medikament {} in der Datenbank erstellt/aktualisiert.", medId);
        }));
    }

    private void handleMedikamentDelete(Optional<String> medIdOpt) {
        medIdOpt.ifPresent(medId -> {
            medikamentRepository.deleteByMedId(medId);
            logger.info("Medikament {} aus der Datenbank entfernt.", medId);
        });
    }

    private void handleUnitBatchCreation(JsonNode payload) {
        Optional<String> unitIdOpt = getIdFromPayload(payload, "unitId");
        if (unitIdOpt.isEmpty()) {
            logger.error("UnitCreated-Ereignis ohne unitId erhalten. Payload: {}", payload.toString());
            return;
        }

        String exampleUnitId = unitIdOpt.get();
        int lastDash = exampleUnitId.lastIndexOf('-');
        if (lastDash == -1) {
            logger.error("Ungültiges Format der Unit-ID für die Chargen-Erstellung: {}", exampleUnitId);
            handleUnitUpdate(Optional.of(exampleUnitId));
            return;
        }

        try {
            String idPrefix = exampleUnitId.substring(0, lastDash + 1);
            int count = Integer.parseInt(exampleUnitId.substring(lastDash + 1));

            logger.info("Starte Chargen-Erstellung für {} Einheiten mit Präfix '{}'.", count, idPrefix);

            List<Unit> batch = new ArrayList<>(count);
            for (int i = 1; i <= count; i++) {
                Unit newUnit = objectMapper.treeToValue(payload, Unit.class);
                newUnit.setUnitId(idPrefix + i);
                batch.add(newUnit);
            }

            unitRepository.saveAll(batch);
            logger.info("{} Einheiten erfolgreich in der Datenbank erstellt.", batch.size());

        } catch (NumberFormatException | IOException e) {
            logger.error("Fehler beim Parsen der Unit-ID für Charge, versuche Fallback. ID: {}, Fehler: {}", exampleUnitId, e.getMessage());
            handleUnitUpdate(Optional.of(exampleUnitId));
        }
    }

    private void handleUnitTransfer(JsonNode payload) {
        String newOwnerId = getIdFromPayload(payload, "currentOwnerActorId").orElse(null);
        String fromActorId = null;

        if (payload.has("transferHistory") && payload.get("transferHistory").isArray()) {
            JsonNode historyArray = payload.get("transferHistory");
            if (historyArray.size() > 0) {
                JsonNode lastTransfer = historyArray.get(historyArray.size() - 1);
                fromActorId = getIdFromPayload(lastTransfer, "fromActorId").orElse(null);
            }
        }

        if (StringUtils.hasText(fromActorId) && StringUtils.hasText(newOwnerId)) {
            logger.info("Unit-Transfer erkannt. Synchronisiere Inventar für Sender {} und Empfänger {}.", fromActorId, newOwnerId);
            synchronizeUnitsForActor(fromActorId);
            synchronizeUnitsForActor(newOwnerId);
        } else {
            logger.warn("Konnte Sender oder Empfänger aus Transfer-Ereignis nicht extrahieren. Payload: {}", payload.toString());
            handleUnitUpdate(getIdFromPayload(payload, "unitId"));
        }
    }

    private void handleUnitUpdate(Optional<String> unitIdOpt) {
        unitIdOpt.ifPresent(unitId -> unitFabricService.getEnrichedUnitById(unitId).ifPresent(unitFromChaincode -> {
            Optional<Unit> existingUnitOpt = unitRepository.findByUnitId(unitId);
            Unit unitToSave = existingUnitOpt.orElse(unitFromChaincode);
            if (existingUnitOpt.isPresent()) {
                unitToSave.setChargeBezeichnung(unitFromChaincode.getChargeBezeichnung());
                unitToSave.setIpfsLink(unitFromChaincode.getIpfsLink());
                unitToSave.setCurrentOwnerActorId(unitFromChaincode.getCurrentOwnerActorId());
                unitToSave.setTransferHistory(unitFromChaincode.getTransferHistory());
                unitToSave.setTemperatureReadings(unitFromChaincode.getTemperatureReadings());
                unitToSave.setConsumed(unitFromChaincode.isConsumed());
                unitToSave.setConsumedRefId(unitFromChaincode.getConsumedRefId());
                unitToSave.setIpfsData(unitFromChaincode.getIpfsData());
            }
            unitRepository.save(unitToSave);
            logger.info("Einheit {} in der Datenbank erstellt/aktualisiert.", unitId);
        }));
    }

    private void handleUnitDelete(Optional<String> unitIdOpt) {
        unitIdOpt.ifPresent(unitId -> {
            unitRepository.deleteByUnitId(unitId);
            logger.info("Einheit {} aus der Datenbank entfernt.", unitId);
        });
    }

    @Transactional
    public void reconcileAndCacheActorId(String actorId) {
        if (!Objects.equals(currentActorId.get(), actorId)) {
            logger.info("Aktuelle Akteur-ID von {} auf {} geändert.", currentActorId.get(), actorId);
            currentActorId.set(actorId);
            saveStateToDatabase();

            if (StringUtils.hasText(actorId)) {
                synchronizeUnitsForActor(actorId);
            }
        }
    }

    @Transactional
    public void synchronizeUnitsForActor(String actorId) {
        if (!StringUtils.hasText(actorId)) return;

        try {
            logger.info("Starte schnelle Synchronisierung der Einheiten für Akteur {}.", actorId);

            List<Unit> unitsFromChaincode = unitFabricService.getUnitsByOwner(actorId);

            unitRepository.deleteByCurrentOwnerActorId(actorId);

            if (!unitsFromChaincode.isEmpty()) {
                unitRepository.saveAll(unitsFromChaincode);
            }

            logger.info("{} Einheiten für Akteur {} erfolgreich synchronisiert.", unitsFromChaincode.size(), actorId);

        } catch (Exception e) {
            logger.error("Fehler bei der Synchronisierung der Einheiten für Akteur {}: {}", actorId, e.getMessage(), e);
        }
    }

    public void saveStateToDatabase() {
        SystemState state = systemStateRepository.findById(SYSTEM_STATE_ID)
                .orElse(new SystemState(SYSTEM_STATE_ID, null));
        state.setCurrentActorId(currentActorId.get());
        systemStateRepository.save(state);
        logger.info("Systemzustand (currentActorId: {}) in der Datenbank gespeichert.", currentActorId.get());
    }
}
package de.jklein.pharmalink.service.system;

import de.jklein.pharmalink.api.dto.ActorResponseDto; // DTO Imports bleiben, falls von anderen Methoden benötigt
import de.jklein.pharmalink.api.dto.MedikamentResponseDto;
import de.jklein.pharmalink.api.dto.UnitResponseDto;
import de.jklein.pharmalink.api.mapper.ActorMapper;
import de.jklein.pharmalink.api.mapper.MedikamentMapper;
import de.jklein.pharmalink.api.mapper.UnitMapper;
import de.jklein.pharmalink.domain.system.SystemState;
import de.jklein.pharmalink.domain.Actor; // NEU: Domain-Objekt Imports
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.repository.system.SystemStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemStateService {

    private final SystemStateRepository systemStateRepository;
    private final ActorMapper actorMapper;
    private final MedikamentMapper medikamentMapper;
    private final UnitMapper unitMapper;

    private SystemState currentSystemState;

    @Transactional
    public SystemState reconcileAndCacheActorId(String actorId) {
        Optional<SystemState> existingState = systemStateRepository.findByCurrentActorId(actorId);

        if (existingState.isPresent()) {
            currentSystemState = existingState.get();
            log.info("Existing SystemState for Actor ID {} loaded.", actorId);
        } else {
            List<SystemState> allStates = systemStateRepository.findAll();
            if (!allStates.isEmpty()) {
                currentSystemState = allStates.get(0);
                currentSystemState.setCurrentActorId(actorId);
                log.info("SystemState updated to new Actor ID: {}", actorId);
            } else {
                currentSystemState = new SystemState(actorId);
                log.info("New SystemState created for Actor ID: {}", actorId);
            }
            systemStateRepository.save(currentSystemState);
        }
        return currentSystemState;
    }

    public void loadFromDatabaseOnFailure() {
        log.warn("Attempting to load SystemState from database due to initialization failure.");
        List<SystemState> allStates = systemStateRepository.findAll();
        if (!allStates.isEmpty()) {
            this.currentSystemState = allStates.get(0);
            log.info("SystemState loaded from database: Actor ID {}", currentSystemState.getCurrentActorId());
        } else {
            log.error("No SystemState found in database after initialization failure. Application may not function correctly.");
        }
    }

    public SystemState getCurrentSystemState() {
        return currentSystemState;
    }

    // UPDATE-METHODEN AKZEPTIEREN NUN DOMAIN-OBJEKTE DIREKT
    @Transactional
    public void updateAllActors(List<Actor> actors) { // Parameter geändert
        if (currentSystemState == null) {
            log.error("currentSystemState is null. Cannot update actors.");
            return;
        }
        currentSystemState.setAllActors(actors); // Direkte Zuweisung
        log.debug("SystemState: All actors updated.");
    }

    @Transactional
    public void updateAllMedikamente(List<Medikament> medikamente) { // Parameter geändert
        if (currentSystemState == null) {
            log.error("currentSystemState is null. Cannot update medikamente.");
            return;
        }
        currentSystemState.setAllMedikamente(medikamente); // Direkte Zuweisung
        log.debug("SystemState: All medikamente updated.");
    }

    @Transactional
    public void updateMyUnits(List<Unit> units) { // Parameter geändert
        if (currentSystemState == null) {
            log.error("currentSystemState is null. Cannot update units.");
            return;
        }
        currentSystemState.setMyUnits(units); // Direkte Zuweisung
        log.debug("SystemState: My units updated.");
    }

    public List<Actor> getAllActors() {
        return currentSystemState != null ? currentSystemState.getAllActors() : Collections.emptyList();
    }

    public List<Medikament> getAllMedikamente() {
        return currentSystemState != null ? currentSystemState.getAllMedikamente() : Collections.emptyList();
    }

    public List<Unit> getMyUnits() {
        return currentSystemState != null ? currentSystemState.getMyUnits() : Collections.emptyList();
    }
}
package de.jklein.pharmalink.api.controller;

import de.jklein.pharmalink.api.dto.ActorResponseDto;
import de.jklein.pharmalink.api.dto.MedikamentResponseDto;
import de.jklein.pharmalink.api.dto.SystemStateDto;
import de.jklein.pharmalink.api.dto.SystemStatsDto;
import de.jklein.pharmalink.api.dto.UnitResponseDto;
import de.jklein.pharmalink.api.mapper.ActorMapper;
import de.jklein.pharmalink.api.mapper.MedikamentMapper;
import de.jklein.pharmalink.api.mapper.UnitMapper;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.service.state.SystemStateService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemStateService systemStateService;
    private final ActorMapper actorMapper;
    private final MedikamentMapper medikamentMapper;
    private final UnitMapper unitMapper;

    public SystemController(SystemStateService systemStateService, ActorMapper actorMapper, MedikamentMapper medikamentMapper, UnitMapper unitMapper) {
        this.systemStateService = systemStateService;
        this.actorMapper = actorMapper;
        this.medikamentMapper = medikamentMapper;
        this.unitMapper = unitMapper;
    }

    @GetMapping("/current-actor-id")
    @Operation(summary = "Aktuelle Akteur-ID abrufen", description = "Ruft die ID des Akteurs ab.")
    public ResponseEntity<Map<String, String>> getCurrentActorId() {
        String actorId = systemStateService.getCurrentActorId().get();

        if (!StringUtils.hasText(actorId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(Map.of("actorId", actorId));
    }

    @GetMapping("/cache/stats")
    @Operation(summary = "Statistiken abrufen", description = "Gibt eine schnelle Zusammenfassung.")
    public ResponseEntity<SystemStatsDto> getCacheStats() {
        SystemStatsDto statsDto = new SystemStatsDto(
                systemStateService.getAllActors().size(),
                systemStateService.getAllMedikamente().size(),
                systemStateService.getMyUnits().size()
        );
        return ResponseEntity.ok(statsDto);
    }

    @GetMapping("/cache/state")
    @Operation(summary = "Zustand des Zwischenspeichers abrufen", description = ".")
    public ResponseEntity<SystemStateDto> getCacheState() {
        String actorId = systemStateService.getCurrentActorId().get();

        List<Actor> allActors = systemStateService.getAllActors();
        List<Medikament> allMedikamente = systemStateService.getAllMedikamente();
        List<Unit> myUnits = systemStateService.getMyUnits();

        List<ActorResponseDto> actorsDto = actorMapper.toDtoList(allActors);
        List<MedikamentResponseDto> medikamenteDto = medikamentMapper.toDtoList(allMedikamente);
        List<UnitResponseDto> unitsDto = unitMapper.toDtoList(myUnits);

        SystemStateDto stateDto = new SystemStateDto(
                actorId,
                allActors.size(),
                allMedikamente.size(),
                myUnits.size(),
                actorsDto,
                medikamenteDto,
                unitsDto
        );

        return ResponseEntity.ok(stateDto);
    }
}
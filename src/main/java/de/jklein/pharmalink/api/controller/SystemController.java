package de.jklein.pharmalink.api.controller;

import de.jklein.pharmalink.api.dto.ActorResponseDto;
import de.jklein.pharmalink.api.dto.MedikamentResponseDto;
import de.jklein.pharmalink.api.dto.SystemStateDto;
import de.jklein.pharmalink.api.dto.SystemStatsDto;
import de.jklein.pharmalink.api.dto.UnitResponseDto;
import de.jklein.pharmalink.api.mapper.ActorMapper;
import de.jklein.pharmalink.api.mapper.MedikamentMapper;
import de.jklein.pharmalink.api.mapper.UnitMapper;
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
    @Operation(summary = "Get Current Actor ID", description = "Retrieves the ID of the actor currently registered in the system state.")
    public ResponseEntity<Map<String, String>> getCurrentActorId() {
        // KORREKTUR: .get() verwenden, um den Wert aus der AtomicReference zu holen.
        String actorId = systemStateService.getCurrentActorId().get();

        if (!StringUtils.hasText(actorId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(Map.of("actorId", actorId));
    }

    @GetMapping("/cache/stats")
    @Operation(summary = "Get Cache Statistics", description = "Returns a quick summary of the number of items currently held in the in-memory cache.")
    public ResponseEntity<SystemStatsDto> getCacheStats() {
        SystemStatsDto statsDto = new SystemStatsDto(
                systemStateService.getAllActors().size(),
                systemStateService.getAllMedikamente().size(),
                systemStateService.getMyUnits().size()
        );
        return ResponseEntity.ok(statsDto);
    }

    @GetMapping("/cache/state")
    @Operation(summary = "Get Live Cache State", description = "Returns the complete current in-memory state of the application cache.")
    public ResponseEntity<SystemStateDto> getCacheState() {
        // KORREKTUR: .get() verwenden, um den Wert aus der AtomicReference zu holen.
        String actorId = systemStateService.getCurrentActorId().get();

        // Konvertiere Domänenobjekte in DTOs
        List<ActorResponseDto> actorsDto = actorMapper.toDtoList(systemStateService.getAllActors());
        List<MedikamentResponseDto> medikamenteDto = medikamentMapper.toDtoList(systemStateService.getAllMedikamente());
        List<UnitResponseDto> unitsDto = unitMapper.toDtoList(systemStateService.getMyUnits());

        // Erstelle das finale DTO für die Antwort
        SystemStateDto stateDto = new SystemStateDto(
                actorId,
                actorsDto.size(),
                medikamenteDto.size(),
                unitsDto.size(),
                actorsDto,
                medikamenteDto,
                unitsDto
        );

        return ResponseEntity.ok(stateDto);
    }
}
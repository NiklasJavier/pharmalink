package de.jklein.pharmalink.api.controller;

import de.jklein.pharmalink.api.dto.SystemStateDto;
import de.jklein.pharmalink.api.dto.SystemStatsDto;
import de.jklein.pharmalink.service.state.SystemStateService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map; // NEU: Import hinzufügen

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemStateService systemStateService;

    public SystemController(SystemStateService systemStateService) {
        this.systemStateService = systemStateService;
    }

    /**
     * **NEUER ENDPUNKT**: Gibt die ID des aktuell im System registrierten Akteurs zurück.
     */
    @GetMapping("/current-actor-id")
    @Operation(summary = "Get Current Actor ID", description = "Retrieves the ID of the actor currently registered in the state state.")
    public ResponseEntity<Map<String, String>> getCurrentActorId() {
        String actorId = systemStateService.getCurrentActorId().get();
        if (actorId == null || actorId.isEmpty()) {
            return ResponseEntity.noContent().build(); // Gibt 204 zurück, wenn kein Akteur gesetzt ist
        }
        return ResponseEntity.ok(Map.of("actorId", actorId));
    }

    // --- Bestehende Endpunkte bleiben unverändert ---

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
        String actorId = systemStateService.getCurrentActorId().get();
        var actors = systemStateService.getAllActors();
        var medikamente = systemStateService.getAllMedikamente();
        var units = systemStateService.getMyUnits();
        SystemStateDto stateDto = new SystemStateDto(actorId, actors.size(), medikamente.size(), units.size(), actors, medikamente, units);
        return ResponseEntity.ok(stateDto);
    }
}
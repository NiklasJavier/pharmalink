package de.jklein.pharmalink.api.controller;

import de.jklein.pharmalink.api.dto.SystemStateDto; // NEU
import de.jklein.pharmalink.service.system.SystemStateService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping; // NEU
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemStateService systemStateService;

    public SystemController(SystemStateService systemStateService) {
        this.systemStateService = systemStateService;
    }

    /**
     * NEUE METHODE: Gibt den aktuellen In-Memory-Zustand des SystemStateService zurück.
     */
    @GetMapping("/cache/state")
    @Operation(summary = "Get Live Cache State", description = "Returns the complete current in-memory state of the application cache (actors, medications, units).")
    public ResponseEntity<SystemStateDto> getCacheState() {
        // Ruft die Listen und die ID aus dem Service ab
        String actorId = systemStateService.getCurrentActorId().get();
        var actors = systemStateService.getAllActors();
        var medikamente = systemStateService.getAllMedikamente();
        var units = systemStateService.getMyUnits();

        // Erstellt das DTO für eine saubere Antwort
        SystemStateDto stateDto = new SystemStateDto(
                actorId,
                actors.size(),
                medikamente.size(),
                units.size(),
                actors,
                medikamente,
                units
        );

        return ResponseEntity.ok(stateDto);
    }

    @PostMapping("/cache/persist")
    @Operation(summary = "Persist Cache to DB", description = "Forces the application to write the current in-memory cache state to the database. Useful for debugging.")
    public ResponseEntity<String> persistCache() {
        try {
            systemStateService.saveStateToDatabase(); // Annahme: Methode wurde öffentlich gemacht
            return ResponseEntity.ok("System state cache was successfully persisted to the database.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to persist cache: " + e.getMessage());
        }
    }
}
package de.jklein.pharmalink.api.controller.fabric;

import de.jklein.pharmalink.api.dto.ActorResponseDto;
import de.jklein.pharmalink.api.dto.MedikamentResponseDto;
import de.jklein.pharmalink.api.mapper.ActorMapper;
import de.jklein.pharmalink.api.mapper.MedikamentMapper;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.service.fabric.ActorFabricService;
import de.jklein.pharmalink.service.fabric.MedicationFabricService;
import de.jklein.pharmalink.service.state.SystemStateService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hersteller")
public class HerstellerFabricController {

    private final MedicationFabricService medicationService;
    private final SystemStateService systemStateService;
    private final ActorFabricService actorService;
    private final ActorMapper actorMapper;
    private final MedikamentMapper medikamentMapper;

    @Autowired
    public HerstellerFabricController(MedicationFabricService medicationService, SystemStateService systemStateService,
                                      ActorFabricService actorService, ActorMapper actorMapper, MedikamentMapper medikamentMapper) {
        this.medicationService = medicationService;
        this.systemStateService = systemStateService;
        this.actorService = actorService;
        this.actorMapper = actorMapper;
        this.medikamentMapper = medikamentMapper;
    }

    @GetMapping("/{herstellerId}")
    @Operation(summary = "Get Manufacturer by ID", description = "Retrieves public information for a specific manufacturer.")
    public ResponseEntity<ActorResponseDto> getHerstellerById(@PathVariable final String herstellerId) {
        return actorService.getEnrichedActorById(herstellerId)
                .map(actorMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/me")
    @Operation(summary = "Get My Manufacturer Info", description = "Retrieves information for the currently initialized manufacturer.")
    public ResponseEntity<?> getMyInfo() {
        final String actorId = systemStateService.getCurrentActorId().get();
        if (actorId == null || actorId.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Application is not initialized with a manufacturer ID."));
        }
        return actorService.getEnrichedActorById(actorId)
                .map(actorMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{herstellerId}/medications")
    @Operation(summary = "Get Medications by Manufacturer", description = "Retrieves all medications for a specific manufacturer.")
    public ResponseEntity<?> getMedicationsByHersteller(@PathVariable final String herstellerId) {
        try {
            List<Medikament> medikamente = medicationService.getMedikamenteByHerstellerId(herstellerId);
            List<MedikamentResponseDto> medikamentDtos = medikamentMapper.toDtoList(medikamente);
            return ResponseEntity.ok(medikamentDtos);
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Error fetching medications: " + e.getMessage()));
        }
    }

    @GetMapping("/id")
    @Operation(summary = "Get My Actor ID", description = "Returns the actorId of the user initialized at application startup.")
    public ResponseEntity<?> getMyActorId() {
        final String actorId = systemStateService.getCurrentActorId().get();
        if (actorId == null || actorId.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No initialized Actor-ID found."));
        }
        return ResponseEntity.ok(Map.of("actorId", actorId));
    }

}
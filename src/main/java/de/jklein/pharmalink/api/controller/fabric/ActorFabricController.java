package de.jklein.pharmalink.api.controller.fabric;

import de.jklein.pharmalink.api.dto.ActorResponseDto;
import de.jklein.pharmalink.api.dto.UpdateActorRequestDto;
import de.jklein.pharmalink.api.mapper.ActorMapper; // NEU: Import für ActorMapper
import de.jklein.pharmalink.domain.Actor; // NEU: Import für Actor-Domain-Objekt
import de.jklein.pharmalink.service.fabric.ActorFabricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors; // NEU: Import für Collectors

@RestController
@RequestMapping("/api/v1/actors")
public class ActorFabricController {

    private final ActorFabricService actorFabricService;
    private final ActorMapper actorMapper; // NEU: ActorMapper injizieren

    @Autowired
    public ActorFabricController(ActorFabricService actorFabricService, ActorMapper actorMapper) { // NEU: Im Konstruktor hinzufügen
        this.actorFabricService = actorFabricService;
        this.actorMapper = actorMapper;
    }

    @PutMapping("/{actorId}")
    public ResponseEntity<?> updateActor(
            @PathVariable final String actorId,
            @RequestBody final UpdateActorRequestDto requestDto) {
        try {
            Actor updatedActor = actorFabricService.updateActor(
                    actorId,
                    requestDto.getName(),
                    requestDto.getEmail(),
                    requestDto.getIpfsData()
            );
            ActorResponseDto responseDto = actorMapper.toDto(updatedActor);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler beim Aktualisieren des Akteurs: " + e.getMessage()));
        }
    }

    /**
     * Ruft eine Liste von Akteuren ab, optional gefiltert nach Rolle.
     * Endpunkt: GET /api/v1/actors?role=hersteller
     *
     * @param role Der optionale Rollen-Parameter, nach dem gefiltert wird.
     * @return Eine Liste von Akteuren als DTOs.
     */
    @GetMapping
    public ResponseEntity<List<ActorResponseDto>> getActorsByRole(@RequestParam(name = "role") final String role) {
        // Services geben jetzt Domain-Objekte zurück
        List<Actor> actors = actorFabricService.getActorsByRole(role);
        // Konvertierung von Domain-Objekten zu DTOs für die API-Antwort
        List<ActorResponseDto> actorDtos = actors.stream()
                .map(actorMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(actorDtos);
    }
}
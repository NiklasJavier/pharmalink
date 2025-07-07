package de.jklein.pharmalink.api.controller.fabric;

import de.jklein.pharmalink.api.dto.ActorResponseDto;
import de.jklein.pharmalink.api.mapper.ActorMapper; // NEU: Import für ActorMapper
import de.jklein.pharmalink.domain.Actor; // NEU: Import für Actor-Domain-Objekt
import de.jklein.pharmalink.service.fabric.ActorFabricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
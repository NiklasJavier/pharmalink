package de.jklein.pharmalink.api.controller;

import de.jklein.pharmalink.api.dto.ActorResponseDto;
import de.jklein.pharmalink.service.ActorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/actors")
public class ActorController {

    private final ActorService actorService;

    @Autowired
    public ActorController(ActorService actorService) {
        this.actorService = actorService;
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
        List<ActorResponseDto> actors = actorService.getActorsByRole(role);
        return ResponseEntity.ok(actors);
    }
}
package de.jklein.pharmalink.api.controller.fabric;

import de.jklein.pharmalink.api.dto.ActorResponseDto;
import de.jklein.pharmalink.api.dto.UpdateActorRequestDto;
import de.jklein.pharmalink.api.mapper.ActorMapper;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.service.fabric.ActorFabricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/actors")
public class ActorFabricController {

    private final ActorFabricService actorFabricService;
    private final ActorMapper actorMapper;

    @Autowired
    public ActorFabricController(ActorFabricService actorFabricService, ActorMapper actorMapper) {
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

    @GetMapping
    public ResponseEntity<List<ActorResponseDto>> getActorsByRole(@RequestParam(name = "role") final String role) {
        List<Actor> actors = actorFabricService.getActorsByRole(role);
        List<ActorResponseDto> actorDtos = actors.stream()
                .map(actorMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(actorDtos);
    }
}
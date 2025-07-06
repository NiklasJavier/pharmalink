package de.jklein.pharmalink.api.controller;

import de.jklein.pharmalink.api.dto.ActorResponseDto;
import de.jklein.pharmalink.api.dto.MedikamentResponseDto;
import de.jklein.pharmalink.api.mapper.ActorMapper;
import de.jklein.pharmalink.api.mapper.MedikamentMapper;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.service.ActorService;
import de.jklein.pharmalink.service.MedicationService;
import de.jklein.pharmalink.service.system.SystemStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/hersteller")
public class HerstellerController {

    private final MedicationService medicationService;
    private final SystemStateService systemStateService;
    private final ActorService actorService;
    private final ActorMapper actorMapper;
    private final MedikamentMapper medikamentMapper;

    @Autowired
    public HerstellerController(MedicationService medicationService, SystemStateService systemStateService,
                                ActorService actorService, ActorMapper actorMapper, MedikamentMapper medikamentMapper) {
        this.medicationService = medicationService;
        this.systemStateService = systemStateService;
        this.actorService = actorService;
        this.actorMapper = actorMapper;
        this.medikamentMapper = medikamentMapper;
    }

    /**
     * Ruft die öffentlichen Informationen eines bestimmten Herstellers ab.
     * Die Antwort wird mit Daten aus IPFS angereichert.
     * Endpunkt: GET /api/v1/hersteller/{herstellerId}
     *
     * @param herstellerId Die ID des Herstellers (actorId).
     * @return Ein DTO mit den Herstellerinformationen oder 404 Not Found.
     */
    @GetMapping("/{herstellerId}")
    public ResponseEntity<ActorResponseDto> getHerstellerById(@PathVariable final String herstellerId) {
        return actorService.getEnrichedActorById(herstellerId)
                .map(actorMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Gibt die Informationen des aktuell initialisierten Herstellers zurück.
     * Die Antwort wird mit Daten aus IPFS angereichert.
     * Endpunkt: GET /api/v1/me
     *
     * @return Ein DTO mit den Herstellerinformationen oder eine Fehlermeldung.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo() {
        final String actorId = systemStateService.getCurrentSystemState().getCurrentActorId();

        if (actorId == null || actorId.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Die Anwendung wurde nicht korrekt mit einer Hersteller-ID initialisiert."));
        }

        return actorService.getEnrichedActorById(actorId)
                .map(actorMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Ruft alle Medikamente ab, die zu einem explizit angegebenen Hersteller gehören.
     *
     * @param herstellerId Die ID des Herstellers.
     * @return Eine Liste der Medikamente als DTOs bei Erfolg, oder eine Fehlermeldung bei Fehler.
     */
    @GetMapping("/{herstellerId}/medications")
    public ResponseEntity<?> getMedicationsByHersteller(@PathVariable final String herstellerId) { // Rückgabetyp auf ResponseEntity<?> geändert
        try {
            List<Medikament> medikamente = medicationService.getMedikamenteByHerstellerId(herstellerId);
            List<MedikamentResponseDto> medikamentDtos = medikamente.stream()
                    .map(medikamentMapper::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(medikamentDtos);
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler bei der Abfrage der Medikamente: " + e.getMessage()));
        }
    }

    /**
     * Gibt die 'actorId' des beim Anwendungsstart initialisierten Benutzers zurück.
     * Endpunkt: GET /api/v1/hersteller/id
     *
     * @return Ein JSON-Objekt mit der actorId oder eine Fehlermeldung.
     */
    @GetMapping("/id")
    public ResponseEntity<?> getMyActorId() {
        final String actorId = systemStateService.getCurrentSystemState().getCurrentActorId();

        if (actorId == null || actorId.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Keine initialisierte Actor-ID gefunden."));
        }

        return ResponseEntity.ok(Map.of("actorId", actorId));
    }

    /**
     * Sucht nach Herstellern, deren Bezeichnung einen bestimmten Text enthält.
     * Endpunkt: GET /api/v1/hersteller/search?search=PharmaCorp
     *
     * @param nameQuery Der Text, nach dem in der Bezeichnung gesucht wird.
     * @return Eine Liste von passenden Herstellern als DTOs.
     */
    @GetMapping("/search")
    public ResponseEntity<List<ActorResponseDto>> searchHersteller(@RequestParam(name = "search") final String nameQuery) {
        List<Actor> hersteller = actorService.searchHerstellerByBezeichnung(nameQuery);
        List<ActorResponseDto> herstellerDtos = hersteller.stream()
                .map(actorMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(herstellerDtos);
    }
}
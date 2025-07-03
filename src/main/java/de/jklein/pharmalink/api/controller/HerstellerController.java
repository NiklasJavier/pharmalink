package de.jklein.pharmalink.api.controller;

import de.jklein.pharmalink.api.dto.ActorResponseDto;
import de.jklein.pharmalink.api.dto.MedikamentResponseDto;
import de.jklein.pharmalink.api.mapper.ActorMapper;
import de.jklein.pharmalink.service.ActorService;
import de.jklein.pharmalink.service.MedicationService;
import de.jklein.pharmalink.service.SystemStateService; // NEU: Import des SystemStateService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hersteller")
public class HerstellerController {

    private final MedicationService medicationService;
    private final SystemStateService systemStateService;
    private final ActorService actorService;
    private final ActorMapper actorMapper;

    @Autowired
    public HerstellerController(MedicationService medicationService, SystemStateService systemStateService,
                                ActorService actorService, ActorMapper actorMapper) {
        this.medicationService = medicationService;
        this.systemStateService = systemStateService;
        this.actorService = actorService;
        this.actorMapper = actorMapper;
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
        // Ruft die neue Service-Methode auf, die das fertige DTO liefert.
        return actorService.getEnrichedActorById(herstellerId)
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
    @GetMapping
    public ResponseEntity<?> getMyInfo() {
        // 1. Die ID wird aus dem globalen SystemStateService bezogen.
        final String actorId = systemStateService.getInitialActorId();

        if (actorId == null || actorId.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Die Anwendung wurde nicht korrekt mit einer Hersteller-ID initialisiert."));
        }

        // 2. Die bereits existierende Service-Methode wird aufgerufen, um die Daten abzurufen und anzureichern.
        return actorService.getEnrichedActorById(actorId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Ruft alle Medikamente ab, die zu einem explizit angegebenen Hersteller gehören.
     * Dieser Endpunkt bleibt unverändert.
     * Endpunkt: GET /api/v1/hersteller/{herstellerId}/medications
     *
     * @param herstellerId Die ID des Herstellers.
     * @return Eine Liste der Medikamente.
     */
    @GetMapping("/{herstellerId}/medications")
    public ResponseEntity<?> getMedicationsByHersteller(@PathVariable final String herstellerId) {
        try {
            List<MedikamentResponseDto> medikamente = medicationService.getMedikamenteByHerstellerId(herstellerId);
            return ResponseEntity.ok(medikamente);
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler bei der Abfrage der Medikamente: " + e.getMessage()));
        }
    }

    /**
     * Gibt die 'actorId' des beim Anwendungsstart initialisierten Benutzers zurück.
     * Endpunkt: GET /api/v1/me/id
     *
     * @return Ein JSON-Objekt mit der actorId oder eine Fehlermeldung.
     */
    @GetMapping("/id")
    public ResponseEntity<?> getMyActorId() {
        // 1. Die ID wird direkt aus dem globalen SystemStateService bezogen.
        final String actorId = systemStateService.getInitialActorId();

        // 2. Prüfen, ob eine ID initialisiert wurde.
        if (actorId == null || actorId.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Keine initialisierte Actor-ID gefunden."));
        }

        // 3. Die ID wird in einem einfachen JSON-Objekt zurückgegeben.
        return ResponseEntity.ok(Map.of("actorId", actorId));
    }
}
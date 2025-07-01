package de.jklein.pharmalink.api.v1;

import de.jklein.pharmalink.service.MedicationService;
import de.jklein.pharmalink.api.dto.CreateMedikamentRequest;
import de.jklein.pharmalink.api.dto.CreateUnitsRequest;
import de.jklein.pharmalink.service.SystemStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * REST-Controller für die Verwaltung von Medikamenten-Assets unter der API-Version 1.
 * Dieser Controller bildet die Schnittstelle zwischen HTTP-Anfragen und der Business-Logik
 * im MedicationService und orientiert sich eng an den Funktionen des Chaincodes.
 */
@RestController
@RequestMapping("/api/v1/medications")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService medicationService;
    private final SystemStateService systemStateService;

    /**
     * Erstellt einen neuen Medikamenten-Stammdatensatz.
     * Endpunkt: POST /api/v1/medications
     *
     * @param request DTO mit allen notwendigen Daten für die Erstellung eines Medikaments.
     * @return Eine HTTP-Antwort mit dem erstellten Asset als JSON-Body und dem Status 201 (Created).
     */
    @PostMapping
    public ResponseEntity<?> createMedication(@RequestBody final CreateMedikamentRequest request) {
        try {
            String currentUserIdentity = systemStateService.getInitialActorId();
            String resultJson = medicationService.createMedikament(currentUserIdentity, request);
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(resultJson);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Ruft einen spezifischen Medikamenten-Stammdatensatz anhand seiner ID ab.
     * Endpunkt: GET /api/v1/medications/{medId}
     *
     * @param medId Die ID des abzufragenden Medikaments.
     * @return Eine HTTP-Antwort mit dem gefundenen Asset als JSON-Body oder 404 (Not Found).
     */
    @GetMapping("/{medId}")
    public ResponseEntity<?> getMedicationById(@PathVariable final String medId) {
        try {
            // WICHTIG: Die Benutzeridentität muss auch hier aus dem Security Context kommen.
            String currentUserIdentity = systemStateService.getInitialActorId();

            String resultJson = medicationService.getMedikamentById(currentUserIdentity, medId);
            if (resultJson == null || resultJson.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resultJson);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Erstellt eine bestimmte Anzahl von verfolgbaren Einheiten (Units) für ein bestehendes Medikament.
     * Endpunkt: POST /api/v1/medications/{medId}/units
     *
     * @param medId   Die ID des Medikaments, für das Units erstellt werden sollen.
     * @param request DTO, das die Anzahl ('amount') der zu erstellenden Units enthält.
     * @return Eine HTTP-Antwort mit dem Status 202 (Accepted), da der Prozess initiiert wurde.
     */
    @PostMapping("/{medId}/units")
    public ResponseEntity<?> createUnitsForMedication(
            @PathVariable final String medId,
            @RequestBody final CreateUnitsRequest request) {
        try {
            // WICHTIG: Die Benutzeridentität muss aus dem Security Context kommen.
            String currentUserIdentity = systemStateService.getInitialActorId();

            medicationService.createUnitsForMedication(currentUserIdentity, medId, request.getAmount());
            String message = "Creation of " + request.getAmount() + " units for medication '" + medId + "' initiated.";
            return ResponseEntity.accepted().body(Collections.singletonMap("message", message));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
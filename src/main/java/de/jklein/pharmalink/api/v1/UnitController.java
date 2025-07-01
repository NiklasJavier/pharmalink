package de.jklein.pharmalink.api.v1;

import de.jklein.pharmalink.service.MedicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import de.jklein.pharmalink.service.SystemStateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * REST-Controller für die Verwaltung von einzelnen, verfolgbaren Einheiten (Units) unter der API-Version 1.
 */
@RestController
@RequestMapping("/api/v1/units")
@RequiredArgsConstructor
public class UnitController {

    private final MedicationService medicationService;
    private final SystemStateService systemStateService;

    /**
     * Ruft eine spezifische Unit anhand ihrer ID ab.
     * Endpunkt: GET /api/v1/units/{unitId}
     *
     * @param unitId Die ID der abzufragenden Unit.
     * @return Eine HTTP-Antwort mit der gefundenen Unit als JSON-Body oder 404 (Not Found).
     */
    @GetMapping("/{unitId}")
    public ResponseEntity<?> getUnitById(@PathVariable final String unitId) {
        try {
            String currentUserIdentity = systemStateService.getInitialActorId();
            String resultJson = medicationService.getUnitById(currentUserIdentity, unitId);

            if (resultJson == null || resultJson.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resultJson);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
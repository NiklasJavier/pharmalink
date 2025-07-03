package de.jklein.pharmalink.api.controller;

import de.jklein.pharmalink.api.dto.AddTemperatureReadingRequestDto;
import de.jklein.pharmalink.api.dto.CreateUnitsRequestDto;
import de.jklein.pharmalink.api.dto.TransferUnitRequestDto;
import de.jklein.pharmalink.api.dto.UnitResponseDto;
import de.jklein.pharmalink.service.UnitService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/units")
public class UnitController {

    private final UnitService unitService;

    @Autowired
    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    /**
     * Ruft eine einzelne, verfolgbare Einheit (Unit) anhand ihrer ID ab.
     * Die Antwort wird mit Daten aus IPFS angereichert.
     * Endpunkt: GET /api/v1/units/{unitId}
     *
     * @param unitId Die ID der abzurufenden Unit.
     * @return Ein DTO der Unit oder 404 Not Found.
     */
    @GetMapping("/{unitId}")
    public ResponseEntity<UnitResponseDto> getUnitById(@PathVariable final String unitId) {
        return unitService.getEnrichedUnitById(unitId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Erstellt neue, verfolgbare Einheiten (Units) für ein bestimmtes Medikament.
     * Endpunkt: POST /api/v1/medications/{medId}/units
     *
     * @param medId Die ID des Medikaments.
     * @param request Das DTO mit den Details für die zu erstellenden Units.
     * @return Eine Liste der erstellten Units als DTOs mit Status 201 (Created).
     */
    @PostMapping("/{medId}/units")
    public ResponseEntity<?> createUnitsForMedication(
            @PathVariable final String medId,
            @Valid @RequestBody final CreateUnitsRequestDto request) {

        try {
            List<UnitResponseDto> createdUnits = unitService.createUnitsForMedication(medId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUnits);
        } catch (Exception e) {
            // Fängt Fehler vom Chaincode ab (z.B. Medikament nicht freigegeben, nicht der Hersteller)
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler beim Erstellen der Units: " + e.getMessage()));
        }
    }

    /**
     * Ruft alle Units für ein bestimmtes Medikament ab, gruppiert nach Charge.
     * Endpunkt: GET /api/v1/medications/{medId}/units-by-charge
     *
     * @param medId Die ID des Medikaments.
     * @return Eine Map, die die Units nach Charge gruppiert enthält.
     */
    @GetMapping("/{medId}/units-by-charge")
    public ResponseEntity<?> getUnitsGroupedByCharge(@PathVariable final String medId) {
        try {
            Map<String, List<UnitResponseDto>> groupedUnits = unitService.getUnitsByMedIdGroupedByCharge(medId);
            return ResponseEntity.ok(groupedUnits);
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler beim Abrufen der gruppierten Units: " + e.getMessage()));
        }
    }

    /**
     * Überträgt eine Unit an einen neuen Besitzer.
     * Endpunkt: POST /api/v1/units/{unitId}/transfer
     *
     * @param unitId Die ID der zu übertragenden Unit.
     * @param request Das DTO, das die ID des neuen Besitzers enthält.
     * @return Die aktualisierte Unit als DTO.
     */
    @PostMapping("/{unitId}/transfer")
    public ResponseEntity<?> transferUnit(
            @PathVariable final String unitId,
            @Valid @RequestBody final TransferUnitRequestDto request) {

        try {
            UnitResponseDto updatedUnit = unitService.transferUnit(unitId, request.getNewOwnerActorId());
            return ResponseEntity.ok(updatedUnit);
        } catch (Exception e) {
            // Fängt Fehler vom Chaincode ab (z.B. "Aufrufer ist nicht der Besitzer").
            // Eine spezifischere Fehlerbehandlung könnte hier 403 Forbidden zurückgeben.
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler beim Transfer der Unit: " + e.getMessage()));
        }
    }

    /**
     * Fügt einen neuen Temperaturmesswert zu einer Unit hinzu.
     * Endpunkt: POST /api/v1/units/{unitId}/temperature-readings
     *
     * @param unitId Die ID der Unit.
     * @param request Das DTO, das die Temperatur und den Zeitstempel enthält.
     * @return Die aktualisierte Unit als DTO.
     */
    @PostMapping("/{unitId}/temperature-readings")
    public ResponseEntity<?> addTemperatureReading(
            @PathVariable final String unitId,
            @Valid @RequestBody final AddTemperatureReadingRequestDto request) {

        try {
            UnitResponseDto updatedUnit = unitService.addTemperatureReading(
                    unitId,
                    request.getTemperature(),
                    request.getTimestamp()
            );
            return ResponseEntity.ok(updatedUnit);
        } catch (Exception e) {
            // Fängt Fehler vom Chaincode ab (z.B. "Aufrufer ist nicht der Besitzer").
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler beim Hinzufügen der Temperaturdaten: " + e.getMessage()));
        }
    }
}
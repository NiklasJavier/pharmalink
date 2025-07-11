package de.jklein.pharmalink.api.controller.fabric;

import de.jklein.pharmalink.api.dto.*;
import de.jklein.pharmalink.api.mapper.UnitMapper;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.service.fabric.UnitFabricService;
import de.jklein.pharmalink.service.state.SystemStateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/units")
public class UnitFabricController {

    private final UnitFabricService unitFabricService;
    private final UnitMapper unitMapper;
    private final SystemStateService systemStateService;

    @Autowired
    public UnitFabricController(UnitFabricService unitFabricService, UnitMapper unitMapper, SystemStateService systemStateService) {
        this.unitFabricService = unitFabricService;
        this.unitMapper = unitMapper;
        this.systemStateService = systemStateService;
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
        // Service gibt Optional<Unit> zurück
        return unitFabricService.getEnrichedUnitById(unitId)
                // Optional<Unit> zu Optional<UnitResponseDto> mappen, dann zu ResponseEntity
                .map(unitMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Erstellt neue, verfolgbare Einheiten (Units) für ein bestimmtes Medikament.
     * Endpunkt: POST /api/v1/medications/{medId}/units
     *
     * @param medId Die ID des Medikaments.
     * @param request Das DTO mit den Details für die zu erstellenden Units.
     * @return Eine Liste der erstellten Units als DTOs mit Status 201 (Created) oder eine Fehlermeldung.
     */
    @PostMapping("/{medId}/units")
    public ResponseEntity<?> createUnitsForMedication(
            @PathVariable final String medId,
            @Valid @RequestBody final CreateUnitsRequestDto request) {

        try {
            // Service gibt List<Unit> zurück
            List<Unit> createdUnits = unitFabricService.createUnitsForMedication(medId, request);
            // Konvertierung von Domain zu DTO-Liste für die API-Antwort
            List<UnitResponseDto> createdUnitDtos = createdUnits.stream()
                    .map(unitMapper::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUnitDtos);
        } catch (Exception e) {
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
     * @return Eine Map, die die Units nach Charge gruppiert enthält, oder eine Fehlermeldung.
     */
    @GetMapping("/{medId}/units-by-charge")
    public ResponseEntity<?> getUnitsGroupedByCharge(@PathVariable final String medId) {
        try {
            // Service gibt Map<String, List<Unit>> zurück
            Map<String, List<Unit>> groupedUnitsDomain = unitFabricService.getUnitsByMedIdGroupedByCharge(medId);

            // Konvertierung von Map<String, List<Unit>> zu Map<String, List<UnitResponseDto>> für die API-Antwort
            Map<String, List<UnitResponseDto>> groupedUnitDtos = groupedUnitsDomain.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().stream()
                                    .map(unitMapper::toDto)
                                    .collect(Collectors.toList())
                    ));
            return ResponseEntity.ok(groupedUnitDtos);
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
     * @return Die aktualisierte Unit als DTO bei Erfolg, oder eine Fehlermeldung bei Fehler.
     */
    @PostMapping("/{unitId}/transfer")
    public ResponseEntity<?> transferUnit(
            @PathVariable final String unitId,
            @Valid @RequestBody final TransferUnitRequestDto request) {

        try {
            // Service gibt Unit (Domain-Objekt) zurück
            Unit updatedUnit = unitFabricService.transferUnit(unitId, request.getNewOwnerActorId());
            // Konvertierung von Domain zu DTO für die API-Antwort
            UnitResponseDto updatedUnitDto = unitMapper.toDto(updatedUnit);
            return ResponseEntity.ok(updatedUnitDto);
        } catch (Exception e) {
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
     * @return Die aktualisierte Unit als DTO bei Erfolg, oder eine Fehlermeldung bei Fehler.
     */
    @PostMapping("/{unitId}/temperature-readings")
    public ResponseEntity<?> addTemperatureReading(
            @PathVariable final String unitId,
            @Valid @RequestBody final AddTemperatureReadingRequestDto request) {

        try {
            // Service gibt Unit (Domain-Objekt) zurück
            Unit updatedUnit = unitFabricService.addTemperatureReading(
                    unitId,
                    request.getTemperature(),
                    request.getTimestamp()
            );
            // Konvertierung von Domain zu DTO für die API-Antwort
            UnitResponseDto updatedUnitDto = unitMapper.toDto(updatedUnit);
            return ResponseEntity.ok(updatedUnitDto);
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler beim Hinzufügen der Temperaturdaten: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{unitId}")
    public ResponseEntity<?> deleteUnit(@PathVariable String unitId) {
        try {
            unitFabricService.deleteUnit(unitId);
            return ResponseEntity.noContent().build(); // Standard-Antwort für erfolgreiches DELETE
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler beim Löschen der Charge: " + e.getMessage()));
        }
    }

    @DeleteMapping("/batch")
    public ResponseEntity<?> deleteUnitsInBatch(@RequestBody DeleteUnitsRequestDto requestDto) {
        try {
            unitFabricService.deleteUnits(requestDto.getUnitIds());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler beim Batch-Löschen der Chargen: " + e.getMessage()));
        }
    }

    @PostMapping("/transfer-range")
    public ResponseEntity<?> transferUnitRange(@RequestBody TransferUnitRangeRequestDto requestDto) {
        try {
            String resultMessage = unitFabricService.transferUnitRange(
                    requestDto.getMedId(),
                    requestDto.getChargeBezeichnung(),
                    requestDto.getStartCounter(),
                    requestDto.getEndCounter(),
                    requestDto.getNewOwnerId()
                    // Der Timestamp-Parameter wird hier entfernt
            );
            return ResponseEntity.ok(Map.of("message", resultMessage));
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler bei der Bereichsübertragung: " + e.getMessage()));
        }
    }

    /**
     * Ruft die Chargenbezeichnungen und die Anzahl der Einheiten pro Charge für ein bestimmtes Medikament ab.
     * Endpunkt: GET /api/v1/units/medications/{medId}/charge-counts
     *
     * @param medId Die ID des Medikaments.
     * @return Eine Map von Chargenbezeichnungen zu der jeweiligen Einheitenanzahl, oder eine Fehlermeldung.
     */
    @GetMapping("/medications/{medId}/charge-counts")
    public ResponseEntity<?> getChargeCountsByMedId(@PathVariable final String medId) {
        try {
            Map<String, Integer> chargeCounts = unitFabricService.getChargeCountsByMedId(medId);
            return ResponseEntity.ok(chargeCounts);
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler beim Abrufen der Chargenanzahlen: " + e.getMessage()));
        }
    }

    @GetMapping("/mine")
    public ResponseEntity<List<UnitResponseDto>> getMyUnits() {
        String ownerActorId = systemStateService.getCurrentActorId().get();
        List<Unit> units = unitFabricService.getUnitsByOwner(ownerActorId);
        List<UnitResponseDto> dtos = units.stream()
                .map(unitMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

}
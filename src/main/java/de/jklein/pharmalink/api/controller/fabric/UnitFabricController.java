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

    @GetMapping("/{unitId}")
    public ResponseEntity<UnitResponseDto> getUnitById(@PathVariable final String unitId) {
        return unitFabricService.getEnrichedUnitById(unitId)
                .map(unitMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{medId}/units")
    public ResponseEntity<?> createUnitsForMedication(
            @PathVariable final String medId,
            @Valid @RequestBody final CreateUnitsRequestDto request) {

        try {
            List<Unit> createdUnits = unitFabricService.createUnitsForMedication(medId, request);
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

    @GetMapping("/{medId}/units-by-charge")
    public ResponseEntity<?> getUnitsGroupedByCharge(@PathVariable final String medId) {
        try {
            Map<String, List<Unit>> groupedUnitsDomain = unitFabricService.getUnitsByMedIdGroupedByCharge(medId);

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

    @PostMapping("/{unitId}/transfer")
    public ResponseEntity<?> transferUnit(
            @PathVariable final String unitId,
            @Valid @RequestBody final TransferUnitRequestDto request) {

        try {
            Unit updatedUnit = unitFabricService.transferUnit(unitId, request.getNewOwnerActorId());
            UnitResponseDto updatedUnitDto = unitMapper.toDto(updatedUnit);
            return ResponseEntity.ok(updatedUnitDto);
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler beim Transfer der Unit: " + e.getMessage()));
        }
    }

    @PostMapping("/{unitId}/temperature-readings")
    public ResponseEntity<?> addTemperatureReading(
            @PathVariable final String unitId,
            @Valid @RequestBody final AddTemperatureReadingRequestDto request) {

        try {
            Unit updatedUnit = unitFabricService.addTemperatureReading(
                    unitId,
                    request.getTemperature(),
                    request.getTimestamp()
            );
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
            return ResponseEntity.noContent().build();
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
            );
            return ResponseEntity.ok(Map.of("message", resultMessage));
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler bei der Bereichsübertragung: " + e.getMessage()));
        }
    }

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
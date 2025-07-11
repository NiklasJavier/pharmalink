package de.jklein.pharmalink.api.controller.fabric;

import de.jklein.pharmalink.api.dto.CreateMedikamentRequestDto;
import de.jklein.pharmalink.api.dto.MedikamentResponseDto;
import de.jklein.pharmalink.api.dto.UpdateMedicationStatusRequestDto;
import de.jklein.pharmalink.api.dto.UpdateMedikamentRequestDto;
import de.jklein.pharmalink.api.mapper.MedikamentMapper;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.service.fabric.MedicationFabricService;
import de.jklein.pharmalink.service.state.SystemStateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/medications")
public class MedicationFabricController {

    private final MedicationFabricService medicationFabricService;
    private final SystemStateService systemStateService;
    private final MedikamentMapper medikamentMapper;

    @Autowired
    public MedicationFabricController(SystemStateService systemStateService, MedicationFabricService medicationFabricService, MedikamentMapper medikamentMapper) {
        this.medicationFabricService = medicationFabricService;
        this.systemStateService = systemStateService;
        this.medikamentMapper = medikamentMapper;
    }

    @PostMapping
    public ResponseEntity<?> createMedikament(
                                               @Valid @RequestBody final CreateMedikamentRequestDto request,
                                               final Principal principal) {

        try {
            Medikament createdMedikament = medicationFabricService.createMedikament(request);
            MedikamentResponseDto createdMedikamentDto = medikamentMapper.toDto(createdMedikament);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdMedikamentDto);
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler beim Erstellen des Medikaments: " + e.getMessage()));
        }
    }

    @GetMapping("/{medId}")
    public ResponseEntity<MedikamentResponseDto> getMedicationById(@PathVariable final String medId) {
        return medicationFabricService.getEnrichedMedikamentById(medId)
                .map(medikamentMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<?> getMyMedications() {
        final String herstellerId = systemStateService.getCurrentActorId().get();

        if (herstellerId == null || herstellerId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Die Anwendung wurde nicht korrekt mit einer Hersteller-ID initialisiert."));
        }

        try {
            List<Medikament> medikamente = medicationFabricService.getMedikamenteByHerstellerId(herstellerId);
            List<MedikamentResponseDto> medikamentDtos = medikamentMapper.toDtoList(medikamente);
            return ResponseEntity.ok(medikamentDtos);
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler bei der Abfrage der Medikamente: " + e.getMessage()));
        }
    }

    @PostMapping("/{medId}/approval")
    public ResponseEntity<?> approveMedication(
                                                @PathVariable final String medId,
                                                @Valid @RequestBody final UpdateMedicationStatusRequestDto request) {

        try {
            Medikament updatedMedikament = medicationFabricService.approveMedication(medId, request.getNewStatus());
            MedikamentResponseDto updatedMedikamentDto = medikamentMapper.toDto(updatedMedikament);
            return ResponseEntity.ok(updatedMedikamentDto);
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler bei der Statusänderung: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<MedikamentResponseDto>> searchMedications(@RequestParam(name = "search") final String searchQuery) {
        List<Medikament> medikamente = medicationFabricService.searchMedicationsByBezeichnung(searchQuery);
        List<MedikamentResponseDto> medikamentDtos = medikamentMapper.toDtoList(medikamente);
        return ResponseEntity.ok(medikamentDtos);
    }

    @PutMapping("/{medId}")
    public ResponseEntity<?> updateMedication(
            @PathVariable final String medId,
            @RequestBody final UpdateMedikamentRequestDto requestDto) {
        try {
            Medikament updatedMedikament = medicationFabricService.updateMedikament(
                    medId,
                    requestDto.getBezeichnung(),
                    requestDto.getInfoblattHash(),
                    requestDto.getIpfsData()
            );
            MedikamentResponseDto responseDto = medikamentMapper.toDto(updatedMedikament);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler beim Aktualisieren des Medikaments: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{medId}/conditional-delete") // nicht aktualisiert im state
    public ResponseEntity<?> deleteMedikamentIfNoUnits(@PathVariable String medId) {
        try {
            medicationFabricService.deleteMedikamentIfNoUnits(medId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            if (e.getMessage().contains("MEDIKAMENT_HAS_UNITS")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Löschen nicht möglich, da bereits Chargen für dieses Medikament existieren."));
            }
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler beim Löschen des Medikaments: " + e.getMessage()));
        }
    }
}
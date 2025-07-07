package de.jklein.pharmalink.api.controller.fabric;

import de.jklein.pharmalink.api.dto.CreateMedikamentRequestDto;
import de.jklein.pharmalink.api.dto.MedikamentResponseDto;
import de.jklein.pharmalink.api.dto.UpdateMedicationStatusRequestDto;
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

/**
 * REST-Controller für die Verwaltung von Medikamenten und deren Einheiten.
 * Dient als primäre Schnittstelle für die API-Clients.
 *
 * Die Hauptverantwortung dieses Controllers ist die Entgegennahme von HTTP-Anfragen,
 * die Validierung der Eingaben und die Delegation der Geschäftslogik an den MedicationService.
 * Er ist bewusst einfach gehalten und enthält keine Geschäftslogik.
 */
@RestController
@RequestMapping("/api/v1/medications")
public class MedicationFabricController {

    private final MedicationFabricService medicationFabricService;
    private final SystemStateService systemStateService;
    private final MedikamentMapper medikamentMapper;

    /**
     * Konstruktor zur Injektion der Services und Mapper durch Spring.
     *
     * @param medicationFabricService Der Service, der die Geschäftslogik kapselt.
     * @param systemStateService Der Service für den Systemzustand.
     * @param medikamentMapper Der Mapper für Medikamente.
     */
    @Autowired
    public MedicationFabricController(SystemStateService systemStateService, MedicationFabricService medicationFabricService, MedikamentMapper medikamentMapper) {
        this.medicationFabricService = medicationFabricService;
        this.systemStateService = systemStateService;
        this.medikamentMapper = medikamentMapper;
    }

    /**
     * Erstellt ein neues Medikament im System.
     * Endpunkt: POST /api/v1/medications
     *
     * @param request Das DTO mit den Daten des zu erstellenden Medikaments.
     * @param principal Das Sicherheitsobjekt des angemeldeten Benutzers (wird für die Berechtigung genutzt).
     * @return Das neu erstellte Medikament als DTO mit dem Status 201 (Created) oder eine Fehlermeldung.
     */
    @PostMapping
    public ResponseEntity<?> createMedikament( // Rückgabetyp auf ResponseEntity<?> geändert
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

    /**
     * Ruft ein spezifisches Medikament anhand seiner ID ab.
     * Die Antwort wird vom Service mit Daten aus weiteren Quellen (z.B. IPFS) angereichert.
     * Endpunkt: GET /api/v1/medications/{medId}
     *
     * @param medId Die eindeutige ID des Medikaments.
     * @return Ein DTO des Medikaments mit Status 200 (OK) oder 404 (Not Found), falls nicht gefunden.
     */
    @GetMapping("/{medId}")
    public ResponseEntity<MedikamentResponseDto> getMedicationById(@PathVariable final String medId) {
        return medicationFabricService.getEnrichedMedikamentById(medId)
                .map(medikamentMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Ruft alle Medikamente ab, die zum beim Anwendungsstart initialisierten Hersteller gehören.
     *
     * @return Eine Liste der Medikamente des initialisierten Herstellers als DTOs bei Erfolg, oder eine Fehlermeldung bei Fehler.
     */
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

    /**
     * Aktualisiert den Status eines Medikaments, z.B. zur Freigabe durch eine Behörde.
     *
     * @param medId Die ID des Medikaments.
     * @param request Das DTO mit dem neuen Status.
     * @return Das aktualisierte Medikament als DTO bei Erfolg, oder eine Fehlermeldung bei Fehler.
     */
    @PostMapping("/{medId}/approval")
    public ResponseEntity<?> approveMedication( // Rückgabetyp auf ResponseEntity<?> geändert
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

    /**
     * Sucht nach Medikamenten, deren Bezeichnung einen bestimmten Text enthält.
     *
     * @param searchQuery Der Text, nach dem in der Bezeichnung gesucht wird.
     * @return Eine Liste von passenden Medikamenten als DTOs.
     */
    @GetMapping("/search")
    public ResponseEntity<List<MedikamentResponseDto>> searchMedications(@RequestParam(name = "search") final String searchQuery) {
        List<Medikament> medikamente = medicationFabricService.searchMedicationsByBezeichnung(searchQuery);
        List<MedikamentResponseDto> medikamentDtos = medikamentMapper.toDtoList(medikamente);
        return ResponseEntity.ok(medikamentDtos);
    }
}
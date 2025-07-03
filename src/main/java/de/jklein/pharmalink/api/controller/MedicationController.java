package de.jklein.pharmalink.api.controller;

import de.jklein.pharmalink.api.dto.*;
import de.jklein.pharmalink.service.MedicationService;
import de.jklein.pharmalink.service.system.SystemStateService;
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
public class MedicationController {

    private final MedicationService medicationService;
    private final SystemStateService systemStateService;

    /**
     * Konstruktor zur Injektion des MedicationService durch Spring.
     *
     * @param medicationService Der Service, der die Geschäftslogik kapselt.
     */
    @Autowired
    public MedicationController(SystemStateService systemStateService, MedicationService medicationService) {
        this.medicationService = medicationService;
        this.systemStateService = systemStateService;
    }

    /**
     * Erstellt ein neues Medikament im System.
     * Endpunkt: POST /api/v1/medications
     *
     * @param request Das DTO mit den Daten des zu erstellenden Medikaments.
     * @param principal Das Sicherheitsobjekt des angemeldeten Benutzers (wird für die Berechtigung genutzt).
     * @return Das neu erstellte Medikament als DTO mit dem Status 201 (Created).
     */
    @PostMapping
    public ResponseEntity<?> createMedikament(
            @Valid @RequestBody final CreateMedikamentRequestDto request,
            final Principal principal) {

        // Die Identität des Aufrufers (principal.getName()) wird hier nicht direkt an den Service
        // übergeben, da der FabricClient bereits mit der Identität des Benutzers konfiguriert ist.
        // Man könnte sie aber für zusätzliche Autorisierungs-Checks im Service verwenden.

        try {
            MedikamentResponseDto createdMedikamentDto = medicationService.createMedikament(request);
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
        // Delegiert die Anfrage direkt an den Service.
        // Der Service ist verantwortlich für das Holen und Anreichern der Daten.
        return medicationService.getEnrichedMedikamentById(medId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Ruft alle Medikamente ab, die zum beim Anwendungsstart initialisierten Hersteller gehören.
     * Endpunkt: GET /api/v1/hersteller/medications
     *
     * @return Eine Liste der Medikamente des initialisierten Herstellers als DTOs.
     */
    @GetMapping
    public ResponseEntity<?> getMyMedications() {
        // 1. Die ID des Herstellers wird aus dem globalen SystemStateService bezogen.
        final String herstellerId = systemStateService.getInitialActorId();

        // 2. Prüfen, ob eine ID initialisiert wurde.
        if (herstellerId == null || herstellerId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Die Anwendung wurde nicht korrekt mit einer Hersteller-ID initialisiert."));
        }

        try {
            // 3. Die Service-Methode wird mit der globalen ID aufgerufen.
            List<MedikamentResponseDto> medikamente = medicationService.getMedikamenteByHerstellerId(herstellerId);
            return ResponseEntity.ok(medikamente);
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler bei der Abfrage der Medikamente: " + e.getMessage()));
        }
    }

    /**
     * Aktualisiert den Status eines Medikaments, z.B. zur Freigabe durch eine Behörde.
     * Endpunkt: POST /api/v1/medications/{medId}/approval
     *
     * @param medId Die ID des Medikaments.
     * @param request Das DTO mit dem neuen Status.
     * @return Das aktualisierte Medikament als DTO.
     */
    @PostMapping("/{medId}/approval")
    public ResponseEntity<?> approveMedication(
            @PathVariable final String medId,
            @Valid @RequestBody final UpdateMedicationStatusRequestDto request) {

        try {
            MedikamentResponseDto updatedMedikament = medicationService.approveMedication(medId, request.getNewStatus());
            return ResponseEntity.ok(updatedMedikament);
        } catch (Exception e) {
            // Wenn der Chaincode eine Berechtigungs-Exception wirft, wird sie hier abgefangen.
            // Eine spezifischere Fehlerbehandlung könnte hier 403 Forbidden zurückgeben.
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Fehler bei der Statusänderung: " + e.getMessage()));
        }
    }

    /**
     * Sucht nach Medikamenten, deren Bezeichnung einen bestimmten Text enthält.
     * Endpunkt: GET /api/v1/medications?search=Aspirin
     *
     * @param searchQuery Der Text, nach dem in der Bezeichnung gesucht wird.
     * @return Eine Liste von passenden Medikamenten als DTOs.
     */
    @GetMapping("/search")
    public ResponseEntity<List<MedikamentResponseDto>> searchMedications(@RequestParam(name = "search") final String searchQuery) {
        List<MedikamentResponseDto> medikamente = medicationService.searchMedicationsByBezeichnung(searchQuery);
        return ResponseEntity.ok(medikamente);
    }
}
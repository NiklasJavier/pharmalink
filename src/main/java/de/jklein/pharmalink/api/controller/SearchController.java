package de.jklein.pharmalink.api.controller;

import de.jklein.pharmalink.api.dto.ActorResponseDto;
import de.jklein.pharmalink.api.dto.MedikamentResponseDto;
import de.jklein.pharmalink.api.dto.UnitResponseDto;
import de.jklein.pharmalink.api.mapper.ActorMapper;
import de.jklein.pharmalink.api.mapper.MedikamentMapper;
import de.jklein.pharmalink.api.mapper.UnitMapper;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.service.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;
    private final MedikamentMapper medikamentMapper;
    private final UnitMapper unitMapper;
    private final ActorMapper actorMapper;

    public SearchController(SearchService searchService, MedikamentMapper medikamentMapper,
                            UnitMapper unitMapper, ActorMapper actorMapper) {
        this.searchService = searchService;
        this.medikamentMapper = medikamentMapper;
        this.unitMapper = unitMapper;
        this.actorMapper = actorMapper;
    }

    @GetMapping("/actors")
    @Operation(summary = "Nach Akteuren suchen", description = "Sucht nach Akteuren anhand ihrer Rolle und/oder ihres Namens. Alle Parameter sind optional.")
    public ResponseEntity<List<ActorResponseDto>> searchActors(
            @Parameter(description = "Nach einer bestimmten Rolle filtern.") @RequestParam(required = false) String role,
            @Parameter(description = "Suchanfrage für den Namen des Akteurs.") @RequestParam(required = false) String bezeichnung,
            @Parameter(description = "Nach einer bestimmten Akteur-ID filtern.") @RequestParam(required = false) String actorId) {
        var foundActors = searchService.searchActors(role, bezeichnung, actorId);
        return ResponseEntity.ok(actorMapper.toDtoList(foundActors));
    }

    @GetMapping("/medikamente")
    @Operation(summary = "Nach Medikamenten suchen", description = "Sucht nach Medikamenten mit optionalen Filtern für Name/Hersteller, Status, Tags und Besitz.")
    public ResponseEntity<List<MedikamentResponseDto>> searchMedikamente(
            @Parameter(description = "Suchanfrage für den Namen des Medikaments oder Herstellers.")
            @RequestParam(required = false) String query,
            @Parameter(description = "Nach einem bestimmten Medikamentenstatus filtern (z. B. 'angelegt', 'freigegeben').")
            @RequestParam(required = false) String status,
            @Parameter(description = "Nach einer Liste von Tags filtern. Das Medikament muss mindestens einen Tag enthalten.")
            @RequestParam(required = false) List<String> tags,
            @Parameter(description = "Wenn 'true', werden nur Medikamente zurückgegeben, die dem aktuellen Akteur gehören.")
            @RequestParam(required = false, defaultValue = "false") boolean ownedByMe) {

        var foundMedikamente = searchService.searchMedikamente(query, status, tags, ownedByMe);
        return ResponseEntity.ok(medikamentMapper.toDtoList(foundMedikamente));
    }

    @GetMapping("/units")
    @Operation(summary = "Einheiten nach Charge suchen", description = "Sucht innerhalb der eigenen Einheiten des Benutzers nach deren Chargenbezeichnung.")
    public ResponseEntity<List<UnitResponseDto>> searchUnitsByCharge(
            @Parameter(description = "Die Suchanfrage für die Chargenbezeichnung.")
            @RequestParam String query) {
        var foundUnits = searchService.searchUnitsByCharge(query);
        return ResponseEntity.ok(unitMapper.toDtoList(foundUnits));
    }

    @GetMapping("/my-units")
    @Operation(summary = "Eigene zwischengespeicherte Einheiten abrufen", description = "Gibt eine Liste aller Einheiten zurück, die dem aktuellen Benutzer gehören und sich im Cache befinden.")
    public ResponseEntity<List<UnitResponseDto>> getMyCachedUnits() {
        List<Unit> myUnits = searchService.getMyUnits();
        List<UnitResponseDto> myUnitsDto = unitMapper.toDtoList(myUnits);
        return ResponseEntity.ok(myUnitsDto);
    }
}
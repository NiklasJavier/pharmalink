package de.jklein.pharmalink.service;

import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.service.system.SystemStateService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils; // Import hinzugefügt
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SearchService {

    private final SystemStateService systemStateService;

    public SearchService(SystemStateService systemStateService) {
        this.systemStateService = systemStateService;
    }

    /**
     * **ERWEITERT**: Durchsucht Medikamente nach Bezeichnung, Hersteller, Status und Tags.
     * Alle Parameter sind optional.
     *
     * @param query  Der Suchbegriff für Medikamenten- oder Herstellerbezeichnung.
     * @param status Der exakte Status des Medikaments (z.B. "angelegt").
     * @param tags   Eine Liste von Tags; das Medikament muss mindestens einen davon enthalten.
     * @return Eine gefilterte Liste von Medikamenten.
     */
    public List<Medikament> searchMedikamente(String query, String status, List<String> tags) {
        Stream<Medikament> medikamentenStream = systemStateService.getAllMedikamente().stream();

        // --- Filter 1: Nach Status ---
        if (StringUtils.hasText(status)) {
            medikamentenStream = medikamentenStream.filter(m -> status.equalsIgnoreCase(m.getStatus()));
        }

        // --- Filter 2: Nach Tags ---
        // Prüft, ob einer der Such-Tags im Key oder Value der Medikamenten-Tags vorkommt.
        if (!CollectionUtils.isEmpty(tags)) {
            medikamentenStream = medikamentenStream.filter(medikament -> {
                if (medikament.getTags() == null || medikament.getTags().isEmpty()) {
                    return false;
                }
                // Ein Medikament passt, wenn irgendeiner seiner Tags (key oder value)
                // einen der übergebenen Such-Tags enthält (case-insensitive).
                return tags.stream().anyMatch(searchTag ->
                        medikament.getTags().entrySet().stream()
                                .anyMatch(entry ->
                                        entry.getKey().toLowerCase(Locale.ROOT).contains(searchTag.toLowerCase(Locale.ROOT)) ||
                                                entry.getValue().toLowerCase(Locale.ROOT).contains(searchTag.toLowerCase(Locale.ROOT))
                                )
                );
            });
        }

        // --- Filter 3: Nach Freitext-Query (Bezeichnung oder Hersteller) ---
        if (StringUtils.hasText(query)) {
            final String lowercaseQuery = query.toLowerCase(Locale.ROOT);
            List<Actor> allActors = systemStateService.getAllActors();

            Set<String> matchingHerstellerIds = allActors.stream()
                    .filter(actor -> "hersteller".equalsIgnoreCase(actor.getRole()))
                    .filter(actor -> actor.getBezeichnung().toLowerCase(Locale.ROOT).contains(lowercaseQuery))
                    .map(Actor::getActorId)
                    .collect(Collectors.toSet());

            medikamentenStream = medikamentenStream.filter(medikament ->
                    medikament.getBezeichnung().toLowerCase(Locale.ROOT).contains(lowercaseQuery) ||
                            matchingHerstellerIds.contains(medikament.getHerstellerId())
            );
        }

        return medikamentenStream.collect(Collectors.toList());
    }

    // --- Bestehende Methoden (searchActors, searchUnitsByCharge) bleiben unverändert ---

    public List<Actor> searchActors(String role, String bezeichnungQuery) {
        Stream<Actor> actorStream = systemStateService.getAllActors().stream();
        if (StringUtils.hasText(role)) {
            actorStream = actorStream.filter(actor -> role.equalsIgnoreCase(actor.getRole()));
        }
        if (StringUtils.hasText(bezeichnungQuery)) {
            final String lowercaseQuery = bezeichnungQuery.toLowerCase(Locale.ROOT);
            actorStream = actorStream.filter(actor ->
                    actor.getBezeichnung().toLowerCase(Locale.ROOT).contains(lowercaseQuery)
            );
        }
        return actorStream.collect(Collectors.toList());
    }

    public List<Unit> searchUnitsByCharge(String query) {
        final String lowercaseQuery = query.toLowerCase(Locale.ROOT);
        return systemStateService.getMyUnits().stream()
                .filter(unit -> unit.getChargeBezeichnung() != null &&
                        unit.getChargeBezeichnung().toLowerCase(Locale.ROOT).contains(lowercaseQuery))
                .collect(Collectors.toList());
    }
}
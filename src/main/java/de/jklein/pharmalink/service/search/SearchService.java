package de.jklein.pharmalink.service.search;

import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.service.state.SystemStateService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SearchService {

    private final SystemStateService systemStateService;

    public SearchService(SystemStateService systemStateService) {
        this.systemStateService = systemStateService;
    }

    /**
     * Searches medications based on a combination of optional filters.
     *
     * @param query     The search term for medication or manufacturer name.
     * @param status    The exact status of the medication (e.g., "angelegt").
     * @param tags      A list of tags; the medication must contain at least one.
     * @param ownedByMe If true, returns only medications owned by the current actor.
     * @return A filtered list of medications.
     */
    public List<Medikament> searchMedikamente(String query, String status, List<String> tags, boolean ownedByMe) {
        // Start with a stream of all medications from the cache.
        Stream<Medikament> medikamentenStream = systemStateService.getAllMedikamente().stream();

        // --- Filter 1: By owner (manufacturer) ---
        if (ownedByMe) {
            String currentActorId = systemStateService.getCurrentActorId().get();
            if (StringUtils.hasText(currentActorId)) {
                medikamentenStream = medikamentenStream.filter(m -> currentActorId.equals(m.getHerstellerId()));
            } else {
                return List.of(); // No actor set, so they can't own anything.
            }
        }

        // --- Filter 2: By status ---
        if (StringUtils.hasText(status)) {
            medikamentenStream = medikamentenStream.filter(m -> status.equalsIgnoreCase(m.getStatus()));
        }

        // --- Filter 3: By tags ---
        if (!CollectionUtils.isEmpty(tags)) {
            medikamentenStream = medikamentenStream.filter(medikament -> {
                if (medikament.getTags() == null || medikament.getTags().isEmpty()) {
                    return false;
                }
                return tags.stream().anyMatch(searchTag ->
                        medikament.getTags().entrySet().stream()
                                .anyMatch(entry ->
                                        entry.getKey().toLowerCase(Locale.ROOT).contains(searchTag.toLowerCase(Locale.ROOT)) ||
                                                entry.getValue().toLowerCase(Locale.ROOT).contains(searchTag.toLowerCase(Locale.ROOT))
                                )
                );
            });
        }

        // --- Filter 4: By free-text query (medication name OR manufacturer name) ---
        if (StringUtils.hasText(query)) {
            final String lowercaseQuery = query.toLowerCase(Locale.ROOT);

            // Create a map of manufacturer IDs to their lowercase names for efficient lookup.
            Map<String, String> manufacturerNames = systemStateService.getAllActors().stream()
                    .filter(actor -> "hersteller".equalsIgnoreCase(actor.getRole()))
                    .collect(Collectors.toMap(
                            Actor::getActorId,
                            actor -> actor.getBezeichnung().toLowerCase(Locale.ROOT)
                    ));

            medikamentenStream = medikamentenStream.filter(medikament ->
                    // Condition A: Medication name matches the query.
                    medikament.getBezeichnung().toLowerCase(Locale.ROOT).contains(lowercaseQuery) ||
                            // Condition B: The medication's manufacturer name matches the query.
                            manufacturerNames.getOrDefault(medikament.getHerstellerId(), "").contains(lowercaseQuery)
            );
        }

        return medikamentenStream.collect(Collectors.toList());
    }

    // --- searchActors and searchUnitsByCharge remain unchanged ---

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
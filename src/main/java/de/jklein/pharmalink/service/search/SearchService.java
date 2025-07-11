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

    public List<Unit> getMyUnits() {
        return systemStateService.getMyUnits();
    }

    public List<Medikament> searchMedikamente(String query, String status, List<String> tags, boolean ownedByMe) {
        Stream<Medikament> medikamentenStream = systemStateService.getAllMedikamente().stream();

        if (ownedByMe) {
            String currentActorId = systemStateService.getCurrentActorId().get();
            if (StringUtils.hasText(currentActorId)) {
                medikamentenStream = medikamentenStream.filter(m -> currentActorId.equals(m.getHerstellerId()));
            } else {
                return List.of();
            }
        }

        if (StringUtils.hasText(status)) {
            medikamentenStream = medikamentenStream.filter(m -> status.equalsIgnoreCase(m.getStatus()));
        }

        if (!CollectionUtils.isEmpty(tags)) {
            medikamentenStream = medikamentenStream.filter(medikament -> this.medikamentHasMatchingTags(medikament, tags));
        }

        if (StringUtils.hasText(query)) {
            final String lowercaseQuery = query.toLowerCase(Locale.ROOT);
            Map<String, String> manufacturerNames = systemStateService.getAllActors().stream()
                    .filter(actor -> "hersteller".equalsIgnoreCase(actor.getRole()))
                    .collect(Collectors.toMap(
                            Actor::getActorId,
                            actor -> actor.getBezeichnung().toLowerCase(Locale.ROOT)
                    ));

            medikamentenStream = medikamentenStream.filter(medikament ->
                    medikament.getBezeichnung().toLowerCase(Locale.ROOT).contains(lowercaseQuery) ||
                            manufacturerNames.getOrDefault(medikament.getHerstellerId(), "").contains(lowercaseQuery)
            );
        }

        return medikamentenStream.collect(Collectors.toList());
    }

    public List<Actor> searchActors(String role, String bezeichnung, String actorId) {
        Stream<Actor> actorStream = systemStateService.getAllActors().stream();

        if (StringUtils.hasText(role)) {
            actorStream = actorStream.filter(actor -> actor.getRole().equalsIgnoreCase(role));
        }

        if (StringUtils.hasText(bezeichnung)) {
            actorStream = actorStream.filter(actor -> actor.getBezeichnung().toLowerCase().contains(bezeichnung.toLowerCase()));
        }

        if (StringUtils.hasText(actorId)) {
            actorStream = actorStream.filter(actor -> actor.getActorId().equals(actorId));
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

    private boolean medikamentHasMatchingTags(Medikament medikament, List<String> searchTags) {
        if (CollectionUtils.isEmpty(medikament.getTags())) {
            return false;
        }
        List<String> lowerCaseSearchTags = searchTags.stream()
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .toList();

        return medikament.getTags().entrySet().stream()
                .anyMatch(entry -> {
                    String lowerKey = entry.getKey().toLowerCase(Locale.ROOT);
                    String lowerValue = entry.getValue().toLowerCase(Locale.ROOT);
                    return lowerCaseSearchTags.stream()
                            .anyMatch(searchTag -> lowerKey.contains(searchTag) || lowerValue.contains(searchTag));
                });
    }
}
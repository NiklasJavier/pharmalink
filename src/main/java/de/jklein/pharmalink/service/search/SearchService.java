package de.jklein.pharmalink.service.search;

import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.repository.ActorRepository;
import de.jklein.pharmalink.repository.MedikamentRepository;
import de.jklein.pharmalink.repository.UnitRepository;
import de.jklein.pharmalink.service.state.SystemStateService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class SearchService {

    private final SystemStateService systemStateService;
    private final ActorRepository actorRepository;
    private final MedikamentRepository medikamentRepository;
    private final UnitRepository unitRepository;
    private final MongoTemplate mongoTemplate;

    public SearchService(SystemStateService systemStateService, ActorRepository actorRepository,
                         MedikamentRepository medikamentRepository, UnitRepository unitRepository, MongoTemplate mongoTemplate) {
        this.systemStateService = systemStateService;
        this.actorRepository = actorRepository;
        this.medikamentRepository = medikamentRepository;
        this.unitRepository = unitRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public List<Unit> getMyUnits() {
        String currentActorId = systemStateService.getCurrentActorId().get();
        if (!StringUtils.hasText(currentActorId)) {
            return List.of();
        }
        return unitRepository.findByCurrentOwnerActorId(currentActorId);
    }

    public List<Medikament> searchMedikamente(String query, String status, List<String> tags, boolean ownedByMe) {
        Query dbQuery = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (ownedByMe) {
            String currentActorId = systemStateService.getCurrentActorId().get();
            if (StringUtils.hasText(currentActorId)) {
                criteriaList.add(Criteria.where("herstellerId").is(currentActorId));
            } else {
                return List.of(); // Wenn "ownedByMe" aber keine ID, leere Liste zurückgeben
            }
        }

        if (StringUtils.hasText(status)) {
            criteriaList.add(Criteria.where("status").is(status));
        }

        if (!CollectionUtils.isEmpty(tags)) {
            // Sucht nach Übereinstimmungen in den Keys oder Values der Tags Map
            List<Criteria> tagCriteria = tags.stream()
                    .map(tag -> new Criteria().orOperator(
                            Criteria.where("tags." + tag.toLowerCase(Locale.ROOT)).exists(true),
                            Criteria.where("tags").is(tag) // Vereinfachte Suche nach Wert, kann angepasst werden
                    )).toList();
            criteriaList.add(new Criteria().orOperator(tagCriteria));
        }

        if (StringUtils.hasText(query)) {
            final String regexQuery = ".*" + query + ".*";
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("bezeichnung").regex(regexQuery, "i"),
                    Criteria.where("medId").regex(regexQuery, "i")
            ));
        }

        if (!criteriaList.isEmpty()) {
            dbQuery.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        return mongoTemplate.find(dbQuery, Medikament.class);
    }

    public List<Actor> searchActors(String role, String bezeichnung, String actorId) {
        Query dbQuery = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (StringUtils.hasText(role)) {
            criteriaList.add(Criteria.where("role").is(role));
        }

        if (StringUtils.hasText(bezeichnung)) {
            criteriaList.add(Criteria.where("bezeichnung").regex(".*" + bezeichnung + ".*", "i"));
        }

        if (StringUtils.hasText(actorId)) {
            criteriaList.add(Criteria.where("actorId").is(actorId));
        }

        if (!criteriaList.isEmpty()) {
            dbQuery.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        return mongoTemplate.find(dbQuery, Actor.class);
    }

    public List<Unit> searchUnitsByCharge(String query) {
        String currentActorId = systemStateService.getCurrentActorId().get();
        if (!StringUtils.hasText(currentActorId)) {
            return List.of();
        }

        Query dbQuery = new Query();
        dbQuery.addCriteria(Criteria.where("currentOwnerActorId").is(currentActorId));
        dbQuery.addCriteria(Criteria.where("chargeBezeichnung").regex(".*" + query + ".*", "i"));

        return mongoTemplate.find(dbQuery, Unit.class);
    }
}
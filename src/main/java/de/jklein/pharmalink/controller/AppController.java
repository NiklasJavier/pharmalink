package de.jklein.pharmalink.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.repository.ActorRepository;
import de.jklein.pharmalink.repository.MedikamentRepository;
import de.jklein.pharmalink.repository.UnitRepository;
import de.jklein.pharmalink.service.audit.AuditService;
import de.jklein.pharmalink.service.search.SearchService;
import de.jklein.pharmalink.service.state.SystemStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/app")
public class AppController {

    private static final Logger logger = LoggerFactory.getLogger(AppController.class);

    // Benötigte Services und Repositories
    private final SystemStateService systemStateService;
    private final SearchService searchService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final ActorRepository actorRepository;
    private final MedikamentRepository medikamentRepository;
    private final UnitRepository unitRepository;

    public AppController(SystemStateService systemStateService, SearchService searchService,
                         ObjectMapper objectMapper, AuditService auditService,
                         ActorRepository actorRepository, MedikamentRepository medikamentRepository,
                         UnitRepository unitRepository) {
        this.systemStateService = systemStateService;
        this.searchService = searchService;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
        this.actorRepository = actorRepository;
        this.medikamentRepository = medikamentRepository;
        this.unitRepository = unitRepository;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/app/dashboard";
        }
        return "auth/login";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model, RedirectAttributes redirectAttributes) throws IOException {
        String currentActorId = systemStateService.getCurrentActorId().get();
        if (!StringUtils.hasText(currentActorId)) {
            redirectAttributes.addFlashAttribute("error", "Keine Akteur-ID initialisiert.");
            return "redirect:/app/errors/unknown-actor";
        }

        Optional<Actor> actorOpt = actorRepository.findByActorId(currentActorId);

        if (actorOpt.isEmpty()) {
            logger.error("Inkonsistenter Zustand: Akteur mit ID {} nicht in der Datenbank gefunden!", currentActorId);
            redirectAttributes.addFlashAttribute("error", "Ihre Akteur-ID " + currentActorId + " konnte nicht gefunden werden.");
            return "redirect:/app/errors/unknown-actor";
        }

        Actor currentActor = actorOpt.get();
        model.addAttribute("currentActorInfo", currentActor);
        model.addAttribute("pageTitle", "Dashboard");

        model.addAttribute("apiTransactionsJson", auditService.getAllApiTransactionsAsJson());
        model.addAttribute("grpcTransactionsJson", auditService.getAllGrpcTransactionsAsJson());
        model.addAttribute("loginAttemptsJson", auditService.getAllLoginAttemptsAsJson());

        List<Unit> myUnits = unitRepository.findByCurrentOwnerActorId(currentActorId);
        String getMyUnitsJson = removeKeysFromJsonNode(objectMapper.writeValueAsString(myUnits), Set.of(""));
        model.addAttribute("getMyUnits", getMyUnitsJson);

        List<Actor> allActors = actorRepository.findAll();
        String getAllActorsJson = removeKeysFromJsonNode(objectMapper.writeValueAsString(allActors), Set.of("actorId", "ipfsLink", "docType"));
        model.addAttribute("getAllActors", getAllActorsJson);

        List<Medikament> allMedikamente = medikamentRepository.findAll();
        String getAllMedikamenteJson = removeKeysFromJsonNode(objectMapper.writeValueAsString(allMedikamente), Set.of("medId", "herstellerId", "infoblattHash", "approvedById", "ipfsLink", "docType", "ipfsData"));
        model.addAttribute("getAllMedikamente", getAllMedikamenteJson);

        try {
            Map<String, Object> kpiData = new LinkedHashMap<>();
            List<?> mainTableData;
            String tableTitle = "";

            switch (currentActor.getRole().toLowerCase()) {
                case "hersteller":
                    mainTableData = medikamentRepository.findByHerstellerId(currentActorId);
                    tableTitle = "Meine Medikamente";
                    kpiData.put("Eigene Medikamente", mainTableData.size());
                    kpiData.put("Meine Einheiten", unitRepository.findByCurrentOwnerActorId(currentActorId).size());
                    kpiData.put("Alle Grosshändler", searchService.searchActors("grosshaendler", null, null).size());
                    break;
                case "grosshaendler":
                case "apotheke":
                    mainTableData = unitRepository.findByCurrentOwnerActorId(currentActorId);
                    tableTitle = "Mein Inventar (Einheiten)";
                    kpiData.put("Besessene Einheiten", mainTableData.size());
                    kpiData.put("Alle Apotheken", searchService.searchActors("apotheke", null, null).size());
                    kpiData.put("Alle Großhändler", searchService.searchActors("grosshaendler", null, null).size());
                    break;
                case "behoerde":
                    mainTableData = medikamentRepository.findAll();
                    tableTitle = "Alle Medikamente im System";
                    kpiData.put("Registrierte Medikamente", mainTableData.size());
                    kpiData.put("Registrierte Akteure", actorRepository.count());
                    kpiData.put("Alle Einheiten im Umlauf", unitRepository.count());
                    break;
                default:
                    mainTableData = List.of();
                    tableTitle = "Keine Daten für Ihre Rolle";
                    kpiData.put("Info", "Keine spezifische Ansicht für Ihre Rolle.");
                    break;
            }

            model.addAttribute("kpiDataJson", objectMapper.writeValueAsString(kpiData));
            model.addAttribute("mainTableDataJson", objectMapper.writeValueAsString(mainTableData));
            model.addAttribute("mainTableTitle", tableTitle);

        } catch (JsonProcessingException e) {
            logger.error("Fehler bei der Serialisierung der Dashboard-Daten für Akteur {}: {}", currentActorId, e.getMessage(), e);
            model.addAttribute("dashboardError", "Fehler bei der Aufbereitung der Ansicht.");
        } catch (Exception e) {
            logger.error("Ein unerwarteter Fehler ist beim Laden des Dashboards aufgetreten für Akteur {}: {}", currentActorId, e.getMessage(), e);
            model.addAttribute("dashboardError", "Ein unerwarteter Fehler ist aufgetreten.");
        }

        return "dashboard/overview";
    }

    @GetMapping("/errors/unknown-actor")
    public String showUnknownActorErrorPage() {
        return "errors/unknown-actor";
    }

    // Die Helper-Methoden zum Entfernen von JSON-Keys bleiben unverändert
    private String removeKeysFromJsonNode(String jsonString, Set<String> keysToRemove) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonString);
        JsonNode filteredNode = removeKeysFromJsonNode(rootNode, keysToRemove);
        return objectMapper.writeValueAsString(filteredNode);
    }

    private JsonNode removeKeysFromJsonNode(JsonNode node, Set<String> keysToRemove) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);

            for (String fieldName : fieldNames) {
                if (keysToRemove.contains(fieldName)) {
                    objectNode.remove(fieldName);
                } else {
                    JsonNode childNode = objectNode.get(fieldName);
                    if (childNode != null) {
                        objectNode.set(fieldName, removeKeysFromJsonNode(childNode, keysToRemove));
                    }
                }
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                arrayNode.set(i, removeKeysFromJsonNode(arrayNode.get(i), keysToRemove));
            }
        }
        return node;
    }
}
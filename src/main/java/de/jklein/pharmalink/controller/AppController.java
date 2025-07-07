package de.jklein.pharmalink.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.service.audit.AuditService;
import de.jklein.pharmalink.service.search.SearchService;
import de.jklein.pharmalink.service.system.SystemStateService;
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

    private final SystemStateService systemStateService;
    private final SearchService searchService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    // Der ActorService wird nicht mehr benötigt, da wir die Daten aus dem SystemState beziehen
    public AppController(SystemStateService systemStateService, SearchService searchService, ObjectMapper objectMapper, AuditService auditService) {
        this.systemStateService = systemStateService;
        this.searchService = searchService;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/app/dashboard";
        }
        return "auth/login";
    }

    /**
     * **ANGEPASST**: Ruft die Informationen des aktuellen Akteurs direkt aus dem
     * In-Memory-Cache des SystemStateService ab.
     */
    @GetMapping("/dashboard")
    public String showDashboard(Model model, RedirectAttributes redirectAttributes) throws IOException {
        String currentActorId = systemStateService.getCurrentActorId().get();
        if (!StringUtils.hasText(currentActorId)) {
            redirectAttributes.addFlashAttribute("error", "Anwendungsfehler: Keine Akteur-ID initialisiert.");
            return "redirect:/app/errors/unknown-actor";
        }

        // Finde den aktuellen Akteur direkt in der gecachten Liste
        Optional<Actor> actorOpt = systemStateService.getAllActors().stream()
                .filter(a -> a.getActorId().equals(currentActorId))
                .findFirst();

        if (actorOpt.isEmpty()) {
            // Dieser Fehler deutet auf einen inkonsistenten Cache hin.
            logger.error("Inkonsistenter Zustand: Akteur mit ID {} nicht im Cache gefunden!", currentActorId);
            redirectAttributes.addFlashAttribute("error", "Ihre Akteur-ID " + currentActorId + " konnte nicht im Anwendungs-Cache gefunden werden.");
            return "redirect:/app/errors/unknown-actor";
        }

        Actor currentActor = actorOpt.get();
        model.addAttribute("currentActorInfo", currentActor);
        model.addAttribute("pageTitle", "Dashboard");

        String apiTransactionsJson = auditService.getAllApiTransactionsAsJson();
        model.addAttribute("apiTransactionsJson", apiTransactionsJson);

        String grpcTransactionsJson = auditService.getAllGrpcTransactionsAsJson();
        model.addAttribute("grpcTransactionsJson", grpcTransactionsJson);

        String loginAttemptsJson = auditService.getAllLoginAttemptsAsJson();
        model.addAttribute("loginAttemptsJson", loginAttemptsJson);

        String getMyUnits = removeKeysFromJsonNode(systemStateService.getMyUnitsAsJsonString(), Set.of(""));
        model.addAttribute("getMyUnits", getMyUnits);

        String getAllActors = removeKeysFromJsonNode(systemStateService.getAllActorsAsJsonString(), Set.of("actorId", "ipfsLink", "docType"));
        model.addAttribute("getAllActors", getAllActors);

        String getAllMedikamente = removeKeysFromJsonNode(systemStateService.getAllMedikamenteAsJsonString(), Set.of("medId", "herstellerId", "infoblattHash", "approvedById", "ipfsLink", "docType", "ipfsData"));
        model.addAttribute("getAllMedikamente", getAllMedikamente);

        try {
            Map<String, Object> kpiData = new LinkedHashMap<>();
            List<?> mainTableData;
            String tableTitle = "";

            switch (currentActor.getRole().toLowerCase()) {
                case "hersteller":
                    mainTableData = searchService.searchMedikamente(null, null, null, true);
                    tableTitle = "Meine Medikamente";
                    kpiData.put("Eigene Medikamente", mainTableData.size());
                    kpiData.put("Meine Einheiten", systemStateService.getMyUnits().size());
                    kpiData.put("Alle Grosshändler", searchService.searchActors("grosshaendler", null).size());
                    break;
                case "grosshaendler":
                case "apotheke":
                    mainTableData = systemStateService.getMyUnits();
                    tableTitle = "Mein Inventar (Einheiten)";
                    kpiData.put("Besessene Einheiten", mainTableData.size());
                    kpiData.put("Alle Apotheken", searchService.searchActors("apotheke", null).size());
                    kpiData.put("Alle Großhändler", searchService.searchActors("grosshaendler", null).size());
                    break;
                case "behoerde":
                    mainTableData = systemStateService.getAllMedikamente();
                    tableTitle = "Alle Medikamente im System";
                    kpiData.put("Registrierte Medikamente", mainTableData.size());
                    kpiData.put("Registrierte Akteure", systemStateService.getAllActors().size());
                    kpiData.put("Alle Einheiten im Umlauf", "N/A"); // Diese Info ist nicht im Cache
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

    private String removeKeysFromJsonNode(String jsonString, Set<String> keysToRemove) throws JsonProcessingException, IOException {
        JsonNode rootNode = objectMapper.readTree(jsonString); // String zu JsonNode parsen
        JsonNode filteredNode = removeKeysFromJsonNode(rootNode, keysToRemove); // Bestehende Methode aufrufen
        return objectMapper.writeValueAsString(filteredNode); // JsonNode zurück zu String serialisieren
    }

    private JsonNode removeKeysFromJsonNode(JsonNode node, Set<String> keysToRemove) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            // Erstellen Sie eine Kopie der Feldnamen, um ConcurrentModificationException zu vermeiden
            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);

            for (String fieldName : fieldNames) {
                if (keysToRemove.contains(fieldName)) {
                    objectNode.remove(fieldName);
                } else {
                    // Rekursiv für verschachtelte Objekte oder Arrays
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
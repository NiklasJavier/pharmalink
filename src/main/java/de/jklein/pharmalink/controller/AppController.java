package de.jklein.pharmalink.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.service.ActorService;
import de.jklein.pharmalink.service.MedicationService;
import de.jklein.pharmalink.service.UnitService;
import de.jklein.pharmalink.service.audit.AuditService;
import de.jklein.pharmalink.service.system.SystemStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/app")
public class AppController {

    private static final Logger logger = LoggerFactory.getLogger(AppController.class);

    private final SystemStateService systemStateService;
    private final ActorService actorService;
    private final MedicationService medicationService;
    private final UnitService unitService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public AppController(SystemStateService systemStateService, ActorService actorService,
                         MedicationService medicationService, UnitService unitService, ObjectMapper objectMapper, AuditService auditService) {
        this.systemStateService = systemStateService;
        this.actorService = actorService;
        this.medicationService = medicationService;
        this.unitService = unitService;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @ModelAttribute
    public void addCommonAttributes(Model model) {
        model.addAttribute("backendVersion", "1.0.0");
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/app/dashboard";
        }
        return "auth/login";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model, RedirectAttributes redirectAttributes) {
        String initialActorId = systemStateService.getCurrentSystemState().getCurrentActorId();
        if (initialActorId == null || initialActorId.isBlank()) {
            logger.warn("Initial Actor ID not found or is blank. Redirecting to unknown-actor error page.");
            redirectAttributes.addFlashAttribute("error", "Ihre Actor ID konnte nicht geladen werden oder ist ungültig. Bitte stellen Sie sicher, dass die Anwendung korrekt initialisiert ist.");
            return "redirect:/app/errors/unknown-actor";
        }

        Optional<Actor> actorOpt = actorService.getEnrichedActorById(initialActorId);

        if (actorOpt.isEmpty()) {
            logger.warn("Actor with ID {} not found or enrichment failed. Redirecting to unknown-actor error page.", initialActorId);
            redirectAttributes.addFlashAttribute("error", "Unbekannter Akteurstyp für ID: " + initialActorId + ". Akteur nicht in der Blockchain gefunden oder nicht registriert.");
            return "redirect:/app/errors/unknown-actor";
        }

        Actor currentActor = actorOpt.get();
        model.addAttribute("initialActorId", initialActorId);
        model.addAttribute("currentActorInfo", currentActor);
        model.addAttribute("pageTitle", "Dashboard");

        String apiTransactionsJson = auditService.getAllApiTransactionsAsJson(); // Rufe den JSON-String ab
        model.addAttribute("apiTransactionsJson", apiTransactionsJson); // Zum Model hinzufügen

        String grpcTransactionsJson = auditService.getAllGrpcTransactionsAsJson(); // Rufe den JSON-String ab
        model.addAttribute("grpcTransactionsJson", grpcTransactionsJson); // Zum Model hinzufügen

        String loginAttemptsJson = auditService.getAllLoginAttemptsAsJson(); // Rufe den JSON-String ab
        model.addAttribute("loginAttemptsJson", loginAttemptsJson);

        try {
            List<Actor> allActors = systemStateService.getAllActors();
            List<Medikament> allMedikamente = systemStateService.getAllMedikamente();
            List<Unit> myUnits = systemStateService.getMyUnits();

            model.addAttribute("allActors", allActors);
            model.addAttribute("allMedikamente", allMedikamente);
            model.addAttribute("myUnits", myUnits);

            switch (currentActor.getRole().toLowerCase()) {
                case "hersteller":
                    List<Medikament> herstellerMedikamente = medicationService.getMedikamenteByHerstellerId(initialActorId);
                    List<String> medikamenteAsJsonList = herstellerMedikamente.stream()
                            .map(med -> {
                                try {
                                    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(med);
                                } catch (Exception e) {
                                    logger.error("Fehler beim Umwandeln von Medikament {} in JSON für Hersteller {}: {}", med.getMedId(), initialActorId, e.getMessage(), e);
                                    return "{\"error\": \"Konnte Medikament nicht in JSON umwandeln: " + e.getMessage() + "\"}";
                                }
                            })
                            .collect(Collectors.toList());
                    model.addAttribute("medikamenteAsJsonList", medikamenteAsJsonList);
                    model.addAttribute("roleSpecificMessage", "Als Hersteller sehen Sie hier eine Übersicht Ihrer Medikamente und deren Einheiten.");
                    break;
                case "grosshaendler":
                case "apotheke":
                    model.addAttribute("roleSpecificMessage", "Als " + currentActor.getRole() + " sehen Sie hier die Ihnen zugeordneten Einheiten und den Verlauf.");
                    break;
                case "behoerde":
                    model.addAttribute("roleSpecificMessage", "Als Behörde haben Sie hier einen Überblick über alle Akteure und Medikamente im System.");
                    break;
                default:
                    model.addAttribute("roleSpecificMessage", "Ihre Rolle ist nicht spezifisch zugeordnet. Allgemeine Informationen werden angezeigt.");
                    break;
            }
        } catch (Exception e) {
            logger.error("Fehler beim Laden der Dashboard-Daten für Akteur ID {}: {}", initialActorId, e.getMessage(), e);
            model.addAttribute("dashboardError", "Fehler beim Laden der Dashboard-Daten: " + e.getMessage());
        }
        return "dashboard/overview";
    }

    @GetMapping("/manage")
    public String showManagePage(Model model) {
        model.addAttribute("pageTitle", "Manage");

        model.addAttribute("apiResponseData", Map.of(
                "name", "PharmaMed", "version", 1.0, "active", true,
                "details", Map.of("manufacturer", "ABC Pharma", "location", "Berlin"),
                "tags", List.of("pharma", "medtech", "blockchain"), "price", 123.45
        ));
        model.addAttribute("arrayOfData", List.of(
                Map.of("id", 1, "item", "A"),
                Map.of("id", 2, "item", "B")
        ));

        return "manage/overview";
    }
    @GetMapping("/errors/unknown-actor")
    public String showUnknownActorErrorPage() {
        return "errors/unknown-actor";
    }
}
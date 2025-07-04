package de.jklein.pharmalink.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jklein.pharmalink.api.dto.ActorResponseDto;
import de.jklein.pharmalink.service.ActorService;
import de.jklein.pharmalink.service.MedicationService;
import de.jklein.pharmalink.service.UnitService;
import de.jklein.pharmalink.service.system.SystemStateService;
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

@Controller
@RequestMapping("/app")
public class AppController {

    private final SystemStateService systemStateService;
    private final ActorService actorService;
    private final MedicationService medicationService;
    private final UnitService unitService;
    private final ObjectMapper objectMapper;

    public AppController(SystemStateService systemStateService, ActorService actorService,
                         MedicationService medicationService, UnitService unitService, ObjectMapper objectMapper) {
        this.systemStateService = systemStateService;
        this.actorService = actorService;
        this.medicationService = medicationService;
        this.unitService = unitService;
        this.objectMapper = objectMapper;
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

// In src/main/java/de/jklein/pharmalink/controller/AppController.java

    @GetMapping("/dashboard")
    public String showDashboard(Model model, RedirectAttributes redirectAttributes) {
        String initialActorId = systemStateService.getInitialActorId();
        if (initialActorId == null) {
            redirectAttributes.addFlashAttribute("error", "Ihre Actor ID konnte nicht geladen werden.");
            return "redirect:/app/errors/unknown-actor";
        }

        model.addAttribute("initialActorId", initialActorId);

        Optional<ActorResponseDto> actorOpt = actorService.getEnrichedActorById(initialActorId);

        if (actorOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Unbekannter Akteurstyp für ID: " + initialActorId);
            return "redirect:/app/errors/unknown-actor";
        }

        ActorResponseDto currentActor = actorOpt.get();
        model.addAttribute("currentActorInfo", currentActor);
        model.addAttribute("pageTitle", "Dashboard");

        try {
            if ("hersteller".equals(currentActor.getRolle())) {
                // Die Liste der Medikamente abrufen
                var medikamente = medicationService.getMedikamenteByHerstellerId(initialActorId);
                // NEU: Die Liste in einen formatierten JSON-String umwandeln
                String medikamenteAsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(medikamente);
                // Den JSON-String an das Model übergeben
                model.addAttribute("medicationsAsJson", medikamenteAsJson);

            } else if ("grosshaendler".equals(currentActor.getRolle()) || "apotheke".equals(currentActor.getRolle())) {
                model.addAttribute("units", unitService.getUnitsByOwner(initialActorId));
            } else if ("behoerde".equals(currentActor.getRolle())) {
                model.addAttribute("allActors", actorService.getActorsByRole("hersteller"));
            }
        } catch (Exception e) {
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
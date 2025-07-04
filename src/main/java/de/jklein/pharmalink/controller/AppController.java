package de.jklein.pharmalink.controller;

import de.jklein.pharmalink.service.system.SystemStateService;
import de.jklein.pharmalink.service.ActorService; // Import des ActorService
import de.jklein.pharmalink.service.MedicationService; // Import des MedicationService
import de.jklein.pharmalink.api.dto.MedikamentResponseDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/app")
public class AppController {

    private final SystemStateService systemStateService;
    private final ActorService actorService; // Injizieren des ActorService
    private final MedicationService medicationService; // Injizieren des MedicationService

    private final ObjectMapper objectMapper = new ObjectMapper(); // Für JSON-Display im Manage-Controller, falls aktiv

    @Autowired
    public AppController(SystemStateService systemStateService, ActorService actorService, MedicationService medicationService) {
        this.systemStateService = systemStateService;
        this.actorService = actorService; // Initialisieren des ActorService
        this.medicationService = medicationService; // Initialisieren des MedicationService
    }

    // Hilfsmethode, um globale Model-Attribute hinzuzufügen
    private void addGlobalAttributes(Model model) {
        String backendVersion = "1.0.0"; // Ersetzen Sie dies durch Ihre tatsächliche Versionslogik
        model.addAttribute("backendVersion", backendVersion);
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        addGlobalAttributes(model);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
                !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/app/dashboard";
        }

        if (model.containsAttribute("error")) {
            model.addAttribute("error", true);
        }
        if (model.containsAttribute("logout")) {
            model.addAttribute("logout", true);
        }

        return "auth/login";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model, RedirectAttributes redirectAttributes) {
        addGlobalAttributes(model);

        String initialActorId = systemStateService.getInitialActorId();

        if (initialActorId == null) {
            redirectAttributes.addFlashAttribute("error", "Ihre Actor ID konnte nicht geladen werden.");
            return "redirect:/app/errors/unknown-actor";
        }

        boolean isKnownActorType = initialActorId.startsWith("hersteller-") ||
                initialActorId.startsWith("grosshaendler-") ||
                initialActorId.startsWith("apotheke-") ||
                initialActorId.startsWith("behoerde-");

        if (!isKnownActorType) {
            redirectAttributes.addFlashAttribute("error", "Unbekannter Akteurstyp für ID: " + initialActorId);
            redirectAttributes.addFlashAttribute("initialActorId", initialActorId);
            return "redirect:/app/errors/unknown-actor";
        }

        model.addAttribute("initialActorId", initialActorId); // Dies ist Ihre Actor ID

        // NEU: API-Aufruf, um Medikamente des Herstellers zu erhalten, direkt über den MedicationService
        try {
            // Rufen Sie die Medikamente direkt über den Service ab, nicht über den Controller
            List<MedikamentResponseDto> medications = medicationService.getMedikamenteByHerstellerId(initialActorId);
            model.addAttribute("medicationsByHersteller", medications);
        } catch (Exception e) {
            model.addAttribute("medicationsError", "Fehler beim Abrufen der Medikamente: " + e.getMessage());
            model.addAttribute("medicationsByHersteller", Collections.emptyList()); // Leere Liste bei Fehler
        }

        // NEU: Aktuelle Actor-Informationen abrufen (Optional, falls auf Dashboard benötigt)
        try {
            actorService.getEnrichedActorById(initialActorId).ifPresent(actorDto -> {
                model.addAttribute("currentActorInfo", actorDto); // Fügt das angereicherte Actor DTO hinzu
            });
        } catch (Exception e) {
            model.addAttribute("actorInfoError", "Fehler beim Abrufen der Akteur-Informationen: " + e.getMessage());
        }


        model.addAttribute("pageTitle", "Dashboard");
        return "dashboard/overview";
    }

    @GetMapping("/manage")
    public String showManagePage(Model model) {
        addGlobalAttributes(model);
        model.addAttribute("pageTitle", "Manage");
        // HINWEIS: JSON-Beispieldaten für den json-display-fragment werden hier weiterhin hinzugefügt,
        // da dies zuvor als Teil der "festen" Manage-Seite definiert wurde.
        String jsonString = "{\"name\":\"PharmaMed\", \"version\":1.0, \"active\":true, " +
                "\"details\":{\"manufacturer\":\"ABC Pharma\", \"location\":\"Berlin\"}, " +
                "\"tags\":[\"pharma\", \"medtech\", \"blockchain\"], \"price\":123.45}";
        try {
            Map<String, Object> jsonData = objectMapper.readValue(jsonString, Map.class);
            model.addAttribute("apiResponseData", jsonData);
        } catch (Exception e) {
            model.addAttribute("jsonError", "Fehler beim Parsen der JSON-Daten: " + e.getMessage());
        }
        String jsonArrayString = "[{\"id\":1, \"item\":\"A\"}, {\"id\":2, \"item\":\"B\"}]";
        try {
            List<Map<String, Object>> jsonArrayData = objectMapper.readValue(jsonArrayString, List.class);
            model.addAttribute("arrayOfData", jsonArrayData);
        } catch (Exception e) {
            model.addAttribute("jsonArrayError", "Fehler beim Parsen der JSON-Array-Daten: " + e.getMessage());
        }

        return "manage/overview";
    }

    @GetMapping("/errors/unknown-actor")
    public String showUnknownActorErrorPage(Model model) {
        addGlobalAttributes(model);
        return "errors/unknown-actor";
    }
}
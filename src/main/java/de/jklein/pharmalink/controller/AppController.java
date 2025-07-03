package de.jklein.pharmalink.controller;

import de.jklein.pharmalink.service.system.SystemStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app")
public class AppController {

    private final SystemStateService systemStateService;

    @Autowired
    public AppController(SystemStateService systemStateService) {
        this.systemStateService = systemStateService;
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

        model.addAttribute("initialActorId", initialActorId);
        model.addAttribute("pageTitle", "Dashboard");
        return "dashboard/overview";
    }

    @GetMapping("/manage")
    public String showManagePage(Model model) {
        addGlobalAttributes(model);
        model.addAttribute("pageTitle", "Manage");
        return "manage/overview";
    }

    @GetMapping("/errors/unknown-actor")
    public String showUnknownActorErrorPage(Model model) {
        addGlobalAttributes(model);
        return "errors/unknown-actor";
    }
}
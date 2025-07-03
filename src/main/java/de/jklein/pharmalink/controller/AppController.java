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

    @GetMapping("/login")
    public String showLoginForm(Model model) {
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
        return "dashboard/overview";
    }

    @GetMapping("/errors/unknown-actor")
    public String showUnknownActorErrorPage(Model model) {
        return "errors/unknown-actor";
    }
}
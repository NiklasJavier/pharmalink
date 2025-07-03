package de.jklein.pharmalink.controller;

import de.jklein.pharmalink.service.system.SystemStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication; // Import Authentication
import org.springframework.security.core.context.SecurityContextHolder; // Import SecurityContextHolder
import org.springframework.security.authentication.AnonymousAuthenticationToken; // Import AnonymousAuthenticationToken
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

        // If the user is already authenticated (and not anonymous), redirect to dashboard
        if (authentication != null && authentication.isAuthenticated() &&
                !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/app/dashboard";
        }

        // Add error/logout messages from query parameters if present
        if (model.containsAttribute("error")) {
            model.addAttribute("error", true);
        }
        if (model.containsAttribute("logout")) {
            model.addAttribute("logout", true);
        }

        // Return the login page for unauthenticated users
        return "auth/login"; // Verweist auf src/main/resources/web/auth/login.html
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model, RedirectAttributes redirectAttributes) {
        String initialActorId = systemStateService.getInitialActorId();

        if (initialActorId == null) {
            redirectAttributes.addFlashAttribute("error", "Ihre Actor ID konnte nicht geladen werden.");
            return "redirect:/app/errors/unknown-actor"; // Corrected redirect path
        }

        boolean isKnownActorType = initialActorId.startsWith("hersteller-") ||
                initialActorId.startsWith("grosshaendler-") ||
                initialActorId.startsWith("apotheke-") ||
                initialActorId.startsWith("behoerde-");

        if (!isKnownActorType) {
            redirectAttributes.addFlashAttribute("error", "Unbekannter Akteurstyp für ID: " + initialActorId);
            redirectAttributes.addFlashAttribute("initialActorId", initialActorId); // Pass ID as flash attribute
            return "redirect:/app/errors/unknown-actor"; // Corrected redirect path
        }

        model.addAttribute("initialActorId", initialActorId);
        return "dashboard/overview"; // Verweist auf src/main/resources/web/dashboard/overview.html
    }

    @GetMapping("/errors/unknown-actor")
    public String showUnknownActorErrorPage(Model model) {
        // Flash attributes are automatically added to the model
        // No explicit retrieval needed, just ensure your template uses them
        return "errors/unknown-actor"; // Zeigt die Fehlerseite an
    }
}
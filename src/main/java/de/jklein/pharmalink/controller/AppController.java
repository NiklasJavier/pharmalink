package de.jklein.pharmalink.controller;

import de.jklein.pharmalink.service.system.SystemStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // Für Flash Attributes

@Controller
@RequestMapping("/app")
public class AppController {

    private final SystemStateService systemStateService;

    @Autowired
    public AppController(SystemStateService systemStateService) {
        this.systemStateService = systemStateService;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login"; // Verweist auf src/main/resources/templates/auth/login.html
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model, RedirectAttributes redirectAttributes) {
        String initialActorId = systemStateService.getInitialActorId();

        if (initialActorId == null) {
            // Wenn keine Actor ID verfügbar ist (z.B. Initialisierungsproblem)
            redirectAttributes.addFlashAttribute("error", "Ihre Actor ID konnte nicht geladen werden.");
            return "redirect:/errors/unknown-actor"; // Umleitung zur Fehlerseite
        }

        // Überprüfen Sie das Präfix der Actor ID
        boolean isKnownActorType = initialActorId.startsWith("hersteller-") ||
                initialActorId.startsWith("grosshaendler-") ||
                initialActorId.startsWith("apotheke-") ||
                initialActorId.startsWith("behoerde-");

        if (!isKnownActorType) {
            // Wenn der Akteurstyp unbekannt ist, leiten Sie zur Fehlerseite um
            redirectAttributes.addFlashAttribute("error", "Unbekannter Akteurstyp für ID: " + initialActorId);
            model.addAttribute("initialActorId", initialActorId); // ID an die Fehlerseite übergeben
            return "redirect:/errors/unknown-actor"; // Umleitung zur Fehlerseite
        }

        // Wenn alles in Ordnung ist, fügen Sie die ID dem Modell hinzu und zeigen Sie das Dashboard an
        model.addAttribute("initialActorId", initialActorId);
        return "dashboard/overview"; // Verweist auf src/main/resources/templates/dashboard/overview.html
    }

    // Neuer Endpunkt für die Fehlerseite
    @GetMapping("/errors/unknown-actor")
    public String showUnknownActorErrorPage(Model model) {
        // Hier können Flash-Attribute von der Umleitung empfangen werden
        if (!model.containsAttribute("initialActorId")) {
            // Fallback, falls initialActorId nicht als Flash-Attribut übergeben wurde
            model.addAttribute("initialActorId", "Nicht verfügbar");
        }
        return "errors/unknown-actor"; // Zeigt die Fehlerseite an
    }

    @GetMapping("/about")
    public String showAboutPage() {
        return "about";
    }
}
package de.jklein.pharmalink.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller für die Root-URL der Anwendung.
 * Leitet unauthentifizierte Benutzer zur Login-Seite um.
 */
@Controller
public class RootController {

    @GetMapping("/")
    public String redirectToDashboard() {
        // Leitet auf das Thymeleaf-Template "index.html" um
        // Alternativ: return "redirect:/dashboard"; wenn du eine spezielle Dashboard-URL hast
        return "index"; // Verweist auf src/main/resources/web/index.html
    }
}
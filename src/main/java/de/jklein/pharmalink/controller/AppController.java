package de.jklein.pharmalink.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // Benötigt, um Daten an das Template zu übergeben
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam; // Optional: Zum Empfangen von URL-Parametern wie 'error' oder 'logout'

/**
 * Controller für anwendungsweite UI-Seiten wie Login und Dashboard.
 * Diese Controller-Methoden geben Thymeleaf-Templates zurück.
 */
@Controller
@RequestMapping("/app") // Alle Mappings in diesem Controller beginnen mit /app
public class AppController {

    /**
     * Zeigt die Login-Seite der Anwendung an.
     * Spring Security leitet Benutzer hierher um, wenn Authentifizierung erforderlich ist.
     * Es können optionale Parameter wie 'error' oder 'logout' vorhanden sein,
     * die in der URL von Spring Security hinzugefügt werden.
     *
     * @param error Optionaler Parameter, der von Spring Security bei fehlgeschlagenem Login gesetzt wird.
     * @param logout Optionaler Parameter, der von Spring Security bei erfolgreichem Logout gesetzt wird.
     * @param model Das Model-Objekt, um Attribute an das Thymeleaf-Template zu übergeben.
     * @return Der Name des Thymeleaf-Templates für die Login-Seite ("login.html").
     */
    @GetMapping("/login")
    public String showLogin(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("loginError", true); // Fügt ein Attribut hinzu, um Fehlermeldungen im Template anzuzeigen
        }
        if (logout != null) {
            model.addAttribute("logoutSuccess", true); // Fügt ein Attribut hinzu, um Logout-Erfolgsmeldungen anzuzeigen
        }

        // Gibt den Namen des Thymeleaf-Templates zurück.
        // Aufgrund der Konfiguration in application.yaml (prefix: classpath:/templates/, suffix: .html)
        // sucht Spring nach src/main/resources/templates/login.html.
        return "login";
    }

    /**
     * Zeigt das Haupt-Dashboard der Anwendung an.
     * Diese Seite ist durch Spring Security geschützt und nur für authentifizierte Benutzer zugänglich.
     *
     * @param model Das Model-Objekt, um Attribute an das Thymeleaf-Template zu übergeben (falls benötigt).
     * @return Der Name des Thymeleaf-Templates für das Dashboard ("dashboard.html").
     */
    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        // Hier könnten Daten vom Backend geladen und dem Model hinzugefügt werden,
        // um sie im dashboard.html anzuzeigen.
        // Beispiel: model.addAttribute("welcomeMessage", "Willkommen im Dashboard!");

        // Gibt den Namen des Thymeleaf-Templates zurück.
        // Sucht nach src/main/resources/templates/dashboard.html.
        return "dashboard";
    }

    // Weitere @GetMapping oder @PostMapping Methoden für andere UI-Seiten hier
    // z.B. eine Seite für Medikamentenverwaltung, Benutzerprofil etc.
}
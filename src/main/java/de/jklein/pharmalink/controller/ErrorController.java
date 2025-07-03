package de.jklein.pharmalink.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus; // Importieren Sie HttpStatus
// import de.jklein.pharmalink.constants.ErrorMessages; // Behalten Sie dies, wenn Sie showSpecificErrorPage nutzen

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    // Hilfsmethode, um globale Model-Attribute hinzuzufügen
    private void addGlobalAttributesForErrorPage(Model model) {
        String backendVersion = "1.0.0"; // Ersetzen Sie dies durch Ihre tatsächliche Versionslogik
        model.addAttribute("backendVersion", backendVersion);
    }

    @GetMapping("/error") // Standard-Mapping für Spring Boot Fehler
    public String handleError(HttpServletRequest request, Model model) {
        addGlobalAttributesForErrorPage(model); // Globale Attribute für Layout

        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object errorType = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        // Standardattribute zum Model hinzufügen
        String statusString = (status != null) ? status.toString() : "Unbekannt";
        model.addAttribute("status", statusString);
        model.addAttribute("error", (errorType != null) ? errorType.toString() : "Unbekannter Fehler");
        model.addAttribute("message", (message != null) ? message.toString() : "Ein unerwarteter Fehler ist aufgetreten.");
        model.addAttribute("path", (path != null) ? path.toString() : "Unbekannter Pfad");
        model.addAttribute("timestamp", new java.util.Date());

        // Exception-Details für 500er Fehler
        if (exception instanceof Throwable) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            ((Throwable) exception).printStackTrace(pw);
            model.addAttribute("exceptionStackTrace", sw.toString()); // Stacktrace
            model.addAttribute("exceptionMessage", ((Throwable) exception).getMessage()); // Exception-Nachricht
        }

        // --- NEUE LOGIK FÜR UNKNOWN-ACTOR vs. GENERISCHE FEHLER ---
        // Prüfen, ob ein spezifisches "flashError" Attribut gesetzt wurde,
        // das auf den "unknown actor" Fall hinweist.
        // Dies sollte von Ihrem AppController kommen, wenn er den Fehler setzt.
        if (model.containsAttribute("flashErrorForUnknownActor")) {
            model.addAttribute("errorMessage", model.getAttribute("flashErrorForUnknownActor"));
            if (model.containsAttribute("initialActorId")) {
                model.addAttribute("path", model.getAttribute("initialActorId")); // Pfad/ID für unknown actor
            }
            model.addAttribute("pageTitle", "Akteur nicht gefunden");
            return "errors/unknown-actor"; // Spezifisches Template für unknown actor
        }

        // Standard-Fehlerbehandlung für 404, 500 und andere
        if (status instanceof Integer) {
            int statusCode = (Integer) status;
            if (statusCode == HttpStatus.NOT_FOUND.value()) { // 404
                model.addAttribute("pageTitle", "Seite nicht gefunden");
                model.addAttribute("errorMessage", "Die angefragte URL wurde auf diesem Server nicht gefunden.");
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) { // 500
                model.addAttribute("pageTitle", "Interner Serverfehler");
                model.addAttribute("errorMessage", "Entschuldigen Sie die Unannehmlichkeiten. Es ist ein unerwarteter Serverfehler aufgetreten.");
            } else if (statusCode == HttpStatus.UNAUTHORIZED.value()) { // 401
                model.addAttribute("pageTitle", "Nicht autorisiert");
                model.addAttribute("errorMessage", "Sie sind nicht berechtigt, auf diese Ressource zuzugreifen. Bitte melden Sie sich an oder kontaktieren Sie den Administrator.");
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) { // 403
                model.addAttribute("pageTitle", "Zugriff verweigert");
                model.addAttribute("errorMessage", "Sie haben keine Berechtigung, auf diese Ressource zuzugreifen.");
            }
            else {
                model.addAttribute("pageTitle", "Fehler " + statusCode);
                model.addAttribute("errorMessage", "Ein Fehler ist aufgetreten: " + (message != null ? message.toString() : "Details unbekannt."));
            }
        } else {
            model.addAttribute("pageTitle", "Fehler");
            model.addAttribute("errorMessage", "Ein Fehler ist aufgetreten.");
        }

        return "errors/error"; // Das allgemeine Fehler-Template
    }

    @GetMapping("/web/error/{code}")
    public String showSpecificErrorPage(@PathVariable int code, Model model) {
        addGlobalAttributesForErrorPage(model); // Globale Attribute
        model.addAttribute("status", code);

        // HIER WIRD ErrorMessages.getTitleForCode(code) erwartet.
        // Falls Sie diese Klasse nicht haben, entfernen Sie diese Zeilen oder implementieren Sie sie.
        // model.addAttribute("titleOverride", ErrorMessages.getTitleForCode(code));
        // model.addAttribute("messageOverride", ErrorMessages.getMessageForCode(code));

        model.addAttribute("path", "/web/error/" + code);
        model.addAttribute("timestamp", new java.util.Date());

        // Setzen Sie pageTitle und errorMessage für dieses spezifische Mapping
        if (code == HttpStatus.NOT_FOUND.value()) {
            model.addAttribute("pageTitle", "Seite nicht gefunden");
            model.addAttribute("errorMessage", "Die angefragte URL konnte nicht gefunden werden.");
        } else if (code == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            model.addAttribute("pageTitle", "Interner Serverfehler");
            model.addAttribute("errorMessage", "Es ist ein unerwarteter Serverfehler aufgetreten.");
        } else if (code == HttpStatus.UNAUTHORIZED.value()) { // 401
            model.addAttribute("pageTitle", "Nicht autorisiert");
            model.addAttribute("errorMessage", "Sie sind nicht berechtigt, auf diese Ressource zuzugreifen.");
        } else if (code == HttpStatus.FORBIDDEN.value()) { // 403
            model.addAttribute("pageTitle", "Zugriff verweigert");
            model.addAttribute("errorMessage", "Sie haben keine Berechtigung, auf diese Ressource zuzugreifen.");
        }
        else {
            model.addAttribute("pageTitle", "Fehler " + code);
            model.addAttribute("errorMessage", "Ein spezifischer Fehler ist aufgetreten.");
        }

        return "errors/error"; // Verwendet Ihr angepasstes Fehler-Template
    }
}
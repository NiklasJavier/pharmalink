package de.jklein.pharmalink.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    private void addGlobalAttributesForErrorPage(Model model) {
        String backendVersion = "1.0.1";
        model.addAttribute("backendVersion", backendVersion);
    }

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        addGlobalAttributesForErrorPage(model);

        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object errorType = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        if (model.containsAttribute("flashErrorForUnknownActor")) {
            model.addAttribute("errorMessage", model.getAttribute("flashErrorForUnknownActor"));
            if (model.containsAttribute("initialActorId")) {
                model.addAttribute("path", model.getAttribute("initialActorId"));
            }
            model.addAttribute("pageTitle", "Akteur nicht gefunden");
            return "errors/unknown-actor";
        }

        String statusString = (status != null) ? status.toString() : "Unbekannt";
        model.addAttribute("status", statusString);
        model.addAttribute("error", (errorType != null) ? errorType.toString() : "Unbekannter Fehler");
        model.addAttribute("path", (path != null && !path.toString().isEmpty()) ? path.toString() : "Unbekannter Pfad");
        model.addAttribute("timestamp", new java.util.Date());

        if (exception instanceof Throwable) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            ((Throwable) exception).printStackTrace(pw);
            model.addAttribute("exceptionStackTrace", sw.toString());
            model.addAttribute("exceptionMessage", ((Throwable) exception).getMessage());
        }

        if (status instanceof Integer) {
            int statusCode = (Integer) status;
            if (statusCode == HttpStatus.UNAUTHORIZED.value()) { // 401 Unauthorized
                model.addAttribute("pageTitle", "Nicht autorisiert");
                model.addAttribute("errorMessage", "Sie sind nicht angemeldet oder Ihre Sitzung ist abgelaufen.");
                model.addAttribute("message", "Bitte melden Sie sich an, um fortzufahren."); // Zusätzliche Anweisung
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) { // 403 Forbidden
                model.addAttribute("pageTitle", "Zugriff verweigert");
                model.addAttribute("errorMessage", "Sie haben keine Berechtigung, auf diese Ressource zuzugreifen.");
                model.addAttribute("message", null); // Keine zusätzliche Nachricht für 403
            } else if (statusCode == HttpStatus.NOT_FOUND.value()) { // 404 Not Found
                model.addAttribute("pageTitle", "Seite nicht gefunden");
                model.addAttribute("errorMessage", "Die angefragte URL wurde auf diesem Server nicht gefunden.");
                model.addAttribute("message", (message != null) ? message.toString() : null); // Ursprüngliche Nachricht beibehalten oder null
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) { // 500 Internal Server Error
                model.addAttribute("pageTitle", "Interner Serverfehler");
                model.addAttribute("errorMessage", "Entschuldigen Sie die Unannehmlichkeiten. Es ist ein unerwarteter Serverfehler aufgetreten.");
                model.addAttribute("message", "Unser Team wurde benachrichtigt und arbeitet bereits an einer Lösung.");
            } else { // Andere Statuscodes
                model.addAttribute("pageTitle", "Fehler " + statusCode);
                model.addAttribute("errorMessage", "Ein Fehler ist aufgetreten: " + (message != null ? message.toString() : "Details unbekannt."));
                model.addAttribute("message", null);
            }
        } else { // Fallback, wenn Status null oder kein Integer ist
            model.addAttribute("pageTitle", "Fehler");
            model.addAttribute("errorMessage", "Ein unerwarteter Fehler ist aufgetreten.");
            model.addAttribute("message", null);
        }

        return "errors/error";
    }

    @GetMapping("/web/error/{code}")
    public String showSpecificErrorPage(@PathVariable int code, Model model) {
        addGlobalAttributesForErrorPage(model);
        model.addAttribute("status", code);
        model.addAttribute("path", "/web/error/" + code);
        model.addAttribute("timestamp", new java.util.Date());

        if (code == HttpStatus.UNAUTHORIZED.value()) {
            model.addAttribute("pageTitle", "Nicht autorisiert");
            model.addAttribute("errorMessage", "Sie sind nicht angemeldet oder Ihre Sitzung ist abgelaufen.");
        } else if (code == HttpStatus.FORBIDDEN.value()) {
            model.addAttribute("pageTitle", "Zugriff verweigert");
            model.addAttribute("errorMessage", "Sie haben keine Berechtigung, auf diese Ressource zuzugreifen.");
        } else if (code == HttpStatus.NOT_FOUND.value()) {
            model.addAttribute("pageTitle", "Seite nicht gefunden");
            model.addAttribute("errorMessage", "Die angefragte URL konnte nicht gefunden werden.");
        } else if (code == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            model.addAttribute("pageTitle", "Interner Serverfehler");
            model.addAttribute("errorMessage", "Es ist ein unerwarteter Serverfehler aufgetreten.");
        } else {
            model.addAttribute("pageTitle", "Fehler " + code);
            model.addAttribute("errorMessage", "Ein spezifischer Fehler ist aufgetreten.");
        }
        return "errors/error";
    }
}
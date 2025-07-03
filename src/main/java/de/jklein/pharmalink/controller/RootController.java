package de.jklein.pharmalink.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller für die Root-URL der Anwendung.
 * Leitet unauthentifizierte Benutzer zur Login-Seite um.
 */
@Controller
public class RootController {

    /**
     * Leitet Anfragen an die Root-URL (/) zur Login-Seite der Anwendung um.
     * Dies ist nützlich, um sicherzustellen, dass Benutzer immer zuerst
     * auf die Login-Seite geleitet werden, wenn sie die Anwendung aufrufen,
     * besonders wenn Spring Security aktiv ist.
     *
     * @return Eine Umleitungs-URL zur Login-Seite.
     */
    @GetMapping("/")
    public String redirectToAppLogin() {
        // Leitet zu der URL der Login-Seite um, die in der SecurityConfig als .loginPage() definiert ist.
        // Spring Security fängt diese Umleitung ab und zeigt die Login-Seite an.
        return "redirect:/app/login";
    }
}
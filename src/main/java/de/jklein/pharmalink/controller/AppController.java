package de.jklein.pharmalink.controller;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/app")
public class AppController {

    @GetMapping("/login")
    public String showLogin(Model model) { // Umbenannt, um klarer zu sein
        // Gibt den Namen des Thymeleaf-Templates zurück (z.B. "login.html" in src/main/resources/templates/)
        return "login"; // Sucht nach src/main/resources/templates/login.html
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) { // Umbenannt, um klarer zu sein
        // Gibt den Namen des Thymeleaf-Templates zurück (z.B. "dashboard.html" in src/main/resources/templates/)
        return "dashboard"; // Sucht nach src/main/resources/templates/dashboard.html
    }
}

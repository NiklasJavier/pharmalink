package de.jklein.pharmalink.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {
    @GetMapping("/")
    public String redirectToDashboard() {
        // Leite zu dem Thymeleaf-Template für die Login-Seite um, nicht zu einem Jsp
        return "redirect:/app/login"; // Oder direkt "login", wenn login.html im templates-Ordner liegt
    }
}
package de.jklein.pharmalink.controller;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/app")
public class AppController {

    @GetMapping("/login")
    public String showDashboard(Model model) {
        return "login";
    }

    @GetMapping("/dashboard")
    public String showProfile(Model model) {
        return "dashboard";
    }
}

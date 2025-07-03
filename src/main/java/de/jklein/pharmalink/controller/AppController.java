package de.jklein.pharmalink.controller;

import ch.qos.logback.core.model.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/app")
public class AppController {

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        return "dashboard";
    }

    @GetMapping("/profile")
    public String showProfile(Model model) {
        return "profile";
    }
}

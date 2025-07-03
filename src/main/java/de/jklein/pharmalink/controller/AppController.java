package de.jklein.pharmalink.controller;

import de.jklein.pharmalink.service.system.SystemStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
        return "auth/login";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("initialActorId", systemStateService.getInitialActorId());
        return "dashboard/overview";
    }
}
package de.jklein.pharmalink.controller;

import org.springframework.security.core.Authentication; // Import Authentication
import org.springframework.security.core.context.SecurityContextHolder; // Import SecurityContextHolder
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

    @GetMapping("/")
    public String redirectToAppropriatePage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if the user is authenticated and not an anonymous user
        // An anonymous user is considered authenticated by Spring Security,
        // but it's typically used for public access and not a real user.
        if (authentication != null && authentication.isAuthenticated() &&
                !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            // If authenticated, redirect to the dashboard
            return "redirect:/app/dashboard";
        } else {
            // If not authenticated (or anonymous), redirect to the login page
            return "redirect:/app/login";
        }
    }
}
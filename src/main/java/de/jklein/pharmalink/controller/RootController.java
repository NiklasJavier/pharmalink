package de.jklein.pharmalink.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.authentication.AnonymousAuthenticationToken; // Wichtig: Import hinzufügen
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class RootController {

    private static final Logger logger = LoggerFactory.getLogger(RootController.class);

    @GetMapping("/")
    public String redirectToAppropriatePage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        logger.info("RootController Debug: Initializing request to root URL '/'.");

        if (authentication != null) {
            logger.info("RootController Debug: Authentication Object: {}", authentication);
            logger.info("RootController Debug: Authentication Class: {}", authentication.getClass().getName()); // EXAKTER KLASSENNAME
            logger.info("RootController Debug: Authentication Principal: {}", authentication.getPrincipal());
            logger.info("RootController Debug: Authentication Principal Class: {}", authentication.getPrincipal().getClass().getName()); // EXAKTER KLASSENNAME DES PRINCIPAL
            logger.info("RootController Debug: authentication.isAuthenticated(): {}", authentication.isAuthenticated());
            logger.info("RootController Debug: authentication instanceof AnonymousAuthenticationToken: {}", (authentication instanceof AnonymousAuthenticationToken));
            logger.info("RootController Debug: Combined condition: (isAuthenticated && !isAnonymous): {}", (authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)));


            if (authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
                logger.info("RootController Debug: User is truly authenticated, redirecting to /app/dashboard");
                return "redirect:/app/dashboard";
            } else {
                logger.info("RootController Debug: User is NOT truly authenticated (or is anonymous), redirecting to /app/login");
                return "redirect:/app/login";
            }
        } else {
            logger.info("RootController Debug: No Authentication object found in SecurityContext, redirecting to /app/login");
            return "redirect:/app/login";
        }
    }
}
package de.jklein.pharmalink.listener;

import de.jklein.pharmalink.domain.audit.LoginAttempt;
import de.jklein.pharmalink.repository.audit.LoginAttemptRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class LoginAuditListener implements ApplicationListener<AbstractAuthenticationEvent> {

    private static final Logger logger = LoggerFactory.getLogger(LoginAuditListener.class);

    private final LoginAttemptRepository loginAttemptRepository;

    @Autowired
    public LoginAuditListener(LoginAttemptRepository loginAttemptRepository) {
        this.loginAttemptRepository = loginAttemptRepository;
    }

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        boolean successful = event instanceof AuthenticationSuccessEvent;
        String username = event.getAuthentication().getName();
        String ipAddress = getCurrentRequestIpAddress();
        String userAgent = getCurrentRequestUserAgent();
        String authType = event.getAuthentication().getClass().getSimpleName();
        String errorMessage = successful ? null : ((AbstractAuthenticationFailureEvent) event).getException().getMessage();

        LoginAttempt loginAttempt = new LoginAttempt(username, LocalDateTime.now(), successful, ipAddress, userAgent, errorMessage, authType);
        saveLoginAttempt(loginAttempt);

        if (successful) {
            logger.info("Erfolgreicher Login: Benutzer '{}' von IP '{}'", username, ipAddress);
        } else {
            logger.warn("Fehlgeschlagener Login: Benutzer '{}' von IP '{}' - Grund: {}", username, ipAddress, errorMessage);
        }
    }

    private void saveLoginAttempt(LoginAttempt loginAttempt) {
        try {
            loginAttemptRepository.save(loginAttempt);
            logger.debug("Login-Versuch für Benutzer '{}' gespeichert. Erfolgreich: {}", loginAttempt.getUsername(), loginAttempt.isSuccessful());
        } catch (Exception e) {
            logger.error("Fehler beim Speichern des Login-Versuchs für Benutzer '{}': {}", loginAttempt.getUsername(), e.getMessage());
        }
    }

    private String getCurrentRequestIpAddress() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(HttpServletRequest::getRemoteAddr)
                .orElse("UNBEKANNT");
    }

    private String getCurrentRequestUserAgent() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(request -> request.getHeader("User-Agent"))
                .orElse("UNBEKANNT");
    }
}
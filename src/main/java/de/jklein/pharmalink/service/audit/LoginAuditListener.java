package de.jklein.pharmalink.service.audit;

import de.jklein.pharmalink.domain.audit.LoginAttempt;
import de.jklein.pharmalink.repository.audit.LoginAttemptRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class LoginAuditListener implements ApplicationListener<org.springframework.context.ApplicationEvent> {

    private static final Logger logger = LoggerFactory.getLogger(LoginAuditListener.class);

    private final LoginAttemptRepository loginAttemptRepository;

    @Autowired
    public LoginAuditListener(LoginAttemptRepository loginAttemptRepository) {
        this.loginAttemptRepository = loginAttemptRepository;
    }

    @Override
    public void onApplicationEvent(org.springframework.context.ApplicationEvent event) {
        if (event instanceof AuthenticationSuccessEvent successEvent) {
            handleSuccessfulLogin(successEvent);
        } else if (event instanceof AbstractAuthenticationFailureEvent failureEvent) {
            handleFailedLogin(failureEvent);
        }
    }

    private void handleSuccessfulLogin(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        String ipAddress = getCurrentRequestIpAddress();
        String userAgent = getCurrentRequestUserAgent();
        String authType = event.getAuthentication().getClass().getSimpleName();

        LoginAttempt loginAttempt = new LoginAttempt(username, LocalDateTime.now(), true, ipAddress, userAgent, null, authType);
        saveLoginAttempt(loginAttempt);
        logger.info("Erfolgreicher Login: Benutzer '{}' von IP '{}'", username, ipAddress);
    }

    private void handleFailedLogin(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String errorMessage = event.getException().getMessage();
        String ipAddress = getCurrentRequestIpAddress();
        String userAgent = getCurrentRequestUserAgent();
        String authType = event.getAuthentication().getClass().getSimpleName();

        LoginAttempt loginAttempt = new LoginAttempt(username, LocalDateTime.now(), false, ipAddress, userAgent, errorMessage, authType);
        saveLoginAttempt(loginAttempt);
        logger.warn("Fehlgeschlagener Login: Benutzer '{}' von IP '{}' - Grund: {}", username, ipAddress, errorMessage);
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
                .orElse("UNKNOWN");
    }

    private String getCurrentRequestUserAgent() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(request -> request.getHeader("User-Agent"))
                .orElse("UNKNOWN");
    }
}
package de.jklein.pharmalinkclient.service;

import com.vaadin.flow.server.VaadinSession;
import de.jklein.pharmalinkclient.config.BackendConfig;
import de.jklein.pharmalinkclient.dto.auth.LoginRequest;
import de.jklein.pharmalinkclient.dto.LoginResponse;
import de.jklein.pharmalinkclient.security.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final WebClient webClient;
    private final UserSession userSession;
    private final BackendConfig backendConfig;

    public AuthService(WebClient.Builder webClientBuilder, UserSession userSession, BackendConfig backendConfig) {
        this.backendConfig = backendConfig;
        this.webClient = webClientBuilder.baseUrl(this.backendConfig.getBaseUrl()).build();
        this.userSession = userSession;
    }

    public CompletableFuture<Optional<String>> fetchToken(String username, String password) {
        log.info("Frage Token für Benutzer an: {}", username);
        LoginRequest loginRequest = new LoginRequest(username, password);
        CompletableFuture<Optional<String>> future = new CompletableFuture<>();

        webClient.post()
                .uri("/v1/auth/login") // **GEÄNDERT: Relative URI verwenden**
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(loginRequest), LoginRequest.class)
                .retrieve()
                .bodyToMono(LoginResponse.class)
                .map(response -> Optional.ofNullable(response.getJwt()))
                .defaultIfEmpty(Optional.empty())
                .subscribe(
                        future::complete,
                        error -> {
                            log.error("Fehler beim Abrufen des Tokens für Benutzer '{}': {}", username, error.getMessage());
                            future.complete(Optional.empty());
                        }
                );
        return future;
    }
    
    public void logout() {
        if (VaadinSession.getCurrent() != null) {
            try {
                userSession.setJwt(null);
                userSession.setUsername(null);
                VaadinSession.getCurrent().getSession().removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                log.info("Benutzerdefinierte Session und Security Context aus HttpSession entfernt.");
            } catch (Exception e) {
                log.error("Fehler beim Bereinigen der Vaadin-Session.", e);
            }
        }
        SecurityContextHolder.clearContext();
        log.info("Benutzer ausgeloggt.");
    }
}
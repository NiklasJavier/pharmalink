package de.jklein.pharmalinkclient.service;

import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import de.jklein.pharmalinkclient.config.BackendConfig;
import de.jklein.pharmalinkclient.dto.ActorIdResponse;
import de.jklein.pharmalinkclient.dto.SystemStatsDto;
import de.jklein.pharmalinkclient.dto.SystemStateDto;
import de.jklein.pharmalinkclient.security.UserSession;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@SpringComponent
@VaadinSessionScope
public class SystemService {

    private final WebClient webClient;
    private final UserSession userSession;
    private final BackendConfig backendConfig;

    public SystemService(WebClient.Builder webClientBuilder, UserSession userSession, BackendConfig backendConfig) {
        this.backendConfig = backendConfig;
        this.webClient = webClientBuilder.baseUrl(this.backendConfig.getBaseUrl()).build();
        this.userSession = userSession;
    }

    public Mono<String> getCurrentActorId() {
        return webClient.get()
                .uri("/system/current-actor-id")
                .headers(headers -> {
                    String jwt = userSession.getJwt();
                    if (jwt != null && !jwt.isEmpty()) {
                        headers.setBearerAuth(jwt);
                    } else {
                        System.err.println("Kein JWT-Token in der UserSession verfügbar für getCurrentActorId.");
                    }
                })
                .retrieve()
                .bodyToMono(ActorIdResponse.class)
                .map(ActorIdResponse::getActorId);
    }

    public Mono<SystemStatsDto> getCacheStats() {
        return webClient.get()
                .uri("/system/cache/stats")
                .headers(headers -> {
                    String jwt = userSession.getJwt();
                    if (jwt != null && !jwt.isEmpty()) {
                        headers.setBearerAuth(jwt);
                    } else {
                        System.err.println("Kein JWT-Token in der UserSession verfügbar für getCacheStats.");
                    }
                })
                .retrieve()
                .bodyToMono(SystemStatsDto.class);
    }

    public Mono<SystemStateDto> getCacheState() {
        return webClient.get()
                .uri("/system/cache/state")
                .headers(headers -> {
                    String jwt = userSession.getJwt();
                    if (jwt != null && !jwt.isEmpty()) {
                        headers.setBearerAuth(jwt);
                    } else {
                        System.err.println("Kein JWT-Token in der UserSession verfügbar für getCacheState.");
                    }
                })
                .retrieve()
                .bodyToMono(SystemStateDto.class); 
    }
}
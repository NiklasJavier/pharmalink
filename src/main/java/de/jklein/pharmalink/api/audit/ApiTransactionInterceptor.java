package de.jklein.pharmalink.api.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jklein.pharmalink.domain.audit.ApiTransaction;
import de.jklein.pharmalink.repository.audit.ApiTransactionRepository;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class ApiTransactionInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ApiTransactionInterceptor.class);
    private static final String START_TIME_ATTRIBUTE = "apiStartTime";

    private final ApiTransactionRepository apiTransactionRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public ApiTransactionInterceptor(ApiTransactionRepository apiTransactionRepository, ObjectMapper objectMapper) {
        this.apiTransactionRepository = apiTransactionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Nur für DispatcherType.REQUEST (nicht für FORWARD, INCLUDE etc.) protokollieren
        if (request.getDispatcherType() == DispatcherType.REQUEST) {
            request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
            // Optional: Request-Body cachen, falls für Audit-Logs benötigt
            if (!(request instanceof ContentCachingRequestWrapper)) {
                // Dies stellt sicher, dass der Request-Body gelesen werden kann,
                // auch wenn er später im Controller gelesen wird.
                // Erfordert Konfiguration des ContentCachingFilter!
                // Für einfache Audit-Logs wird der Body oft nicht in preHandle gecacht,
                // sondern optional in afterCompletion, falls er noch verfügbar ist.
                // Oder über einen Filter global gecacht.
            }
        }
        return true; // Anfrage weiterleiten
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        // Nichts zu tun in postHandle für dieses Audit-Logging
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        // Nur protokollieren, wenn der Request vom Typ REQUEST war und ein Startzeitpunkt gesetzt wurde
        if (request.getDispatcherType() != DispatcherType.REQUEST || request.getAttribute(START_TIME_ATTRIBUTE) == null) {
            return;
        }

        long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        long duration = System.currentTimeMillis() - startTime;
        LocalDateTime timestamp = LocalDateTime.now();

        String url = request.getRequestURI();
        String method = request.getMethod();
        String username = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .orElse("anonymous"); // oder "unauthenticated"

        String requestBody = null;
        if (request instanceof ContentCachingRequestWrapper) {
            byte[] content = ((ContentCachingRequestWrapper) request).getContentAsByteArray();
            if (content.length > 0) {
                try {
                    requestBody = new String(content, request.getCharacterEncoding());
                } catch (Exception e) {
                    logger.warn("Could not read request body for URL {}: {}", url, e.getMessage());
                }
            }
        }

        int status = response.getStatus();
        boolean successful = (status >= 200 && status < 300); // HTTP 2xx Statuscodes sind erfolgreich
        String errorMessage = null;
        if (ex != null) {
            errorMessage = ex.getMessage();
            successful = false; // Bei Exception ist es ein Fehlschlag
        } else if (!successful) {
            errorMessage = "HTTP Status: " + status; // Kein 2xx, aber keine explizite Exception
        }

        ApiTransaction apiTransaction = new ApiTransaction(url, method, username, requestBody, status, timestamp, successful, errorMessage);

        try {
            apiTransactionRepository.save(apiTransaction);
            logger.debug("Logged API transaction: {} {} -> Status {} (Duration: {}ms)", method, url, status, duration);
        } catch (Exception e) {
            logger.error("Failed to save API transaction log for {} {}: {}", method, url, e.getMessage());
        }
    }
}
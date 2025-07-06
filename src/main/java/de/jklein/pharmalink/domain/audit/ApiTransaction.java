package de.jklein.pharmalink.domain.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_transactions")
@Getter
@Setter
@NoArgsConstructor
public class ApiTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "http_method", nullable = false)
    private String httpMethod;

    @Column(name = "username")
    private String username; // Benutzer (Principal.getName()), falls verfügbar

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody; // Optional: Request-Body als JSON-String

    @Column(name = "response_status", nullable = false)
    private int responseStatus; // HTTP-Statuscode

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "successful", nullable = false)
    private boolean successful; // true für 2xx, false sonst

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // Nur bei Fehlern

    public ApiTransaction(String url, String httpMethod, String username, String requestBody, int responseStatus, LocalDateTime timestamp, boolean successful, String errorMessage) {
        this.url = url;
        this.httpMethod = httpMethod;
        this.username = username;
        this.requestBody = requestBody;
        this.responseStatus = responseStatus;
        this.timestamp = timestamp;
        this.successful = successful;
        this.errorMessage = errorMessage;
    }
}
package de.jklein.pharmalink.domain.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts")
@Getter
@Setter
@NoArgsConstructor
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "successful", nullable = false)
    private boolean successful;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "authentication_type")
    private String authenticationType;

    public LoginAttempt(String username, LocalDateTime timestamp, boolean successful, String ipAddress, String userAgent, String errorMessage, String authenticationType) {
        this.username = username;
        this.timestamp = timestamp;
        this.successful = successful;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.errorMessage = errorMessage;
        this.authenticationType = authenticationType;
    }
}
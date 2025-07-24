package de.jklein.pharmalink.domain.audit;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "audit.login_attempts")
@Getter
@Setter
@NoArgsConstructor
public class LoginAttempt {

    @Id
    private String id;

    @Field("username")
    private String username;

    @Field("timestamp")
    private LocalDateTime timestamp;

    @Field("successful")
    private boolean successful;

    @Field("ip_address")
    private String ipAddress;

    @Field("user_agent")
    private String userAgent;

    @Field("error_message")
    private String errorMessage;

    @Field("authentication_type")
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
package de.jklein.pharmalink.domain.audit;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "audit.api_transactions")
@Getter
@Setter
@NoArgsConstructor
public class ApiTransaction {

    @Id
    private String id;

    @Field("url")
    private String url;

    @Field("http_method")
    private String httpMethod;

    @Field("username")
    private String username;

    @Field("request_body")
    private String requestBody;

    @Field("response_status")
    private int responseStatus;

    @Field("timestamp")
    private LocalDateTime timestamp;

    @Field("successful")
    private boolean successful;

    @Field("error_message")
    private String errorMessage;

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
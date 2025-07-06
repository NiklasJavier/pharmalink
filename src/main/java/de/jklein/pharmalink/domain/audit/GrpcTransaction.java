package de.jklein.pharmalink.domain.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "grpc_transactions")
@Getter
@Setter
@NoArgsConstructor
public class GrpcTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_name", nullable = false)
    private String transactionName;

    @Column(name = "transaction_args", columnDefinition = "TEXT")
    private String transactionArgs; // JSON-String der Argumente

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "successful", nullable = false)
    private boolean successful;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // Nur bei Fehlschlag

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload; // Optional: Antwort-Payload (z.B. JSON-String)

    public GrpcTransaction(String transactionName, String transactionArgs, LocalDateTime timestamp, boolean successful, String errorMessage, String responsePayload) {
        this.transactionName = transactionName;
        this.transactionArgs = transactionArgs;
        this.timestamp = timestamp;
        this.successful = successful;
        this.errorMessage = errorMessage;
        this.responsePayload = responsePayload;
    }
}
package de.jklein.pharmalink.domain.audit;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "audit.grpc_transactions")
@Getter
@Setter
@NoArgsConstructor
public class GrpcTransaction {

    @Id
    private String id;

    @Field("transaction_name")
    private String transactionName;

    @Field("transaction_args")
    private String transactionArgs;

    @Field("timestamp")
    private LocalDateTime timestamp;

    @Field("successful")
    private boolean successful;

    @Field("error_message")
    private String errorMessage;

    @Field("response_payload")
    private String responsePayload;

    public GrpcTransaction(String transactionName, String transactionArgs, LocalDateTime timestamp, boolean successful, String errorMessage, String responsePayload) {
        this.transactionName = transactionName;
        this.transactionArgs = transactionArgs;
        this.timestamp = timestamp;
        this.successful = successful;
        this.errorMessage = errorMessage;
        this.responsePayload = responsePayload;
    }
}
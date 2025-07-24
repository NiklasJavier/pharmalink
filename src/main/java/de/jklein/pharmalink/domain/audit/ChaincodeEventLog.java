package de.jklein.pharmalink.domain.audit;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "audit.chaincode_events")
@Getter
@Setter
@NoArgsConstructor
public class ChaincodeEventLog {

    @Id
    private String id;

    @Field("processed_at")
    private LocalDateTime processedAt = LocalDateTime.now();

    @Field("event_name")
    private String eventName;

    @Field("transaction_id")
    private String transactionId;

    @Field("block_number")
    private long blockNumber;

    @Field("payload")
    private String payload;

    public ChaincodeEventLog(String eventName, String transactionId, long blockNumber, String payload) {
        this.eventName = eventName;
        this.transactionId = transactionId;
        this.blockNumber = blockNumber;
        this.payload = payload;
    }
}
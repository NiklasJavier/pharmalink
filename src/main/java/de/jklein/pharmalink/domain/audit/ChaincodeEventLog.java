package de.jklein.pharmalink.domain.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chaincode_event_audit_log")
@Getter
@Setter
@NoArgsConstructor
public class ChaincodeEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime processedAt;

    @Column(nullable = false)
    private String eventName;

    @Column(nullable = false)
    private String transactionId;

    private long blockNumber;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;

    public ChaincodeEventLog(String eventName, String transactionId, long blockNumber, String payload) {
        this.eventName = eventName;
        this.transactionId = transactionId;
        this.blockNumber = blockNumber;
        this.payload = payload;
    }
}
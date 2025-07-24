package de.jklein.pharmalink.domain.system;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pharmalink.state")
@Data
@NoArgsConstructor
public class SystemState {

    @Id
    private String id;

    private String currentActorId;

    private long lastProcessedBlockNumber = 0L;

    public SystemState(String id, String currentActorId) {
        this.id = id;
        this.currentActorId = currentActorId;
    }
}
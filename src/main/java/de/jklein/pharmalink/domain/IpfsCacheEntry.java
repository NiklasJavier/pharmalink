package de.jklein.pharmalink.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "pharmalink.ipfs_cache")
@Getter
@Setter
@NoArgsConstructor
public class IpfsCacheEntry {

    @Id
    private String ipfsHash;

    private String content;

    private LocalDateTime lastAccessed;

    private LocalDateTime createdAt;

    public IpfsCacheEntry(String ipfsHash, String content) {
        this.ipfsHash = ipfsHash;
        this.content = content;
        this.lastAccessed = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }
}
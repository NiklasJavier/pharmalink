package de.jklein.pharmalink.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entität zum Zwischenspeichern von IPFS-Inhalten in der Datenbank.
 * Der IPFS-Hash dient als Primärschlüssel.
 */
@Entity
@Table(name = "ipfs_cache")
public class IpfsCacheEntry {

    @Id
    @Column(name = "ipfs_hash", nullable = false, unique = true)
    private String ipfsHash;

    // Speichert den Inhalt als Text (z.B. JSON-String)
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "last_accessed", nullable = false)
    private LocalDateTime lastAccessed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Standardkonstruktor für JPA
    public IpfsCacheEntry() {
    }

    public IpfsCacheEntry(String ipfsHash, String content) {
        this.ipfsHash = ipfsHash;
        this.content = content;
        this.lastAccessed = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    // Getter und Setter
    public String getIpfsHash() {
        return ipfsHash;
    }

    public void setIpfsHash(String ipfsHash) {
        this.ipfsHash = ipfsHash;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(LocalDateTime lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "IpfsCacheEntry{" +
                "ipfsHash='" + ipfsHash + '\'' +
                ", content='" + content.substring(0, Math.min(content.length(), 50)) + "...'" + // Kürzen für Log
                ", lastAccessed=" + lastAccessed +
                ", createdAt=" + createdAt +
                '}';
    }
}
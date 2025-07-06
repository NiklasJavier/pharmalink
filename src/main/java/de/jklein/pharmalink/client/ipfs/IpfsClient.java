package de.jklein.pharmalink.client.ipfs;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jklein.pharmalink.config.IpfsConfig;
import de.jklein.pharmalink.domain.IpfsCacheEntry;
import de.jklein.pharmalink.repository.IpfsCacheRepository;
import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Type; // NEU: Import für Type
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Client für die Interaktion mit dem InterPlanetary File System (IPFS).
 * Verantwortlich für das Hinzufügen von Daten zu IPFS und das Abrufen von Daten über Hashes.
 * Implementiert nun auch einen Datenbank-Cache.
 */
@Component
public class IpfsClient {

    private static final Logger logger = LoggerFactory.getLogger(IpfsClient.class);

    private final IPFS ipfs;
    private final ObjectMapper objectMapper;
    private final IpfsCacheRepository ipfsCacheRepository;

    /**
     * Konstruktor zur Initialisierung des IPFS-Clients.
     *
     * @param ipfsConfig Die Konfiguration für den IPFS-Daemon.
     * @param objectMapper Der Jackson ObjectMapper für JSON-Serialisierung/Deserialisierung.
     * @param ipfsCacheRepository Das Repository für den IPFS-Datenbank-Cache.
     */
    @Autowired
    public IpfsClient(IpfsConfig ipfsConfig, ObjectMapper objectMapper, IpfsCacheRepository ipfsCacheRepository) {
        this.ipfs = new IPFS(ipfsConfig.getIpfsHost(), ipfsConfig.getIpfsPort());
        this.objectMapper = objectMapper;
        this.ipfsCacheRepository = ipfsCacheRepository;
        logger.info("IPFS Client initialized with host: {} and port: {}", ipfsConfig.getIpfsHost(), ipfsConfig.getIpfsPort());
    }

    /**
     * Fügt ein Objekt als JSON zu IPFS hinzu und gibt dessen Hash zurück.
     *
     * @param object Das zu speichernde Objekt.
     * @return Der IPFS-Hash des hinzugefügten Objekts als String.
     * @throws IOException Wenn ein Fehler beim Serialisieren oder Hinzufügen zu IPFS auftritt.
     */
    public String addObject(Object object) throws IOException {
        String json = objectMapper.writeValueAsString(object);
        NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(json.getBytes());
        MerkleNode addResult = ipfs.add(file).get(0);
        String ipfsHash = addResult.hash.toBase58();
        logger.info("Object added to IPFS with hash: {}", ipfsHash);

        try {
            IpfsCacheEntry cacheEntry = new IpfsCacheEntry(ipfsHash, json);
            ipfsCacheRepository.save(cacheEntry);
            logger.debug("IPFS content for hash {} cached in database after adding.", ipfsHash);
        } catch (Exception e) {
            logger.error("Failed to cache IPFS content for hash {} after adding: {}", ipfsHash, e.getMessage());
        }

        return ipfsHash;
    }

    /**
     * Ruft den Inhalt eines Objekts von IPFS anhand seines Hashes ab.
     * Sucht zuerst im lokalen Datenbank-Cache.
     *
     * @param ipfsHash Der IPFS-Hash des abzurufenden Objekts.
     * @return Der Inhalt des Objekts als String, oder null, wenn nicht gefunden.
     * @throws IOException Wenn ein Fehler beim Abrufen von IPFS auftritt.
     */
    public String getObject(String ipfsHash) throws IOException {
        if (ipfsHash == null || ipfsHash.isBlank()) {
            logger.warn("Attempted to get IPFS object with null or blank hash.");
            return null;
        }

        Optional<IpfsCacheEntry> cachedEntry = ipfsCacheRepository.findByIpfsHash(ipfsHash);
        if (cachedEntry.isPresent()) {
            IpfsCacheEntry entry = cachedEntry.get();
            entry.setLastAccessed(LocalDateTime.now());
            ipfsCacheRepository.save(entry);
            logger.debug("IPFS content for hash {} found in database cache.", ipfsHash);
            return entry.getContent();
        }

        logger.debug("IPFS content for hash {} not found in database cache. Fetching from IPFS network.", ipfsHash);
        try {
            Multihash filePointer = Multihash.fromBase58(ipfsHash);
            byte[] contentBytes = ipfs.cat(filePointer);
            String content = new String(contentBytes);
            logger.info("Successfully fetched IPFS content for hash: {}", ipfsHash);

            try {
                IpfsCacheEntry newCacheEntry = new IpfsCacheEntry(ipfsHash, content);
                ipfsCacheRepository.save(newCacheEntry);
                logger.debug("IPFS content for hash {} successfully cached in database.", ipfsHash);
            } catch (Exception e) {
                logger.error("Failed to cache IPFS content for hash {}: {}", ipfsHash, e.getMessage());
            }

            return content;
        } catch (Exception e) {
            logger.error("Error fetching IPFS content for hash {}: {}", ipfsHash, e.getMessage());
            throw new IOException("Failed to retrieve IPFS content for hash: " + ipfsHash, e);
        }
    }

    /**
     * Ruft den Inhalt eines Objekts von IPFS ab und deserialisiert es in den angegebenen Typ.
     * Nutzt intern getObject(String ipfsHash) mit Cache-Logik.
     *
     * @param ipfsHash Der IPFS-Hash des abzurufenden Objekts.
     * @param valueType Das Type-Objekt, das den Typ darstellt, in den der Inhalt deserialisiert werden soll (z.B. TypeToken<Map<String, Object>>().getType()).
     * @param <T> Der generische Typ des Objekts.
     * @return Das deserialisierte Objekt, oder null, wenn der Inhalt nicht gefunden oder deserialisiert werden konnte.
     * @throws IOException Wenn ein Fehler beim Abrufen oder Deserialisieren auftritt.
     */
    public <T> T getObject(String ipfsHash, Type valueType) throws IOException { // NEU: Signatur geändert von Class<T> zu Type
        String jsonContent = getObject(ipfsHash);
        if (jsonContent != null) {
            try {
                return objectMapper.readValue(jsonContent, objectMapper.getTypeFactory().constructType(valueType)); // NEU: Anpassung für Type
            } catch (IOException e) {
                logger.error("Error deserializing IPFS content for hash {} to type {}: {}", ipfsHash, valueType.getTypeName(), e.getMessage());
                throw e;
            }
        }
        return null;
    }
}
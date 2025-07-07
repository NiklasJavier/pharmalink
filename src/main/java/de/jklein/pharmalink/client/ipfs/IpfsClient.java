package de.jklein.pharmalink.client.ipfs;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Client für die Interaktion mit IPFS.
 * Finale Version mit korrekter Speicherlogik und Reparaturlogik für Altdaten.
 */
@Component
public class IpfsClient {

    private static final Logger logger = LoggerFactory.getLogger(IpfsClient.class);

    private final IPFS ipfs;
    private final ObjectMapper objectMapper;
    private final IpfsCacheRepository ipfsCacheRepository;

    @Autowired
    public IpfsClient(IpfsConfig ipfsConfig, ObjectMapper objectMapper, IpfsCacheRepository ipfsCacheRepository) {
        this.ipfs = new IPFS(ipfsConfig.getIpfsHost(), ipfsConfig.getIpfsPort());
        this.objectMapper = objectMapper;
        this.ipfsCacheRepository = ipfsCacheRepository;
        logger.info("IPFS Client initialized with host: {} and port: {}", ipfsConfig.getIpfsHost(), ipfsConfig.getIpfsPort());
    }

    /**
     * **FINALE VERSION**: Speichert ein Objekt korrekt in IPFS.
     * Diese Methode erkennt, ob das übergebene Objekt bereits ein JSON-String ist,
     * und verhindert so aktiv eine doppelte Serialisierung.
     *
     * @param data Das zu speichernde Objekt (kann ein POJO, eine Map oder ein JSON-String sein).
     * @return Der IPFS-Hash des hinzugefügten Objekts.
     * @throws IOException Wenn ein Fehler auftritt.
     */
    public String addObject(Object data) throws IOException {
        String jsonStringToSend;

        // Prüfen, ob die Eingabe bereits ein JSON-formatierter String ist.
        if (data instanceof String) {
            String inputString = (String) data;
            try {
                // Wir nutzen readTree, um zu validieren, ob es sich um eine gültige JSON-Struktur handelt.
                objectMapper.readTree(inputString);
                // Wenn ja, verwenden wir den String direkt, ohne erneute Serialisierung.
                jsonStringToSend = inputString;
                logger.debug("Input object is already a valid JSON string. Using it directly.");
            } catch (JsonProcessingException e) {
                // Wenn es ein String, aber KEIN gültiges JSON ist (z.B. "Hallo Welt"),
                // dann behandeln wir es als normalen String und serialisieren es zu einem JSON-String-Literal.
                jsonStringToSend = objectMapper.writeValueAsString(data);
                logger.debug("Input object is a non-JSON string. Serializing it as a JSON string literal.");
            }
        } else {
            // Wenn es kein String ist (z.B. eine Map oder ein POJO), serialisieren wir es normal.
            jsonStringToSend = objectMapper.writeValueAsString(data);
            logger.debug("Input object is a Java object. Serializing it to JSON.");
        }

        // Ab hier ist der jsonStringToSend garantiert korrekt formatiert.
        byte[] jsonBytes = jsonStringToSend.getBytes(StandardCharsets.UTF_8);
        NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(jsonBytes);
        MerkleNode addResult = ipfs.add(file).get(0);
        String ipfsHash = addResult.hash.toBase58();
        logger.info("Object added correctly to IPFS with hash: {}", ipfsHash);

        try {
            IpfsCacheEntry cacheEntry = new IpfsCacheEntry(ipfsHash, jsonStringToSend);
            ipfsCacheRepository.save(cacheEntry);
            logger.debug("Correctly formatted JSON for hash {} cached in database.", ipfsHash);
        } catch (Exception e) {
            logger.error("Failed to cache IPFS content for hash {} after adding: {}", ipfsHash, e.getMessage());
        }

        return ipfsHash;
    }

    /**
     * Ruft den rohen JSON-Inhalt eines Objekts von IPFS ab und nutzt dabei den Cache.
     * (Unverändert, da die Caching-Logik korrekt ist)
     */
    public String getObject(String ipfsHash) throws IOException {
        if (!isValidIpfsHashFormat(ipfsHash)) {
            logger.warn("Invalid IPFS hash format provided: '{}'. Skipping retrieval.", ipfsHash);
            return null;
        }

        Optional<IpfsCacheEntry> cachedEntry = ipfsCacheRepository.findByIpfsHash(ipfsHash);
        if (cachedEntry.isPresent()) {
            IpfsCacheEntry entry = cachedEntry.get();
            entry.setLastAccessed(LocalDateTime.now());
            ipfsCacheRepository.save(entry);
            return entry.getContent();
        }

        try {
            Multihash filePointer = Multihash.fromBase58(ipfsHash);
            byte[] contentBytes = ipfs.cat(filePointer);
            String content = new String(contentBytes, StandardCharsets.UTF_8);

            try {
                IpfsCacheEntry newCacheEntry = new IpfsCacheEntry(ipfsHash, content);
                ipfsCacheRepository.save(newCacheEntry);
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
     * Ruft den Inhalt von IPFS ab und deserialisiert ihn.
     * Behält die Reparaturlogik bei, um fehlerhafte Altdaten lesen zu können.
     */
    public <T> T getObject(String ipfsHash, Type valueType) throws IOException {
        String jsonContent = getObject(ipfsHash);

        if (jsonContent == null || jsonContent.isBlank()) {
            return null;
        }

        String contentToParse = jsonContent;

        // Reparaturlogik für fehlerhafte Altdaten (doppelt serialisiert)
        try {
            String unwrapped = objectMapper.readValue(contentToParse, String.class);
            if (unwrapped.trim().startsWith("{") && unwrapped.trim().endsWith("}")) {
                logger.warn("Detected and repaired double-serialized JSON for hash: {}", ipfsHash);
                contentToParse = unwrapped;
            }
        } catch (JsonProcessingException e) {
            // Dies ist der Normalfall für korrekt gespeicherte Daten.
            logger.debug("Content for hash {} is not double-serialized. Proceeding normally.", ipfsHash);
        }

        // Finale Deserialisierung
        try {
            return objectMapper.readValue(contentToParse, objectMapper.getTypeFactory().constructType(valueType));
        } catch (IOException e) {
            logger.error("FINAL DESERIALIZATION FAILED for hash {} and type {}. Content was: '{}'", ipfsHash, valueType.getTypeName(), contentToParse, e);
            throw e;
        }
    }

    private boolean isValidIpfsHashFormat(String ipfsHash) {
        if (ipfsHash == null || ipfsHash.isBlank()) {
            return false;
        }
        String trimmedHash = ipfsHash.trim();
        return (trimmedHash.startsWith("Qm") && trimmedHash.length() == 46) || (trimmedHash.startsWith("b") && trimmedHash.length() > 30);
    }
}
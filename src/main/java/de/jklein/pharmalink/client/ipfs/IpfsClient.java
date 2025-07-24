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
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
public class IpfsClient {

    private static final Logger logger = LoggerFactory.getLogger(IpfsClient.class);
    private static final int sperrfristFehlgeschlagenerHash = 5;

    private final IPFS ipfs;
    private final ObjectMapper objectMapper;
    private final IpfsCacheRepository ipfsCacheRepository;

    private final Map<String, LocalDateTime> failedHashesCache = new ConcurrentHashMap<>();

    @Autowired
    public IpfsClient(IpfsConfig ipfsConfig, ObjectMapper objectMapper, IpfsCacheRepository ipfsCacheRepository) {
        int timeoutMillis = ipfsConfig.getTimeout() * 1000;
        this.ipfs = new IPFS(ipfsConfig.getHost(), ipfsConfig.getPort(), "/api/v0/", timeoutMillis, timeoutMillis, false);
        this.objectMapper = objectMapper;
        this.ipfsCacheRepository = ipfsCacheRepository;
        logger.info("IPFS-Client initialisiert mit Host: {}, Port: {} und Timeout: {}s",
                ipfsConfig.getHost(), ipfsConfig.getPort(), ipfsConfig.getTimeout());
    }

    public String addObject(Object data) throws IOException {
        String jsonStringToSend;

        if (data instanceof String) {
            String inputString = (String) data;
            try {
                objectMapper.readTree(inputString);
                jsonStringToSend = inputString;
                logger.debug("Eingabeobjekt ist bereits ein gültiger JSON-String. Wird direkt verwendet.");
            } catch (JsonProcessingException e) {
                jsonStringToSend = objectMapper.writeValueAsString(data);
                logger.debug("Eingabeobjekt ist ein Nicht-JSON-String. Wird als JSON-String-Literal serialisiert.");
            }
        } else {
            jsonStringToSend = objectMapper.writeValueAsString(data);
            logger.debug("Eingabeobjekt ist ein Java-Objekt. Wird zu JSON serialisiert.");
        }

        byte[] jsonBytes = jsonStringToSend.getBytes(StandardCharsets.UTF_8);
        NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(jsonBytes);
        MerkleNode addResult = ipfs.add(file).get(0);
        String ipfsHash = addResult.hash.toBase58();
        logger.info("Objekt erfolgreich zu IPFS mit Hash {} hinzugefügt.", ipfsHash);

        try {
            IpfsCacheEntry cacheEntry = new IpfsCacheEntry(ipfsHash, jsonStringToSend);
            ipfsCacheRepository.save(cacheEntry);
            logger.debug("Korrekt formatiertes JSON für Hash {} in der Datenbank zwischengespeichert.", ipfsHash);
        } catch (Exception e) {
            logger.error("Fehler beim Zwischenspeichern des IPFS-Inhalts für Hash {} nach dem Hinzufügen: {}", ipfsHash, e.getMessage());
        }

        return ipfsHash;
    }

    public String getObject(String ipfsHash) throws IOException {
        if (!isValidIpfsHashFormat(ipfsHash)) {
            logger.warn("Ungültiges IPFS-Hash-Format angegeben: '{}'. Abruf wird übersprungen.", ipfsHash);
            return null;
        }

        Optional<IpfsCacheEntry> cachedEntry = ipfsCacheRepository.findByIpfsHash(ipfsHash);
        if (cachedEntry.isPresent()) {
            IpfsCacheEntry entry = cachedEntry.get();
            entry.setLastAccessed(LocalDateTime.now());
            ipfsCacheRepository.save(entry);
            return entry.getContent();
        }

        if (failedHashesCache.containsKey(ipfsHash)) {
            LocalDateTime failedTime = failedHashesCache.get(ipfsHash);
            if (failedTime.plus(sperrfristFehlgeschlagenerHash, ChronoUnit.MINUTES).isAfter(LocalDateTime.now())) {
                logger.trace("IPFS-Abruf für Hash {} wird übersprungen (negativer Cache).", ipfsHash);
                return null;
            } else {
                failedHashesCache.remove(ipfsHash);
            }
        }

        try {
            Multihash filePointer = Multihash.fromBase58(ipfsHash);
            byte[] contentBytes = ipfs.cat(filePointer);
            String content = new String(contentBytes, StandardCharsets.UTF_8);

            try {
                IpfsCacheEntry newCacheEntry = new IpfsCacheEntry(ipfsHash, content);
                ipfsCacheRepository.save(newCacheEntry);
            } catch (Exception e) {
                logger.error("Fehler beim Zwischenspeichern des IPFS-Inhalts für Hash {}: {}", ipfsHash, e.getMessage());
            }

            return content;
        } catch (Exception e) {
            logger.error("Fehler beim Abrufen des IPFS-Inhalts für Hash {}: {}", ipfsHash, e.getMessage());
            failedHashesCache.put(ipfsHash, LocalDateTime.now());
            throw new IOException("Abrufen des IPFS-Inhalts für Hash fehlgeschlagen: " + ipfsHash, e);
        }
    }

    public <T> T getObject(String ipfsHash, Type valueType) throws IOException {
        String jsonContent = getObject(ipfsHash);

        if (jsonContent == null || jsonContent.isBlank()) {
            return null;
        }

        String contentToParse = jsonContent;

        try {
            String unwrapped = objectMapper.readValue(contentToParse, String.class);
            if (unwrapped.trim().startsWith("{") && unwrapped.trim().endsWith("}")) {
                logger.warn("Doppelt serialisiertes JSON für Hash {} erkannt und repariert.", ipfsHash);
                contentToParse = unwrapped;
            }
        } catch (JsonProcessingException e) {
            logger.debug("Inhalt für Hash {} ist nicht doppelt serialisiert. Fortfahren mit normaler Verarbeitung.", ipfsHash);
        }

        try {
            return objectMapper.readValue(contentToParse, objectMapper.getTypeFactory().constructType(valueType));
        } catch (IOException e) {
            logger.error("FINALE DESERIALISIERUNG FEHLGESCHLAGEN für Hash {} und Typ {}. Inhalt war: '{}'", ipfsHash, valueType.getTypeName(), contentToParse, e);
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
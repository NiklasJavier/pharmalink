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
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Client für die Interaktion mit dem InterPlanetary File System (IPFS).
 * Verantwortlich für das Hinzufügen von Daten zu IPFS und das Abrufen von Daten über Hashes.
 * Implementiert nun auch einen Datenbank-Cache und Validierung von Hashes.
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
     * Fügt ein Objekt als JSON zu IPFS hinzu und gibt dessen Hash zurück.
     * Wenn das übergebene Objekt bereits ein String ist und als JSON-String-Literal erkannt wird,
     * wird dieser "entmaskiert" und das resultierende Roh-JSON direkt verwendet,
     * um eine doppelte Serialisierung zu vermeiden. Andernfalls wird das Objekt wie gewohnt serialisiert.
     *
     * @param object Das zu speichernde Objekt.
     * @return Der IPFS-Hash des hinzugefügten Objekts als String.
     * @throws IOException Wenn ein Fehler beim Serialisieren oder Hinzufügen zu IPFS auftritt.
     */
    public String addObject(Object object) throws IOException {
        String jsonStringToSend;

        if (object instanceof String) {
            String inputString = (String) object;
            try {
                // Erster Versuch: Versuchen Sie, den Eingabestring als einen einfachen String aus JSON zu deserialisieren.
                // Dies "entmaskiert" einen JSON-String-Literal (z.B. "{\"key\":\"value\"}" -> {"key":"value"}).
                String unescapedContent = objectMapper.readValue(inputString, String.class);

                // Zweiter Schritt: Prüfen Sie, ob der "entmaskierte" Inhalt tatsächlich eine gültige JSON-Struktur ist.
                // Dies unterscheidet zwischen einem entmaskierten JSON-Literal und einem einfachen String (z.B. "hello").
                objectMapper.readTree(unescapedContent);
                jsonStringToSend = unescapedContent; // Es war ein JSON-String-Literal, verwenden Sie nun das Roh-JSON.

            } catch (Exception e) {
                // Wenn der `inputString` kein gültiges JSON-String-Literal war (z.B. ein einfacher String "hello",
                // oder ein fehlerhaftes JSON wie '{...}' mit einfachen Anführungszeichen),
                // dann serialisieren Sie das ursprüngliche `object` als ein Standard-Java-Objekt zu JSON.
                jsonStringToSend = objectMapper.writeValueAsString(object);
            }
        } else {
            // Wenn es kein String ist (z.B. ein Java-POJO), serialisieren Sie es immer zu JSON.
            jsonStringToSend = objectMapper.writeValueAsString(object);
        }

        NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(jsonStringToSend.getBytes());
        MerkleNode addResult = ipfs.add(file).get(0);
        String ipfsHash = addResult.hash.toBase58();
        logger.info("Object added to IPFS with hash: {}", ipfsHash);

        try {
            // Speichern Sie den tatsächlich an IPFS gesendeten JSON-String im Cache
            IpfsCacheEntry cacheEntry = new IpfsCacheEntry(ipfsHash, jsonStringToSend);
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
     * @return Der Inhalt des Objekts als String, oder null, wenn nicht gefunden oder Hash ungültig.
     * @throws IOException Wenn ein Fehler beim Abrufen von IPFS auftritt.
     */
    public String getObject(String ipfsHash) throws IOException {
        // NEU: Vorabprüfung des IPFS-Hash-Formats
        if (!isValidIpfsHashFormat(ipfsHash)) {
            logger.warn("Attempted to get IPFS object with invalid hash format: '{}'. Skipping retrieval.", ipfsHash);
            return null; // Ungültiges Format, nicht weitersuchen
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
     * Nutzt intern getObject(String ipfsHash) mit Cache-Logik und Hash-Validierung.
     *
     * @param ipfsHash Der IPFS-Hash des abzurufenden Objekts.
     * @param valueType Das Type-Objekt, das den Typ darstellt, in den der Inhalt deserialisiert werden soll.
     * @param <T> Der generische Typ des Objekts.
     * @return Das deserialisierte Objekt, oder null, wenn der Inhalt nicht gefunden oder deserialisiert werden konnte.
     * @throws IOException Wenn ein Fehler beim Abrufen oder Deserialisieren auftritt.
     */
    public <T> T getObject(String ipfsHash, Type valueType) throws IOException {
        String jsonContent = getObject(ipfsHash); // Nutzt die Caching-Logik und Hash-Validierung
        if (jsonContent != null) {
            try {
                // Überprüfen, ob der String mit ' beginnt und endet, und diese entfernen.
                // Dies behebt das Problem der doppelten String-Kodierung/falscher Anführungszeichen.
                String contentToParse = jsonContent;
                if (contentToParse.startsWith("'") && contentToParse.endsWith("'") && contentToParse.length() > 1) {
                    contentToParse = contentToParse.substring(1, contentToParse.length() - 1);
                }

                // Nun den (bereinigten) String deserialisieren
                return objectMapper.readValue(contentToParse, objectMapper.getTypeFactory().constructType(valueType));
            } catch (IOException e) {
                logger.error("Error deserializing IPFS content for hash {} to type {}: {}", ipfsHash, valueType.getTypeName(), e.getMessage());
                throw e;
            }
        }
        return null;
    }

    /**
     * Überprüft, ob der gegebene String ein grundlegend gültiges IPFS-Hash-Format hat.
     * Dies ist eine heuristische Prüfung und ersetzt keine vollständige CID-Validierung.
     *
     * @param ipfsHash Der zu prüfende IPFS-Hash.
     * @return true, wenn der Hash ein gültiges Format zu haben scheint, false sonst.
     */
    private boolean isValidIpfsHashFormat(String ipfsHash) {
        if (ipfsHash == null || ipfsHash.isBlank()) {
            return false;
        }
        String trimmedHash = ipfsHash.trim();
        // Grundlegende Prüfung für CIDv0 (Qm...) und CIDv1 (b...)
        // CIDv0 Hashes sind 46 Zeichen lang, CIDv1 können länger sein und beginnen mit 'b'.
        // Diese Prüfung ist eine Heuristik, keine vollständige CID-Validierung (die Multihash übernimmt).
        return (trimmedHash.startsWith("Qm") && trimmedHash.length() == 46) || (trimmedHash.startsWith("b") && trimmedHash.length() > 30);
    }
}
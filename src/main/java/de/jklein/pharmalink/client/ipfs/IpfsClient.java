package de.jklein.pharmalink.client.ipfs;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // Autowired für die IPFS-Bean
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Component
public class IpfsClient {

    private static final Logger logger = LoggerFactory.getLogger(IpfsClient.class);

    private final IPFS ipfs; // IPFS-Instanz wird injiziert

    // Konstruktor-Injektion für die IPFS-Bean
    @Autowired
    public IpfsClient(IPFS ipfs) {
        this.ipfs = ipfs;
        logger.info("IPFS client instance created.");
    }

    /**
     * Fügt Inhalte zum IPFS-Netzwerk hinzu.
     *
     * @param data Der Inhalt als Byte-Array.
     * @return Der CID (Content Identifier) des hinzugefügten Inhalts.
     * @throws IOException Wenn ein Fehler beim Hinzufügen der Daten auftritt.
     */
    public String add(byte[] data) throws IOException {
        NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(data);
        MerkleNode result = ipfs.add(file).get(0);
        logger.info("Added data to IPFS. CID: {}", result.hash.toBase58());
        return result.hash.toBase58();
    }

    /**
     * Fügt Inhalte zum IPFS-Netzwerk aus einem InputStream hinzu.
     *
     * @param inputStream Der InputStream des Inhalts.
     * @param fileName Der Dateiname, der für das Hinzufügen in IPFS verwendet werden soll.
     * @return Der CID (Content Identifier) des hinzugefügten Inhalts.
     * @throws IOException Wenn ein Fehler beim Hinzufügen der Daten auftritt.
     */
    public String add(InputStream inputStream, String fileName) throws IOException {
        NamedStreamable.InputStreamWrapper file = new NamedStreamable.InputStreamWrapper(fileName, inputStream);
        MerkleNode result = ipfs.add(file).get(0);
        logger.info("Added stream data to IPFS. CID: {}", result.hash.toBase58());
        return result.hash.toBase58();
    }

    /**
     * Ruft Inhalte vom IPFS-Netzwerk anhand des CID ab.
     *
     * @param cid Der CID (Content Identifier) des abzurufenden Inhalts.
     * @return Der Inhalt als Byte-Array, oder ein leeres Optional, wenn der Inhalt nicht gefunden wird.
     * @throws IOException Wenn ein Fehler beim Abrufen der Daten auftritt.
     */
    public Optional<byte[]> get(String cid) throws IOException {
        try {
            byte[] data = ipfs.cat(io.ipfs.multihash.Multihash.fromBase58(cid));
            logger.info("Retrieved data from IPFS. CID: {}", cid);
            return Optional.ofNullable(data);
        } catch (IOException e) {
            logger.warn("Could not retrieve data for CID: {}. Error: {}", cid, e.getMessage());
            return Optional.empty();
        }
    }
}
package de.jklein.pharmalink.client.ipfs;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multiaddr.MultiAddress;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Component
public class IpfsClient {

    private static final Logger logger = LoggerFactory.getLogger(IpfsClient.class);

    @Value("${ipfs.host}")
    private String ipfsHost;

    @Value("${ipfs.port}")
    private int ipfsPort;

    @Value("${ipfs.protocol:http}")
    private String ipfsProtocol;

    private IPFS ipfs;

    /**
     * Initialisiert den IPFS-Client nach der Injektion der Abhängigkeiten.
     * Stellt die Verbindung zum IPFS-Daemon her.
     */
    @PostConstruct
    public void init() {
        try {
            // Beispiel: /ip4/127.0.0.1/tcp/5001
            String multiAddr = String.format("/ip4/%s/tcp/%d", ipfsHost, ipfsPort);
            this.ipfs = new IPFS(new MultiAddress(multiAddr));
            ipfs.id(); // test
            logger.info("✅ IPFS client successfully connected to {}:{}", ipfsHost, ipfsPort);
        } catch (IOException e) {
            logger.error("❌ Failed to connect to IPFS daemon at {}:{}. Please ensure the daemon is running.", ipfsHost, ipfsPort, e);
        }
    }

    /**
     * Schließt die IPFS-Verbindung vor dem Beenden der Anwendung (falls erforderlich).
     * io.ipfs.api.IPFS hat keine explizite close-Methode, aber für Robustheit kann man dies hier haben.
     */
    @PreDestroy
    public void destroy() {
        logger.info("IPFS client shutting down.");
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
        MerkleNode result = ipfs.add(file).get(0); // get(0) da es ein einzelnes Element ist
        logger.info("Added data to IPFS. CID: {}", result.hash.toBase58());
        return result.hash.toBase58(); // Konvertiert den Hash zu einem Base58-String (CID)
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
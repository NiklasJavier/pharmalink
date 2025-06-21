package de.jklein.pharmalink.config;

import io.ipfs.api.IPFS;
import io.ipfs.multiaddr.MultiAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@ConfigurationProperties(prefix = "ipfs")
public class IpfsConfig {

    private static final Logger logger = LoggerFactory.getLogger(IpfsConfig.class);

    @Value("${ipfs.host}")
    private String ipfsHost;

    @Value("${ipfs.port}")
    private int ipfsPort;

    /**
     * Definiert und konfiguriert die IPFS-Client-Instanz als Spring Bean.
     * Diese Bean wird automatisch von Spring initialisiert und kann in
     * anderen Komponenten (wie IpfsClient) injiziert werden.
     *
     * @return Eine konfigurierte IPFS-Instanz.
     */
    @Bean
    public IPFS ipfs() {
        try {
            // Verwenden Sie /dns4/ für Hostnamen anstelle von /ip4/
            String multiAddr = String.format("/ip4/%s/tcp/%d", ipfsHost, ipfsPort);
            IPFS ipfsInstance = new IPFS(new MultiAddress(multiAddr));
            ipfsInstance.id(); // Test connection
            logger.info("IPFS Bean successfully initialized and connected to {}:{}", ipfsHost, ipfsPort);
            return ipfsInstance;
        } catch (IOException e) {
            logger.error("Failed to connect to IPFS daemon at {}:{}. Please ensure the daemon is running.", ipfsHost, ipfsPort, e);
            throw new RuntimeException("Failed to initialize IPFS client bean", e);
        }
    }
}
package de.jklein.pharmalink.config;

import io.ipfs.api.IPFS;
import io.ipfs.multiaddr.MultiAddress;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@ConfigurationProperties(prefix = "ipfs")
@Getter
@Setter
public class IpfsConfig {

    private static final Logger logger = LoggerFactory.getLogger(IpfsConfig.class);

    @Value("${ipfs.host}")
    private String ipfsHost;

    @Value("${ipfs.port}")
    private int ipfsPort;

    @Bean
    public IPFS ipfs() {
        try {
            String multiAddr = String.format("/dns4/%s/tcp/%d", ipfsHost, ipfsPort);
            IPFS ipfsInstance = new IPFS(new MultiAddress(multiAddr));
            ipfsInstance.id();
            logger.info("IPFS-Bean erfolgreich initialisiert und mit {}:{} verbunden.", ipfsHost, ipfsPort);
            return ipfsInstance;
        } catch (IOException e) {
            logger.error("Verbindung zum IPFS-Daemon unter {}:{} fehlgeschlagen. Bitte stellen Sie sicher, dass der Daemon läuft.", ipfsHost, ipfsPort, e);
            throw new RuntimeException("Initialisierung des IPFS-Client-Beans fehlgeschlagen.", e);
        }
    }
}
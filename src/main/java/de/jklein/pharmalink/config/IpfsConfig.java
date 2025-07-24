package de.jklein.pharmalink.config;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@ConfigurationProperties(prefix = "ipfs")
@Getter
@Setter
public class IpfsConfig {
    private String host;
    private int port;
    private int timeout;
}
package de.jklein.pharmalink.config;

import java.time.LocalDateTime;
import com.google.gson.Gson; // Import hinzugefügt
import com.google.gson.GsonBuilder; // Import hinzugefügt
import de.jklein.pharmalink.util.LocalDateTimeAdapter; // Import Ihres Adapters

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.Setter;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Hash;
import org.hyperledger.fabric.client.identity.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

@Configuration
@Getter
@Setter
@ConfigurationProperties(prefix = "fabric")
public class FabricConfig {

    private ManagedChannel grpcChannel;

    @Value("${fabric.msp-id}")
    private String mspId;

    @Value("${fabric.crypto-path}")
    private Path cryptoPath;

    @Value("${fabric.cert-path}")
    private Path certDir;

    @Value("${fabric.key-dir-path}")
    private Path keyDirPath;

    @Value("${fabric.tls-cert-path}")
    private Path tlsCertPath;

    @Value("${fabric.peer.endpoint}")
    private String peerEndpoint;

    @Value("${fabric.peer.override-auth}")
    private String overrideAuth;

    @Bean
    public ManagedChannel grpcChannel() throws IOException {
        System.out.println("--> Initialisiere gRPC-Verbindung...");

        Path resolvedTlsCertPath = cryptoPath.resolve(tlsCertPath);

        if (!Files.exists(resolvedTlsCertPath)) {
            throw new IOException("TLS Zertifikats-Datei nicht gefunden unter: " + resolvedTlsCertPath);
        }
        ChannelCredentials credentials = TlsChannelCredentials.newBuilder()
                .trustManager(resolvedTlsCertPath.toFile())
                .build();
        this.grpcChannel = Grpc.newChannelBuilder(peerEndpoint, credentials)
                .overrideAuthority(overrideAuth)
                .build();
        System.out.println("--> gRPC-Verbindung initialisiert.");
        return this.grpcChannel;
    }

    @Bean
    public Identity identity() throws IOException, CertificateException {
        Path certFile = getFirstFilePath(cryptoPath.resolve(certDir));
        try (Reader certReader = Files.newBufferedReader(certFile)) {
            X509Certificate certificate = Identities.readX509Certificate(certReader);
            return new X509Identity(mspId, certificate);
        }
    }

    @Bean
    public Signer signer() throws IOException, InvalidKeyException {
        Path keyFile = getFirstFilePath(cryptoPath.resolve(keyDirPath));
        try (Reader keyReader = Files.newBufferedReader(keyFile)) {
            PrivateKey privateKey = Identities.readPrivateKey(keyReader);
            return Signers.newPrivateKeySigner(privateKey);
        }
    }

    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    @Bean
    public Gateway gateway(ManagedChannel grpcChannel, Identity identity, Signer signer) {
        System.out.println("--> Initialisiere Hyperledger Fabric Gateway...");
        Gateway gateway = Gateway.newInstance()
                .identity(identity)
                .signer(signer)
                .hash(Hash.SHA256)
                .connection(grpcChannel)
                .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
                .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES))
                .connect();

        System.out.println("--> Gateway erfolgreich verbunden.");
        return gateway;
    }

    @Bean
    public Contract contract(Gateway gateway, @Value("${fabric.channel-name}") String channelName, @Value("${fabric.chaincode-name}") String chaincodeName) {
        return gateway.getNetwork(channelName).getContract(chaincodeName);
    }

    @PreDestroy
    public void closeConnection() throws InterruptedException {
        if (this.grpcChannel != null) {
            System.out.println("--> Schließe gRPC-Verbindung...");
            this.grpcChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            System.out.println("--> gRPC-Verbindung geschlossen.");
        }
    }

    private Path getFirstFilePath(Path dirPath) throws IOException {
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            throw new IOException("Verzeichnis nicht gefunden oder ist keine Datei: " + dirPath);
        }
        try (var files = Files.list(dirPath)) {
            return files.findFirst().orElseThrow(() -> new IOException("Keine Datei im Verzeichnis: " + dirPath));
        }
    }
}
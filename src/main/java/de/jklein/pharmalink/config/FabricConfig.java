package de.jklein.pharmalink.config;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import jakarta.annotation.PreDestroy;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
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

/**
 * Konfiguriert und erstellt die Verbindung zum Hyperledger Fabric Netzwerk.
 * Stellt den Gateway und den Contract als verwaltete Spring Beans bereit.
 */
@Configuration
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

    /**
     * Erstellt das Gateway-Objekt als Singleton-Bean.
     * Dies wird nur einmal beim Start der Anwendung ausgeführt.
     * @return Ein verbundenes Gateway-Objekt.
     */
    @Bean
    public Gateway gateway() throws IOException, CertificateException, InvalidKeyException {
        System.out.println("--> Initialisiere Hyperledger Fabric Gateway...");

        Path fullTlsCertPath = cryptoPath.resolve(tlsCertPath);
        this.grpcChannel = newGrpcConnection(fullTlsCertPath);

        Gateway gateway = Gateway.newInstance()
                .identity(newIdentity())
                .signer(newSigner())
                .connection(this.grpcChannel)
                .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .submitOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
                .connect();

        System.out.println("--> Gateway erfolgreich verbunden.");
        return gateway;
    }

    /**
     * Erstellt das Contract-Objekt als Bean, abhängig vom Gateway.
     * Macht den Contract direkt für die Injektion in Services verfügbar.
     */
    @Bean
    public Contract contract(Gateway gateway, @Value("${fabric.channel-name}") String channelName, @Value("${fabric.chaincode-name}") String chaincodeName) {
        return gateway.getNetwork(channelName).getContract(chaincodeName);
    }

    /**
     * Diese Methode wird von Spring automatisch aufgerufen, wenn die Anwendung herunterfährt,
     * um die gRPC-Verbindung sauber zu schließen.
     */
    @PreDestroy
    public void closeConnection() throws InterruptedException {
        if (this.grpcChannel != null) {
            System.out.println("--> Schließe gRPC-Verbindung...");
            this.grpcChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            System.out.println("--> gRPC-Verbindung geschlossen.");
        }
    }

    private ManagedChannel newGrpcConnection(Path tlsCertPath) throws IOException {
        if (!Files.exists(tlsCertPath)) {
            throw new IOException("TLS Zertifikats-Datei nicht gefunden unter: " + tlsCertPath);
        }
        ChannelCredentials credentials = TlsChannelCredentials.newBuilder()
                .trustManager(tlsCertPath.toFile())
                .build();
        return Grpc.newChannelBuilder(peerEndpoint, credentials)
                .overrideAuthority(overrideAuth)
                .build();
    }

    private Identity newIdentity() throws IOException, CertificateException {
        Path certPath = getFirstFilePath(cryptoPath.resolve(certDir));
        try (Reader certReader = Files.newBufferedReader(certPath)) {
            X509Certificate certificate = Identities.readX509Certificate(certReader);
            return new X509Identity(mspId, certificate);
        }
    }

    private Signer newSigner() throws IOException, InvalidKeyException {
        Path keyPath = getFirstFilePath(cryptoPath.resolve(keyDirPath));
        try (Reader keyReader = Files.newBufferedReader(keyPath)) {
            PrivateKey privateKey = Identities.readPrivateKey(keyReader);
            return Signers.newPrivateKeySigner(privateKey);
        }
    }

    private Path getFirstFilePath(Path dirPath) throws IOException {
        if (!Files.exists(dirPath)) {
            throw new IOException("Verzeichnis nicht gefunden: " + dirPath);
        }
        try (var files = Files.list(dirPath)) {
            return files.findFirst().orElseThrow(() -> new IOException("Keine Datei im Verzeichnis: " + dirPath));
        }
    }
}
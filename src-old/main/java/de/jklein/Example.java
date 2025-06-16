package de.jklein;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import org.hyperledger.fabric.client.*;
import org.hyperledger.fabric.client.identity.*;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public final class Example {
    // Statische Konfigurationen, die keine Dateipfade benötigen
    private static final String MSP_ID = "Org1MSP";
    private static final String CHANNEL_NAME = "pharmalink";
    private static final String CHAINCODE_NAME = "pharmalink_chaincode_main";
    private static final String PEER_ENDPOINT = "node.d1.navine.tech:7051";
    private static final String OVERRIDE_AUTH = "peer0.org1.example.com";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(final String[] args) {
        // 1. LogManager-Problem BEHEBEN: Eigenschaft ganz am Anfang setzen
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");

        ManagedChannel grpcChannel = null;
        boolean success = false;

        try {
            // --- Pfade definieren UND validieren ---
            String homeDir = "/Users/niklas/Development/-21 software/fabric-cli/";
            Path cryptoPath = Paths.get(homeDir, "organizations/peerOrganizations/org1.example.com");
            Path certPath = cryptoPath.resolve(Paths.get("users/Admin@org1.example.com/msp/signcerts/cert.pem"));
            Path keyDirPath = cryptoPath.resolve(Paths.get("users/Admin@org1.example.com/msp/keystore"));
            Path tlsCertPath = cryptoPath.resolve(Paths.get("peers/peer0.org1.example.com/tls/ca.crt"));

            // Proaktive Prüfung der Pfade
            if (!Files.exists(certPath)) throw new IOException("Zertifikats-Datei nicht gefunden unter: " + certPath);
            if (!Files.exists(keyDirPath)) throw new IOException("Schlüssel-Verzeichnis nicht gefunden unter: " + keyDirPath);
            if (!Files.exists(tlsCertPath)) throw new IOException("TLS Zertifikats-Datei nicht gefunden unter: " + tlsCertPath);

            // --- Verbindung aufbauen ---
            grpcChannel = newGrpcConnection(tlsCertPath);

            Gateway.Builder builder = Gateway.newInstance()
                    .identity(newIdentity(certPath))
                    .signer(newSigner(keyDirPath))
                    .connection(grpcChannel)
                    .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                    .submitOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS));

            try (Gateway gateway = builder.connect()) {
                System.out.println("--> Erfolgreich mit dem Gateway verbunden.");
                Network network = gateway.getNetwork(CHANNEL_NAME);
                Contract contract = network.getContract(CHAINCODE_NAME);
                String assetId = "asset" + Instant.now().toEpochMilli();

                System.out.println("\n--> Sende Transaktion: CreateAsset, erstellt ein neues Asset mit der ID " + assetId);
                contract.submitTransaction("CreateAsset", assetId, "Aspirin", "100mg", "Bayer", "10.99");
                System.out.println("*** Transaktion 'CreateAsset' erfolgreich committet.");

                System.out.println("\n--> Evaluiere Transaktion: ReadAsset, liest das Asset mit der ID " + assetId);
                byte[] getResult = contract.evaluateTransaction("ReadAsset", assetId);
                System.out.println("*** Abfrage-Ergebnis 'ReadAsset': " + prettyJson(getResult));
            }

            success = true;

        } catch (Throwable t) { // 2. Fehlerbehandlung VERBESSERT: Fängt jetzt alles ab
            System.err.println("!!! EIN FEHLER IST AUFGETRETEN:");
            t.printStackTrace(System.err);
        } finally {
            if (grpcChannel != null) {
                try {
                    grpcChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Eindeutige Statusmeldung ganz am Ende
        System.out.println("\n========================================================");
        if (success) {
            System.out.println("✅ Skript erfolgreich und ohne Fehler abgeschlossen.");
        } else {
            System.out.println("❌ Skript mit Fehlern beendet. Bitte Logs überprüfen.");
        }
        System.out.println("========================================================");
    }

    // --- Hilfsmethoden angepasst, um Pfade als Argumente zu erhalten ---

    private static ManagedChannel newGrpcConnection(Path tlsCertPath) throws IOException {
        ChannelCredentials tlsCredentials = TlsChannelCredentials.newBuilder()
                .trustManager(tlsCertPath.toFile())
                .build();
        return Grpc.newChannelBuilder(PEER_ENDPOINT, tlsCredentials)
                .overrideAuthority(OVERRIDE_AUTH)
                .build();
    }

    private static Identity newIdentity(Path certPath) throws IOException, CertificateException {
        try (Reader certReader = Files.newBufferedReader(certPath)) {
            X509Certificate certificate = Identities.readX509Certificate(certReader);
            return new X509Identity(MSP_ID, certificate);
        }
    }

    private static Signer newSigner(Path keyDirPath) throws IOException, InvalidKeyException {
        Path keyFilePath = getFirstFilePath(keyDirPath);
        try (Reader keyReader = Files.newBufferedReader(keyFilePath)) {
            PrivateKey privateKey = Identities.readPrivateKey(keyReader);
            return Signers.newPrivateKeySigner(privateKey);
        }
    }

    private static Path getFirstFilePath(Path dirPath) throws IOException {
        try (var keyFiles = Files.list(dirPath)) {
            return keyFiles.findFirst().orElseThrow(() -> new IOException("Keine Datei im Verzeichnis gefunden: " + dirPath));
        }
    }

    private static String prettyJson(final byte[] json) {
        var parsedJson = JsonParser.parseString(new String(json, StandardCharsets.UTF_8));
        return gson.toJson(parsedJson);
    }
}
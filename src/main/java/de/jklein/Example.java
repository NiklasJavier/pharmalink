package de.jklein;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.hyperledger.fabric.client.*;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;

public final class Example {
    public static void main(final String[] args)
            throws IOException, CertificateException, InvalidKeyException, GatewayException, CommitException,
            InterruptedException {
        // Create client identity based on X.509 certificate.
        Reader certReader = Files.newBufferedReader(Paths.get("path/to/certificate.pem"));
        X509Certificate certificate = Identities.readX509Certificate(certReader);
        Identity identity = new X509Identity("mspId", certificate);

        // Create signing implementation based on private key.
        Reader keyReader = Files.newBufferedReader(Paths.get("path/to/private-key.pem"));
        PrivateKey privateKey = Identities.readPrivateKey(keyReader);
        Signer signer = Signers.newPrivateKeySigner(privateKey);

        // Create gRPC client connection, which should be shared by all gateway connections to this endpoint.
        ChannelCredentials tlsCredentials = TlsChannelCredentials.newBuilder()
                .trustManager(Paths.get("path/to/tls-CA-certificate.pem").toFile())
                .build();
        ManagedChannel grpcChannel = Grpc.newChannelBuilder("gateway.example.org:1337", tlsCredentials)
                .build();

        // Create a Gateway connection for a specific client identity.
        Gateway.Builder builder = Gateway.newInstance()
                .identity(identity)
                .signer(signer)
                .hash(Hash.SHA256)
                .connection(grpcChannel);

        try (Gateway gateway = builder.connect()) {
            // Obtain smart contract deployed on the network.
            Network network = gateway.getNetwork("channelName");
            Contract contract = network.getContract("chaincodeName");

            // Submit transactions that store state to the ledger.
            byte[] putResult = contract.submitTransaction(
                    "put", "time", LocalDateTime.now().toString());
            System.out.println(new String(putResult, StandardCharsets.UTF_8));

            // Evaluate transactions that query state from the ledger.
            byte[] getResult = contract.evaluateTransaction("get", "time");
            System.out.println(new String(getResult, StandardCharsets.UTF_8));
        } finally {
            grpcChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
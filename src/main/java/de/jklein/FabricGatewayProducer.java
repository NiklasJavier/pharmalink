package de.jklein;

import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@ApplicationScoped
public class FabricGatewayProducer {

    private static final Logger LOG = Logger.getLogger(FabricGatewayProducer.class);

    @ConfigProperty(name = "hlf.mspId")
    String mspId;

    @ConfigProperty(name = "hlf.certPath")
    String certPath;

    @ConfigProperty(name = "hlf.keyPath")
    String keyPath;

    private Gateway gateway;

    @Produces
    @ApplicationScoped
    public Gateway createGateway() throws IOException, CertificateException, InvalidKeyException {
        LOG.info("Initialisiere Hyperledger Fabric Gateway...");

        // 1. Identität (aus dem Zertifikat erstellen)
        Reader certReader = Files.newBufferedReader(Paths.get(certPath));
        X509Certificate certificate = Identities.readX509Certificate(certReader);
        Identity identity = new X509Identity(mspId, certificate);

        // 2. Signer (privaten Schlüssel)
        Reader keyReader = Files.newBufferedReader(Paths.get(keyPath));
        PrivateKey privateKey = Identities.readPrivateKey(keyReader);
        Signer signer = Signers.newPrivateKeySigner(privateKey);

        // 3. Gateway-Verbindung aufbauen. Die `connect()`-Methode ohne Argumente
        // lässt das Gateway die gRPC-Verbindung und deren Lebenszyklus selbst verwalten.
        this.gateway = Gateway.newInstance()
                .identity(identity)
                .signer(signer)
                .connect();

        LOG.info("Hyperledger Fabric Gateway erfolgreich initialisiert.");
        return this.gateway;
    }

    /**
     * Diese Methode wird von Quarkus beim Herunterfahren der Anwendung aufgerufen,
     * um die Gateway-Verbindung sauber zu schließen.
     * @param ev Das ShutdownEvent von Quarkus.
     */
    void onStop(@Observes ShutdownEvent ev) {
        if (this.gateway != null) {
            LOG.info("Schließe Hyperledger Fabric Gateway Verbindung...");
            this.gateway.close();
            LOG.info("Gateway Verbindung erfolgreich geschlossen.");
        }
    }
}
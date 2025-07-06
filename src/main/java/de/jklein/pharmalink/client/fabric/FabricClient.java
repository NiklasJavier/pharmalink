package de.jklein.pharmalink.client.fabric;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import de.jklein.pharmalink.config.FabricConfig;
import de.jklein.pharmalink.domain.audit.GrpcTransaction;
import de.jklein.pharmalink.repository.audit.GrpcTransactionRepository;
import org.hyperledger.fabric.client.*;
import org.hyperledger.fabric.client.identity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class FabricClient {

    private static final Logger logger = LoggerFactory.getLogger(FabricClient.class);
    private static final long BLOCK_EVENT_TIMEOUT = 1000; // ms

    private final Gateway gateway;
    private final Network network;
    private final Contract contract;
    private final Gson gson;
    private final ObjectMapper objectMapper;
    private final GrpcTransactionRepository grpcTransactionRepository;

    @Autowired
    public FabricClient(
            Gateway gateway,
            Identity identity,
            Signer signer,
            FabricConfig fabricConfig,
            Gson gson,
            ObjectMapper objectMapper,
            GrpcTransactionRepository grpcTransactionRepository,
            @Value("${fabric.channel-name}") String channelName,
            @Value("${fabric.chaincode-name}") String chaincodeName
    ) throws IOException, InvalidKeyException, CertificateException {
        this.gateway = gateway;

        this.network = gateway.getNetwork(channelName);
        this.contract = network.getContract(chaincodeName);
        this.gson = gson;
        this.objectMapper = objectMapper;
        this.grpcTransactionRepository = grpcTransactionRepository;

        logger.info("Fabric Client initialized for MSP: {}, User: {}. Connected to Channel: {}, Chaincode: {}",
                fabricConfig.getMspId(),
                identity.getMspId(),
                channelName,
                chaincodeName);
    }

    /**
     * Führt eine Submit-Transaktion auf dem Chaincode aus.
     *
     * @param transactionName Der Name der Chaincode-Transaktion.
     * @param args            Die Argumente der Transaktion.
     * @return Den String-Payload der Transaktion bei Erfolg.
     * @throws GatewayException Bei Fehlern in der Fabric-Kommunikation.
     * @throws CommitException  NEU: Explizite Deklaration, da der Compiler sie erwartet.
     */
    public String submitGenericTransaction(String transactionName, String... args) throws GatewayException, CommitException { // NEU: CommitException hinzugefügt
        LocalDateTime startTime = LocalDateTime.now();
        String transactionArgsJson = convertArgsToJson(args);
        boolean success = false;
        String errorMessage = null;
        String responsePayload = null;

        try {
            byte[] result = contract.submitTransaction(transactionName, args); // Diese Zeile wirft CommitException

            responsePayload = new String(result);
            success = true;
            logger.info("Successfully submitted transaction '{}'. Result: {}", transactionName, responsePayload);
            return responsePayload;
        } catch (GatewayException e) { // Fängt auch CommitException ab
            errorMessage = e.getMessage();
            logger.error("Failed to submit transaction '{}': {}", transactionName, errorMessage);
            throw e;
        } finally {
            logGrpcTransaction(transactionName, transactionArgsJson, startTime, success, errorMessage, responsePayload);
        }
    }

    /**
     * Führt eine Evaluate-Transaktion auf dem Chaincode aus.
     *
     * @param transactionName Der Name der Chaincode-Transaktion.
     * @param args            Die Argumente der Transaktion.
     * @return Den String-Payload der Transaktion bei Erfolg.
     * @throws GatewayException Bei Fehlern in der Fabric-Kommunikation.
     */
    public String evaluateGenericTransaction(String transactionName, String... args) throws GatewayException {
        LocalDateTime startTime = LocalDateTime.now();
        String transactionArgsJson = convertArgsToJson(args);
        boolean success = false;
        String errorMessage = null;
        String responsePayload = null;

        try {
            byte[] result = contract.evaluateTransaction(transactionName, args);
            responsePayload = new String(result);
            success = true;
            logger.info("Successfully evaluated transaction '{}'. Result: {}", transactionName, responsePayload);
            return responsePayload;
        } catch (GatewayException e) {
            errorMessage = e.getMessage();
            logger.error("Failed to evaluate transaction '{}': {}", transactionName, errorMessage);
            throw e;
        } finally {
            logGrpcTransaction(transactionName, transactionArgsJson, startTime, success, errorMessage, responsePayload);
        }
    }

    /**
     * Führt eine Evaluate-Transaktion auf dem Chaincode aus und deserialisiert das Ergebnis.
     *
     * @param transactionName Der Name der Chaincode-Transaktion.
     * @param args            Die Argumente der Transaktion.
     * @param valueType       Der Typ, in den das Ergebnis deserialisiert werden soll.
     * @return Das deserialisierte Objekt oder null, wenn die Transaktion fehlschlägt oder kein Ergebnis liefert.
     * @throws GatewayException Bei Fehlern in der Fabric-Kommunikation.
     */
    public <T> T evaluateTransaction(String transactionName, Object valueType, String... args) throws GatewayException {
        LocalDateTime startTime = LocalDateTime.now();
        String transactionArgsJson = convertArgsToJson(args);
        boolean success = false;
        String errorMessage = null;
        String responsePayload = null;
        T deserializedResult = null;

        try {
            byte[] result = contract.evaluateTransaction(transactionName, args);
            responsePayload = new String(result);
            deserializedResult = gson.fromJson(responsePayload, (Type) valueType);
            success = true;
            logger.info("Successfully evaluated transaction '{}'. Result: {}", transactionName, responsePayload);
            return deserializedResult;
        } catch (GatewayException e) {
            errorMessage = e.getMessage();
            logger.error("Failed to evaluate transaction '{}': {}", transactionName, errorMessage);
            throw e;
        } finally {
            logGrpcTransaction(transactionName, transactionArgsJson, startTime, success, errorMessage, responsePayload);
        }
    }

    private void logGrpcTransaction(String transactionName, String argsJson, LocalDateTime timestamp, boolean successful, String errorMessage, String responsePayload) {
        GrpcTransaction transaction = new GrpcTransaction(transactionName, argsJson, timestamp, successful, errorMessage, responsePayload);
        try {
            grpcTransactionRepository.save(transaction);
            logger.debug("Logged gRPC transaction: {}", transactionName);
        } catch (Exception e) {
            logger.error("Failed to save gRPC transaction log for {}: {}", transactionName, e.getMessage());
        }
    }

    private String convertArgsToJson(String... args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(args);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert transaction arguments to JSON: {}", e.getMessage());
            return "[\"Error converting arguments\"]";
        }
    }

    public Gson getGson() {
        return gson;
    }
}
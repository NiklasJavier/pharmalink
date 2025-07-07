package de.jklein.pharmalink.client.fabric;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonParser; // Hinzugefügt für prettyJson
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
import io.grpc.Status; // Hinzugefügt für Fehlerbehandlung bei Events

import java.io.IOException;
import java.nio.charset.StandardCharsets; // Hinzugefügt für prettyJson
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.lang.reflect.Type; // Beibehalten, da evaluateTransaction weiterhin TypeToken nutzen könnte
import java.util.concurrent.ExecutorService; // Hinzugefügt für asynchrones Event-Listening
import java.util.concurrent.Executors; // Hinzugefügt für asynchrones Event-Listening
import java.util.concurrent.TimeUnit; // Hinzugefügt für ExecutorService Shutdown
import java.util.function.Consumer; // Hinzugefügt für Event-Handler

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
    private final ExecutorService eventExecutor = Executors.newCachedThreadPool(); // Für asynchrones Event-Listening

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
     * @throws CommitException  Explizite Deklaration, da der Compiler sie erwartet.
     */
    public String submitGenericTransaction(String transactionName, String... args) throws GatewayException, CommitException {
        LocalDateTime startTime = LocalDateTime.now();
        String transactionArgsJson = convertArgsToJson(args);
        boolean success = false;
        String errorMessage = null;
        String responsePayload = null;

        try {
            byte[] result = contract.submitTransaction(transactionName, args);

            responsePayload = new String(result);
            success = true;
            logger.info("Successfully submitted transaction '{}'. Result: {}", transactionName, responsePayload);
            return responsePayload;
        } catch (GatewayException e) {
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
     * @param valueType       Der Typ, in den das Ergebnis deserialisiert werden soll.
     * @param args            Die Argumente der Transaktion.
     * @return Das deserialisierte Objekt oder null, wenn die Transaktion fehlschlägt oder kein Ergebnis liefert.
     * @throws GatewayException Bei Fehlern in der Fabric-Kommunikation.
     */
    public <T> T evaluateTransaction(String transactionName, Class<T> valueType, String... args) throws GatewayException {
        LocalDateTime startTime = LocalDateTime.now();
        String transactionArgsJson = convertArgsToJson(args);
        boolean success = false;
        String errorMessage = null;
        String responsePayload = null;
        T deserializedResult = null;

        try {
            byte[] result = contract.evaluateTransaction(transactionName, args);
            responsePayload = new String(result);
            deserializedResult = gson.fromJson(responsePayload, valueType);
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

    /**
     * Startet das Lauschen auf Chaincode-Events und leitet sie an den Handler weiter.
     *
     * @param chaincodeName Der Name des Chaincodes, dessen Events gelauscht werden sollen.
     * @param eventHandler  Ein Consumer, der auf jedes empfangene ChaincodeEvent angewendet wird.
     * @return Ein CloseableIterator<ChaincodeEvent>, der die Event-Session repräsentiert.
     */
    public CloseableIterator<ChaincodeEvent> startChaincodeEventListening(String chaincodeName, Consumer<ChaincodeEvent> eventHandler) {
        logger.info("Starting chaincode event listening for chaincode: {}", chaincodeName);

        var eventIter = network.getChaincodeEvents(chaincodeName);

        eventExecutor.execute(() -> readEvents(eventIter, eventHandler));

        return eventIter;
    }

    /**
     * Liest Chaincode-Events und wendet den bereitgestellten Handler auf jedes Event an.
     *
     * @param eventIter     Der Iterator für Chaincode-Events.
     * @param eventHandler  Der Consumer zum Verarbeiten jedes Events.
     */
    private void readEvents(final CloseableIterator<ChaincodeEvent> eventIter, Consumer<ChaincodeEvent> eventHandler) {
        try {
            eventIter.forEachRemaining(event -> {
                logger.info("<-- Chaincode event received: TxId='{}', EventName='{}', Payload='{}', BlockNum='{}'",
                        event.getTransactionId(), event.getEventName(), prettyJson(event.getPayload()), event.getBlockNumber());
                eventHandler.accept(event); // Leite das Event an den externen Handler weiter
            });
        } catch (GatewayRuntimeException e) {
            if (e.getStatus().getCode() != Status.Code.CANCELLED) {
                logger.error("Error during chaincode event listening: {}", e.getMessage(), e);
            } else {
                logger.info("Chaincode event listening cancelled for chaincode.");
            }
        } catch (Exception e) {
            logger.error("Unexpected error during chaincode event listening: {}", e.getMessage(), e);
        }
    }

    /**
     * Hilfsmethode, um JSON-Payloads schön zu formatieren.
     * @param jsonBytes Das JSON als Byte-Array.
     * @return Formatiertes JSON als String.
     */
    private String prettyJson(final byte[] jsonBytes) {
        if (jsonBytes == null || jsonBytes.length == 0) {
            return "";
        }
        String json = new String(jsonBytes, StandardCharsets.UTF_8);
        try {
            return gson.toJson(JsonParser.parseString(json));
        } catch (Exception e) {
            logger.warn("Failed to pretty print JSON: {}", json);
            return json; // Gib unformatiertes JSON zurück, wenn das Parsen fehlschlägt
        }
    }

    // Optional: Fügen Sie eine Methode hinzu, um den Executor beim Beenden der Anwendung herunterzufahren
    public void shutdownEventExecutor() {
        if (eventExecutor != null) {
            eventExecutor.shutdownNow();
            try {
                if (!eventExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("Event executor did not terminate in time.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for event executor to shut down.", e);
            }
        }
    }
}
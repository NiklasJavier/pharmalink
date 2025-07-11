package de.jklein.pharmalink.client.fabric;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import de.jklein.pharmalink.config.FabricConfig;
import de.jklein.pharmalink.domain.audit.GrpcTransaction;
import de.jklein.pharmalink.repository.audit.GrpcTransactionRepository;
import io.grpc.Status;
import org.hyperledger.fabric.client.*;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class FabricClient {

    private static final Logger logger = LoggerFactory.getLogger(FabricClient.class);
    private static final long BLOCK_EVENT_TIMEOUT = 1000;

    private final Gateway gateway;
    private final Network network;
    private final Contract contract;
    private final Gson gson;
    private final ObjectMapper objectMapper;
    private final GrpcTransactionRepository grpcTransactionRepository;
    private final ExecutorService eventExecutor = Executors.newCachedThreadPool();

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

        logger.info("Fabric-Client initialisiert für MSP: {}, Benutzer: {}. Verbunden mit Kanal: {}, Chaincode: {}",
                fabricConfig.getMspId(),
                identity.getMspId(),
                channelName,
                chaincodeName);
    }

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
            logger.info("Transaktion '{}' erfolgreich übermittelt. Ergebnis: {}", transactionName, responsePayload);
            return responsePayload;
        } catch (GatewayException e) {
            errorMessage = e.getMessage();
            logger.error("Fehler beim Übermitteln der Transaktion '{}': {}", transactionName, errorMessage, e);
            throw e;
        } finally {
            logGrpcTransaction(transactionName, transactionArgsJson, startTime, success, errorMessage, responsePayload);
        }
    }

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
            logger.info("Transaktion '{}' erfolgreich ausgewertet. Ergebnis: {}", transactionName, responsePayload);
            return responsePayload;
        } catch (GatewayException e) {
            errorMessage = e.getMessage();
            logger.error("Fehler beim Auswerten der Transaktion '{}': {}", transactionName, errorMessage, e);
            throw e;
        } finally {
            logGrpcTransaction(transactionName, transactionArgsJson, startTime, success, errorMessage, responsePayload);
        }
    }

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
            logger.info("Transaktion '{}' erfolgreich ausgewertet. Ergebnis: {}", transactionName, responsePayload);
            return deserializedResult;
        } catch (GatewayException e) {
            errorMessage = e.getMessage();
            logger.error("Fehler beim Auswerten der Transaktion '{}': {}", transactionName, errorMessage, e);
            throw e;
        } finally {
            logGrpcTransaction(transactionName, transactionArgsJson, startTime, success, errorMessage, responsePayload);
        }
    }

    private void logGrpcTransaction(String transactionName, String argsJson, LocalDateTime timestamp, boolean successful, String errorMessage, String responsePayload) {
        GrpcTransaction transaction = new GrpcTransaction(transactionName, argsJson, timestamp, successful, errorMessage, responsePayload);
        try {
            grpcTransactionRepository.save(transaction);
            logger.debug("gRPC-Transaktion protokolliert: {}", transactionName);
        } catch (Exception e) {
            logger.error("Fehler beim Speichern des gRPC-Transaktionsprotokolls für {}: {}", transactionName, e.getMessage());
        }
    }

    private String convertArgsToJson(String... args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(args);
        } catch (JsonProcessingException e) {
            logger.error("Fehler beim Konvertieren der Transaktionsargumente in JSON: {}", e.getMessage());
            return "[\"Fehler beim Konvertieren der Argumente\"]";
        }
    }

    public Gson getGson() {
        return gson;
    }

    public CloseableIterator<ChaincodeEvent> startChaincodeEventListening(String chaincodeName, Consumer<ChaincodeEvent> eventHandler) {
        logger.info("Starte Chaincode-Ereignisüberwachung für Chaincode: {}", chaincodeName);
        var eventIter = network.getChaincodeEvents(chaincodeName);
        eventExecutor.execute(() -> readEvents(eventIter, eventHandler));
        return eventIter;
    }

    private void readEvents(final CloseableIterator<ChaincodeEvent> eventIter, Consumer<ChaincodeEvent> eventHandler) {
        try {
            eventIter.forEachRemaining(event -> {
                logger.info("<-- Chaincode-Ereignis empfangen: Tx-ID='{}', Ereignisname='{}', Inhalt='{}', Block-Nr='{}'",
                        event.getTransactionId(), event.getEventName(), prettyJson(event.getPayload()), event.getBlockNumber());
                eventHandler.accept(event);
            });
        } catch (GatewayRuntimeException e) {
            if (e.getStatus().getCode() != Status.Code.CANCELLED) {
                logger.error("Fehler während der Chaincode-Ereignisüberwachung: {}", e.getMessage(), e);
            } else {
                logger.info("Chaincode-Ereignisüberwachung für Chaincode abgebrochen.");
            }
        } catch (Exception e) {
            logger.error("Unerwarteter Fehler während der Chaincode-Ereignisüberwachung: {}", e.getMessage(), e);
        }
    }

    private String prettyJson(final byte[] jsonBytes) {
        if (jsonBytes == null || jsonBytes.length == 0) {
            return "";
        }
        String json = new String(jsonBytes, StandardCharsets.UTF_8);
        try {
            return gson.toJson(JsonParser.parseString(json));
        } catch (Exception e) {
            logger.warn("Fehler beim Formatieren von JSON: {}", json);
            return json;
        }
    }

    public void shutdownEventExecutor() {
        if (eventExecutor != null) {
            eventExecutor.shutdownNow();
            try {
                if (!eventExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("Ereignis-Executor wurde nicht rechtzeitig beendet.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Warten auf das Herunterfahren des Ereignis-Executors wurde unterbrochen.", e);
            }
        }
    }
}
package de.jklein.pharmalink.client.fabric;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import de.jklein.pharmalink.config.FabricConfig;
import de.jklein.pharmalink.domain.audit.GrpcTransaction;
import de.jklein.pharmalink.repository.audit.GrpcTransactionRepository;
import io.grpc.StatusRuntimeException;
import org.hyperledger.fabric.client.*;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class FabricClient {

    private static final Logger logger = LoggerFactory.getLogger(FabricClient.class);

    private final Gateway gateway;
    private final Network network;
    private final Contract contract;
    private final Gson gson;
    private final ObjectMapper objectMapper;
    private final GrpcTransactionRepository grpcTransactionRepository;
    private final ExecutorService eventExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    });

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

    public void startEventListeningWithRetry(String chaincodeName, Supplier<Long> startBlockSupplier, Consumer<ChaincodeEvent> eventHandler) {
        eventExecutor.execute(() -> {
            long backoffMillis = 1000;
            while (!Thread.currentThread().isInterrupted()) {
                long startBlock = startBlockSupplier.get() > 0 ? startBlockSupplier.get() + 1 : 0L;
                try (CloseableIterator<ChaincodeEvent> eventIter = listenFromBlock(chaincodeName, startBlock)) {
                    logger.info("Verbindung zur Chaincode-Ereignisüberwachung hergestellt, starte bei Block {}.", startBlock);
                    backoffMillis = 1000;

                    while (eventIter.hasNext()) {
                        eventHandler.accept(eventIter.next());
                    }
                } catch (StatusRuntimeException e) {
                    logger.error("gRPC-Verbindungsfehler bei der Ereignisüberwachung: {}. Versuche erneute Verbindung in {}s.", e.getStatus(), backoffMillis / 1000);
                    sleep(backoffMillis);
                    backoffMillis = Math.min(backoffMillis * 2, 20000);
                } catch (Exception e) {
                    logger.error("Unerwarteter Fehler bei der Ereignisüberwachung. Breche ab.", e);
                    break;
                }
            }
            logger.warn("Event-Listening-Schleife wurde beendet.");
        });
    }

    private CloseableIterator<ChaincodeEvent> listenFromBlock(String chaincodeName, long startBlock) {
        ChaincodeEventsRequest request = network.newChaincodeEventsRequest(chaincodeName)
                .startBlock(startBlock)
                .build();
        return request.getEvents();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdownEventExecutor() {
        eventExecutor.shutdownNow();
        try {
            if (!eventExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Event-Executor wurde nicht innerhalb von 5 Sekunden beendet.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
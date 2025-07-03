package de.jklein.pharmalink.client.fabric;

import com.google.gson.Gson;
import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.GatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays; // Hinzugefügter Import

@Service
public class FabricClient {

    private static final Logger logger = LoggerFactory.getLogger(FabricClient.class);

    @Autowired
    private Contract contract;

    private final Gson gson = new Gson();

    /**
     * Führt eine schreibende Transaktion aus, die ein Objekt als JSON-String erwartet.
     * @param functionName Der Name der Chaincode-Funktion.
     * @param assetId Die ID des Assets.
     * @param asset Das zu erstellende Asset-Objekt.
     */
    public void submitCreateTransaction(String functionName, String assetId, Object asset) throws Exception {
        String assetJson = gson.toJson(asset);
        logger.info("--> Submitting Create Transaction: '{}' for asset ID '{}'", functionName, assetId);
        // NEU: Loggt den genauen JSON-Payload, der gesendet wird.
        logger.info("    Payload JSON: {}", assetJson);
        contract.submitTransaction(functionName, assetId, assetJson);
        logger.info("*** Create Transaction '{}' committed successfully", functionName);
    }

    /**
     * Führt eine lesende Abfrage aus und wandelt das Ergebnis in ein Java-Objekt um.
     * @param functionName Der Name der Chaincode-Funktion.
     * @param assetId Die ID des zu lesenden Assets.
     * @param clazz Die Klasse, in die das JSON-Ergebnis umgewandelt werden soll.
     * @return Das deserialisierte Objekt.
     */
    public <T> T evaluateTransaction(String functionName, String assetId, Class<T> clazz) throws GatewayException {
        logger.info("--> Evaluating Transaction: '{}' for asset ID '{}'", functionName, assetId);
        byte[] result = contract.evaluateTransaction(functionName, assetId);
        if (result == null || result.length == 0) {
            logger.warn("<-- Transaction '{}' returned no result.", functionName);
            return null;
        }
        String assetJson = new String(result, StandardCharsets.UTF_8);
        // NEU: Loggt das empfangene JSON, bevor es umgewandelt wird.
        logger.info("<-- Received Result JSON: {}", assetJson);
        return gson.fromJson(assetJson, clazz);
    }

    /**
     * Führt eine generische, schreibende Transaktion aus.
     * @param functionName Der Name der Chaincode-Funktion.
     * @param args Die Argumente für die Chaincode-Funktion.
     * @return Der JSON-String der Transaktionsantwort.
     * @throws GatewayException Bei Fehlern im Gateway.
     * @throws CommitException Bei Fehlern beim Commit der Transaktion.
     */
    public String submitGenericTransaction(String functionName, String... args) throws GatewayException, CommitException {
        // GEÄNDERT: Bessere Lesbarkeit der Argumente im Log.
        logger.info("--> Submitting Generic Transaction: '{}' with args: {}", functionName, Arrays.toString(args));
        byte[] resultBytes = contract.submitTransaction(functionName, args);
        String result = new String(resultBytes, StandardCharsets.UTF_8);
        logger.info("*** Generic Transaction '{}' committed successfully. Result: {}", functionName, result);
        return result;
    }

    /**
     * Führt eine generische, lesende Abfrage mit mehreren Argumenten aus.
     * @param functionName Der Name der Chaincode-Funktion.
     * @param args Die Argumente für die Chaincode-Funktion.
     * @return Der JSON-String des Abfrageergebnisses.
     * @throws GatewayException Bei Fehlern im Gateway.
     */
    public String evaluateGenericTransaction(String functionName, String... args) throws GatewayException {
        logger.info("--> Evaluating Generic Transaction: '{}' with args: {}", functionName, Arrays.toString(args));
        byte[] result = contract.evaluateTransaction(functionName, args);
        if (result == null || result.length == 0) {
            logger.warn("<-- Transaction '{}' returned no result.", functionName);
            return "[]"; // Leeres JSON-Array zurückgeben, um null-Fehler zu vermeiden
        }
        String resultJson = new String(result, StandardCharsets.UTF_8);
        logger.info("<-- Received Result JSON: {}", resultJson);
        return resultJson;
    }

    public Gson getGson() {
        return gson;
    }
}
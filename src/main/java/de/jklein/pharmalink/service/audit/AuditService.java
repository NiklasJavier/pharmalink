package de.jklein.pharmalink.service.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jklein.pharmalink.domain.audit.ApiTransaction;
import de.jklein.pharmalink.domain.audit.GrpcTransaction;
import de.jklein.pharmalink.domain.audit.LoginAttempt; // NEU: Import LoginAttempt
import de.jklein.pharmalink.repository.audit.ApiTransactionRepository;
import de.jklein.pharmalink.repository.audit.GrpcTransactionRepository;
import de.jklein.pharmalink.repository.audit.LoginAttemptRepository; // NEU: Import LoginAttemptRepository
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"); // Für gRPC (volle Zeit)
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Für API (nur Datum) und Login

    private final ApiTransactionRepository apiTransactionRepository;
    private final GrpcTransactionRepository grpcTransactionRepository;
    private final LoginAttemptRepository loginAttemptRepository; // NEU: Repository für Login-Versuche
    private final ObjectMapper objectMapper;

    @Autowired
    public AuditService(ApiTransactionRepository apiTransactionRepository, GrpcTransactionRepository grpcTransactionRepository, LoginAttemptRepository loginAttemptRepository, ObjectMapper objectMapper) { // NEU: LoginAttemptRepository im Konstruktor
        this.apiTransactionRepository = apiTransactionRepository;
        this.grpcTransactionRepository = grpcTransactionRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Ruft alle API-Transaktionen ab, sortiert nach ID absteigend.
     * @return Eine Liste von ApiTransaction-Objekten.
     */
    public List<ApiTransaction> getAllApiTransactionsOrderedByIdDesc() {
        List<ApiTransaction> transactions = apiTransactionRepository.findAll();
        transactions.sort(Comparator.comparing(ApiTransaction::getId).reversed());
        return transactions;
    }

    /**
     * Ruft alle API-Transaktionen ab (sortiert nach ID absteigend) und gibt sie als flachen JSON-String zurück.
     * Die Einträge innerhalb der Liste sind nach ID absteigend sortiert und der Timestamp steht an erster Stelle (nur Datum).
     * @return Ein JSON-String der Transaktionen.
     */
    public String getAllApiTransactionsAsJson() {
        List<ApiTransaction> allTransactions = getAllApiTransactionsOrderedByIdDesc();

        List<Map<String, Object>> jsonCompatibleList = allTransactions.stream()
                .map(tx -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("timestamp", tx.getTimestamp().format(DATE_TIME_FORMATTER)); // Nur Datum für API-Transaktionen
                    map.put("httpMethod", tx.getHttpMethod());
                    map.put("responseStatus", tx.getResponseStatus());
                    map.put("successful", tx.isSuccessful());
                    map.put("url", tx.getUrl());
                    map.put("username", tx.getUsername());
                    return map;
                })
                .collect(Collectors.toList());

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonCompatibleList);
        } catch (JsonProcessingException e) {
            logger.error("Fehler beim Konvertieren der API-Transaktionen in JSON: {}", e.getMessage(), e);
            return "{\"error\": \"Fehler beim Erstellen der Transaktionsübersicht.\"}";
        }
    }

    /**
     * Ruft alle gRPC-Transaktionen ab, sortiert nach ID absteigend.
     * @return Eine Liste von GrpcTransaction-Objekten.
     */
    public List<GrpcTransaction> getAllGrpcTransactionsOrderedByIdDesc() {
        List<GrpcTransaction> transactions = grpcTransactionRepository.findAll();
        transactions.sort(Comparator.comparing(GrpcTransaction::getId).reversed());
        return transactions;
    }

    /**
     * Ruft alle gRPC-Transaktionen ab (sortiert nach ID absteigend) und gibt sie als flachen JSON-String zurück.
     * Die Einträge innerhalb der Liste sind nach ID absteigend sortiert und die ID steht an erster Stelle.
     * @return Ein JSON-String der gRPC-Transaktionen.
     */
    public String getAllGrpcTransactionsAsJson() {
        List<GrpcTransaction> allTransactions = getAllGrpcTransactionsOrderedByIdDesc();

        List<Map<String, Object>> jsonCompatibleList = allTransactions.stream()
                .map(tx -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("timestamp", tx.getTimestamp().format(DATE_TIME_FORMATTER)); // Timestamp hier mit voller Zeit
                    map.put("transactionName", tx.getTransactionName());
                    map.put("successful", tx.isSuccessful());
                    map.put("transactionArgs", tx.getTransactionArgs());
                    return map;
                })
                .collect(Collectors.toList());

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonCompatibleList);
        } catch (JsonProcessingException e) {
            logger.error("Fehler beim Konvertieren der gRPC-Transaktionen in JSON: {}", e.getMessage(), e);
            return "{\"error\": \"Fehler beim Erstellen der gRPC-Transaktionsübersicht.\"}";
        }
    }

    /**
     * Ruft alle Login-Versuche ab, sortiert nach ID absteigend.
     * @return Eine Liste von LoginAttempt-Objekten.
     */
    public List<LoginAttempt> getAllLoginAttemptsOrderedByIdDesc() {
        List<LoginAttempt> attempts = loginAttemptRepository.findAll();
        attempts.sort(Comparator.comparing(LoginAttempt::getId).reversed()); // Sortieren nach ID absteigend
        return attempts;
    }

    /**
     * Ruft alle Login-Versuche ab (sortiert nach ID absteigend) und gibt sie als flachen JSON-String zurück.
     * Die Einträge innerhalb der Liste sind nach ID absteigend sortiert und der Timestamp steht an erster Stelle.
     * @return Ein JSON-String der Login-Versuche.
     */
    public String getAllLoginAttemptsAsJson() {
        List<LoginAttempt> allAttempts = getAllLoginAttemptsOrderedByIdDesc();

        List<Map<String, Object>> jsonCompatibleList = allAttempts.stream()
                .map(attempt -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("timestamp", attempt.getTimestamp().format(DATE_TIME_FORMATTER)); // Timestamp mit voller Zeit
                    map.put("username", attempt.getUsername());
                    map.put("successful", attempt.isSuccessful());
                    map.put("ipAddress", attempt.getIpAddress());
                    map.put("authenticationType", attempt.getAuthenticationType());
                    return map;
                })
                .collect(Collectors.toList());

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonCompatibleList);
        } catch (JsonProcessingException e) {
            logger.error("Fehler beim Konvertieren der Login-Versuche in JSON: {}", e.getMessage(), e);
            return "{\"error\": \"Fehler beim Erstellen der Login-Übersicht.\"}";
        }
    }
}
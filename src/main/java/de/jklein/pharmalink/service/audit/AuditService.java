package de.jklein.pharmalink.service.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jklein.pharmalink.domain.audit.ApiTransaction;
import de.jklein.pharmalink.domain.audit.GrpcTransaction;
import de.jklein.pharmalink.domain.audit.LoginAttempt;
import de.jklein.pharmalink.repository.audit.ApiTransactionRepository;
import de.jklein.pharmalink.repository.audit.GrpcTransactionRepository;
import de.jklein.pharmalink.repository.audit.LoginAttemptRepository;
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
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ApiTransactionRepository apiTransactionRepository;
    private final GrpcTransactionRepository grpcTransactionRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuditService(ApiTransactionRepository apiTransactionRepository, GrpcTransactionRepository grpcTransactionRepository, LoginAttemptRepository loginAttemptRepository, ObjectMapper objectMapper) {
        this.apiTransactionRepository = apiTransactionRepository;
        this.grpcTransactionRepository = grpcTransactionRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.objectMapper = objectMapper;
    }

    public List<ApiTransaction> getAllApiTransactionsOrderedByIdDesc() {
        return apiTransactionRepository.findAll().stream()
                .sorted(Comparator.comparing(ApiTransaction::getId).reversed())
                .collect(Collectors.toList());
    }

    public String getAllApiTransactionsAsJson() {
        List<ApiTransaction> allTransactions = getAllApiTransactionsOrderedByIdDesc();

        List<Map<String, Object>> jsonCompatibleList = allTransactions.stream()
                .map(tx -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("timestamp", tx.getTimestamp().format(DATE_TIME_FORMATTER));
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

    public List<GrpcTransaction> getAllGrpcTransactionsOrderedByIdDesc() {
        return grpcTransactionRepository.findAll().stream()
                .sorted(Comparator.comparing(GrpcTransaction::getId).reversed())
                .collect(Collectors.toList());
    }



    public String getAllGrpcTransactionsAsJson() {
        List<GrpcTransaction> allTransactions = getAllGrpcTransactionsOrderedByIdDesc();

        List<Map<String, Object>> jsonCompatibleList = allTransactions.stream()
                .map(tx -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("timestamp", tx.getTimestamp().format(DATE_TIME_FORMATTER));
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

    public List<LoginAttempt> getAllLoginAttemptsOrderedByIdDesc() {
        return loginAttemptRepository.findAll().stream()
                .sorted(Comparator.comparing(LoginAttempt::getId).reversed())
                .collect(Collectors.toList());
    }

    public String getAllLoginAttemptsAsJson() {
        List<LoginAttempt> allAttempts = getAllLoginAttemptsOrderedByIdDesc();

        List<Map<String, Object>> jsonCompatibleList = allAttempts.stream()
                .map(attempt -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("timestamp", attempt.getTimestamp().format(DATE_TIME_FORMATTER));
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
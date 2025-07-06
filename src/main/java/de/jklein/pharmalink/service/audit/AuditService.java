package de.jklein.pharmalink.service.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jklein.pharmalink.domain.audit.ApiTransaction;
import de.jklein.pharmalink.repository.audit.ApiTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter; // Für Formatierung des Timestamps
import java.util.Comparator;
import java.util.LinkedHashMap; // Wichtig für Beibehaltung der Einfügereihenfolge (falls nötig, sonst HashMap)
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");


    private final ApiTransactionRepository apiTransactionRepository;
    private final ObjectMapper objectMapper; // Für JSON-Konvertierung

    @Autowired
    public AuditService(ApiTransactionRepository apiTransactionRepository, ObjectMapper objectMapper) {
        this.apiTransactionRepository = apiTransactionRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Ruft alle API-Transaktionen ab, sortiert nach ID absteigend.
     * @return Eine Liste von ApiTransaction-Objekten.
     */
    public List<ApiTransaction> getAllApiTransactionsOrderedByIdDesc() {
        // FindAll gibt eine unsortierte Liste zurück, daher manuell sortieren.
        // Oder JpaRepository um findByOrderByIdDesc() erweitern, wenn dies oft benötigt wird.
        List<ApiTransaction> transactions = apiTransactionRepository.findAll();
        transactions.sort(Comparator.comparing(ApiTransaction::getId).reversed()); // Sortieren nach ID absteigend
        return transactions;
    }

    /**
     * Ruft alle API-Transaktionen ab, gruppiert sie nach URL und gibt das Ergebnis als JSON-String zurück.
     * Die Einträge innerhalb jeder URL-Gruppe sind nach ID absteigend sortiert.
     * @return Ein JSON-String der gruppierten Transaktionen.
     */
    public String getGroupedApiTransactionsByUrlAsJson() {
        List<ApiTransaction> allTransactions = getAllApiTransactionsOrderedByIdDesc(); // Bereits sortiert

        // Gruppierung nach URL
        Map<String, List<ApiTransaction>> groupedByUrl = allTransactions.stream()
                .collect(Collectors.groupingBy(ApiTransaction::getUrl, LinkedHashMap::new, Collectors.toList()));
        // LinkedHashMap::new bewahrt die Reihenfolge der URL-Einträge, wie sie zuerst erscheinen.

        // Das ApiTransaction-Objekt enthält LocalDateTime. Wir müssen es für JSON korrekt formatieren.
        // Erstellen Sie eine Map<String, Object> Struktur, die dem gewünschten JSON-Output entspricht
        Map<String, List<Map<String, Object>>> jsonCompatibleMap = new LinkedHashMap<>();
        groupedByUrl.forEach((url, transactions) -> {
            List<Map<String, Object>> transactionMaps = transactions.stream()
                    .map(tx -> {
                        Map<String, Object> map = new LinkedHashMap<>(); // Beibehaltung der Reihenfolge der Felder
                        map.put("id", tx.getId());
                        map.put("httpMethod", tx.getHttpMethod());
                        map.put("responseStatus", tx.getResponseStatus());
                        map.put("successful", tx.isSuccessful());
                        map.put("timestamp", tx.getTimestamp().format(DATE_TIME_FORMATTER)); // LocalDateTime formatieren
                        map.put("url", tx.getUrl());
                        map.put("username", tx.getUsername());
                        map.put("requestBody", tx.getRequestBody()); // Kann null sein
                        map.put("errorMessage", tx.getErrorMessage()); // Kann null sein
                        return map;
                    })
                    .collect(Collectors.toList());
            jsonCompatibleMap.put(url, transactionMaps);
        });

        try {
            // Konvertiere die gruppierte Map in einen JSON-String
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonCompatibleMap);
        } catch (JsonProcessingException e) {
            logger.error("Fehler beim Konvertieren der API-Transaktionen in JSON: {}", e.getMessage(), e);
            return "{\"error\": \"Fehler beim Erstellen der Transaktionsübersicht.\"}";
        }
    }
}
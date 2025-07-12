package de.jklein.pharmalinkclient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jklein.pharmalinkclient.config.BackendConfig;
import de.jklein.pharmalinkclient.dto.CreateMedikamentRequestDto;
import de.jklein.pharmalinkclient.dto.MedikamentFilterCriteriaDto;
import de.jklein.pharmalinkclient.dto.MedikamentResponseDto;
import de.jklein.pharmalinkclient.dto.UpdateMedikamentRequestDto;
import de.jklein.pharmalinkclient.dto.UpdateMedicationStatusRequestDto;
import de.jklein.pharmalinkclient.security.UserSession;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SpringComponent
@UIScope
public class MedikamentService {

    private static final Logger log = LoggerFactory.getLogger(MedikamentService.class);

    private final RestTemplate restTemplate;
    private final BackendConfig backendConfig;
    private final UserSession userSession;

    public MedikamentService(BackendConfig backendConfig, UserSession userSession) {
        this.restTemplate = new RestTemplate();
        this.backendConfig = backendConfig;
        this.userSession = userSession;
    }

    private HttpEntity<String> createHttpEntityWithJwt() {
        HttpHeaders headers = new HttpHeaders();
        String jwt = userSession.getJwt();
        if (jwt != null && !jwt.isEmpty()) {
            headers.setBearerAuth(jwt);
        } else {
            log.warn("Kein JWT-Token in der UserSession verfügbar. Authentifizierung erforderlich.");
            return new HttpEntity<>(headers);
        }
        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> createHttpEntityWithJwtAndBody(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jwt = userSession.getJwt();
        if (jwt != null && !jwt.isEmpty()) {
            headers.setBearerAuth(jwt);
        } else {
            log.warn("Kein JWT-Token in der UserSession verfügbar. Authentifizierung erforderlich.");
        }
        return new HttpEntity<>(body, headers);
    }

    public List<MedikamentResponseDto> searchMedikamente(MedikamentFilterCriteriaDto criteria) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(backendConfig.getBaseUrl() + "/v1/search/medikamente");

        if (criteria.getSearchTerm() != null && !criteria.getSearchTerm().isEmpty()) {
            uriBuilder.queryParam("query", criteria.getSearchTerm());
        }
        if (criteria.getStatusFilter() != null && !criteria.getStatusFilter().isEmpty() && !criteria.getStatusFilter().equals("Ohne Filter")) {
            uriBuilder.queryParam("status", criteria.getStatusFilter());
        }
        if (criteria.isFilterByCurrentActor()) {
            uriBuilder.queryParam("ownedByMe", true);
        }

        String url = uriBuilder.toUriString();
        log.info("Sende GET-Anfrage für Medikamente an URL: {}", url);
        HttpEntity<String> entity = createHttpEntityWithJwt();

        try {
            ResponseEntity<MedikamentResponseDto[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    MedikamentResponseDto[].class
            );
            if (response.getBody() != null) {
                List<MedikamentResponseDto> result = Arrays.asList(response.getBody());
                log.info("Erfolgreich {} Medikamenten-Einträge erhalten.", result.size());
                return result;
            }
        } catch (HttpClientErrorException e) {
            log.error("Fehler beim Suchen/Filtern der Medikamente: {} {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Allgemeiner Fehler beim Suchen/Filtern der Medikamente: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public MedikamentResponseDto getMedikamentById(String medId) {
        String url = backendConfig.getBaseUrl() + "/v1/medications/" + medId;
        log.info("Sende GET-Anfrage für Medikament ID {} an URL: {}", medId, url);
        HttpEntity<String> entity = createHttpEntityWithJwt();

        try {
            ResponseEntity<MedikamentResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    MedikamentResponseDto.class
            );
            log.info("Medikament mit ID {} erfolgreich abgerufen.", medId);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Fehler beim Laden des Medikaments mit ID '{}': {} {}", medId, e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Allgemeiner Fehler beim Laden des Medikaments mit ID: {}", e.getMessage(), e);
        }
        return null;
    }

    public MedikamentResponseDto createMedikament(CreateMedikamentRequestDto request) {
        String url = backendConfig.getBaseUrl() + "/v1/medications";
        log.info("Sende POST-Anfrage zum Erstellen eines Medikaments an URL: {}", url);
        HttpEntity<CreateMedikamentRequestDto> entity = createHttpEntityWithJwtAndBody(request);

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
            String jsonOutput = mapper.writeValueAsString(request);
            log.debug("----- Ausgehendes JSON für Medikament-Erstellung -----\n{}\n--------------------------------------------------", jsonOutput);

            ResponseEntity<MedikamentResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    MedikamentResponseDto.class
            );
            log.info("Medikament erfolgreich erstellt. Response Body: {}", response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Fehler beim Erstellen des Medikaments: {} {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return null;
        } catch (Exception e) {
            log.error("Allgemeiner Fehler beim Erstellen des Medikaments: {}", e.getMessage(), e);
            return null;
        }
    }

    public MedikamentResponseDto updateMedikament(String medId, UpdateMedikamentRequestDto request) {
        String url = backendConfig.getBaseUrl() + "/v1/medications/" + medId;
        log.info("Sende PUT-Anfrage zum Aktualisieren von Medikament ID {} an URL: {}", medId, url);
        HttpEntity<UpdateMedikamentRequestDto> entity = createHttpEntityWithJwtAndBody(request);

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
            String jsonOutput = mapper.writeValueAsString(request);
            log.debug("----- Ausgehendes JSON für Medikament-Update -----\n{}\n--------------------------------------------------", jsonOutput);

            ResponseEntity<MedikamentResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    MedikamentResponseDto.class
            );
            log.info("Medikament mit ID {} erfolgreich aktualisiert. Response Body: {}", medId, response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Fehler beim Aktualisieren des Medikaments mit ID '{}': {} {}", medId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return null;
        } catch (Exception e) {
            log.error("Allgemeiner Fehler beim Aktualisieren des Medikaments: {}", e.getMessage(), e);
        }
        return null;
    }

    public boolean approveMedication(String medId, UpdateMedicationStatusRequestDto request) {
        String url = backendConfig.getBaseUrl() + "/v1/medications/" + medId + "/approval";
        log.info("Sende POST-Anfrage zum Genehmigen/Ablehnen von Medikament ID {} an URL: {}", medId, url);
        HttpEntity<UpdateMedicationStatusRequestDto> entity = createHttpEntityWithJwtAndBody(request);

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Void.class
            );
            log.info("Medikament mit ID {} erfolgreich genehmigt/abgelehnt.", medId);
            return true;
        } catch (HttpClientErrorException e) {
            log.error("Fehler beim Genehmigen/Ablehnen des Medikaments mit ID '{}': {} {}", medId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return false;
        } catch (Exception e) {
            log.error("Allgemeiner Fehler beim Genehmigen/Ablehnen des Medikaments: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteMedikamentIfNoUnits(String medId) {
        String url = backendConfig.getBaseUrl() + "/v1/medications/" + medId + "/conditional-delete";
        log.info("Sende DELETE-Anfrage zum bedingten Löschen von Medikament ID {} an URL: {}", medId, url);
        HttpEntity<String> entity = createHttpEntityWithJwt();

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );
            log.info("Medikament mit ID {} erfolgreich bedingt gelöscht.", medId);
            return true;
        } catch (HttpClientErrorException e) {
            log.error("Fehler beim bedingten Löschen des Medikaments mit ID '{}': {} {}", medId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return false;
        } catch (Exception e) {
            log.error("Allgemeiner Fehler beim bedingten Löschen des Medikaments: {}", e.getMessage(), e);
            return false;
        }
    }
}
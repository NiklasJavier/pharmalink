// UnitService.java
package de.jklein.pharmalinkclient.service;

import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import de.jklein.pharmalinkclient.config.BackendConfig;
import de.jklein.pharmalinkclient.dto.CreateUnitsRequestDto;
import de.jklein.pharmalinkclient.dto.TransferUnitRequestDto;
import de.jklein.pharmalinkclient.dto.UnitResponseDto;
import de.jklein.pharmalinkclient.dto.AddTemperatureReadingRequestDto;
import de.jklein.pharmalinkclient.dto.TransferUnitRangeRequestDto;
import de.jklein.pharmalinkclient.dto.DeleteUnitsRequestDto;
import de.jklein.pharmalinkclient.security.UserSession;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SpringComponent
@UIScope
public class UnitService {

    private final RestTemplate restTemplate;
    private final BackendConfig backendConfig;
    private final UserSession userSession;

    public UnitService(BackendConfig backendConfig, UserSession userSession) {
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
            System.err.println("Kein JWT-Token in der UserSession verfügbar. Authentifizierung erforderlich.");
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
            System.err.println("Kein JWT-Token in der UserSession verfügbar. Authentifizierung erforderlich.");
        }
        return new HttpEntity<>(body, headers);
    }

    public UnitResponseDto getUnitById(String unitId) {
        String url = backendConfig.getBaseUrl() + "/v1/units/" + unitId;
        HttpEntity<String> entity = createHttpEntityWithJwt();

        try {
            ResponseEntity<UnitResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    UnitResponseDto.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.err.println("Fehler beim Laden der Einheit mit ID '" + unitId + "': " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Allgemeiner Fehler beim Laden der Einheit mit ID: " + e.getMessage());
        }
        return null;
    }

    public boolean createUnitsForMedication(String medId, CreateUnitsRequestDto request) {
        String url = backendConfig.getBaseUrl() + "/v1/units/" + medId + "/units";
        HttpEntity<CreateUnitsRequestDto> entity = createHttpEntityWithJwtAndBody(request);

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Void.class
            );
            System.out.println("Einheit(en) für Medikament ID '" + medId + "' erfolgreich erstellt.");
            return true;
        } catch (HttpClientErrorException e) {
            System.err.println("Fehler beim Erstellen von Einheiten für Medikament ID '" + medId + "': " + e.getStatusCode() + " " + e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            System.err.println("Allgemeiner Fehler beim Erstellen von Einheiten: " + e.getMessage());
            return false;
        }
    }

    public UnitResponseDto transferUnit(String unitId, TransferUnitRequestDto request) {
        String url = backendConfig.getBaseUrl() + "/v1/units/" + unitId + "/transfer";
        HttpEntity<TransferUnitRequestDto> entity = createHttpEntityWithJwtAndBody(request);

        try {
            ResponseEntity<UnitResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    UnitResponseDto.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.err.println("Fehler beim Transfer der Einheit mit ID '" + unitId + "': " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Allgemeiner Fehler beim Transfer der Einheit: " + e.getMessage());
        }
        return null;
    }

    public UnitResponseDto addTemperatureReading(String unitId, AddTemperatureReadingRequestDto request) {
        String url = backendConfig.getBaseUrl() + "/v1/units/" + unitId + "/temperature-readings";
        HttpEntity<AddTemperatureReadingRequestDto> entity = createHttpEntityWithJwtAndBody(request);

        try {
            ResponseEntity<UnitResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    UnitResponseDto.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.err.println("Fehler beim Hinzufügen von Temperaturmessung zu Einheit '" + unitId + "': " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Allgemeiner Fehler beim Hinzufügen von Temperaturmessung: " + e.getMessage());
        }
        return null;
    }

    public Map<String, List<UnitResponseDto>> getUnitsGroupedByCharge(String medId) {
        String url = UriComponentsBuilder.fromHttpUrl(backendConfig.getBaseUrl() + "/v1/units/" + medId + "/units-by-charge")
                .toUriString(); // Sicherstellen, dass die URL korrekt ist
        HttpEntity<String> entity = createHttpEntityWithJwt();

        System.out.println("DEBUG: Requesting charge counts from URL: " + url);
        try {
            ResponseEntity<Map<String, List<UnitResponseDto>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, List<UnitResponseDto>>>() {}
            );
            if (response.getBody() != null) {
                System.out.println("DEBUG: Received charge counts response: " + response.getBody());
                return response.getBody();
            }
        } catch (HttpClientErrorException e) {
            System.err.println("Fehler beim Laden der Chargenanzahl für Medikament ID '" + medId + "': " + e.getStatusCode() + " " + e.getResponseBodyAsString());
            System.err.println("Backend Response Body: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Allgemeiner Fehler beim Laden der Chargenanzahl für Medikament ID: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("DEBUG: Returning empty map for charge counts.");
        return Collections.emptyMap();
    }

    public boolean transferUnitRange(TransferUnitRangeRequestDto request) {
        String url = backendConfig.getBaseUrl() + "/v1/units/transfer-range";
        HttpEntity<TransferUnitRangeRequestDto> entity = createHttpEntityWithJwtAndBody(request);

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Void.class
            );
            System.out.println("Einheiten-Bereich erfolgreich transferiert.");
            return true;
        } catch (HttpClientErrorException e) {
            System.err.println("Fehler beim Transfer des Einheiten-Bereichs: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            System.err.println("Allgemeiner Fehler beim Transfer des Einheiten-Bereichs: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUnit(String unitId) {
        String url = backendConfig.getBaseUrl() + "/v1/units/" + unitId;
        HttpEntity<String> entity = createHttpEntityWithJwt();

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );
            System.out.println("Einheit mit ID " + unitId + " erfolgreich gelöscht.");
            return true;
        } catch (HttpClientErrorException e) {
            System.err.println("Fehler beim Löschen der Einheit mit ID '" + unitId + "': " + e.getStatusCode() + " " + e.getResponseBodyAsString());
            return false;
        }
    }

    public boolean deleteUnitsInBatch(DeleteUnitsRequestDto request) {
        String url = backendConfig.getBaseUrl() + "/v1/units/batch";
        HttpEntity<DeleteUnitsRequestDto> entity = createHttpEntityWithJwtAndBody(request);

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );
            System.out.println("Einheiten im Batch erfolgreich gelöscht.");
            return true;
        } catch (HttpClientErrorException e) {
            System.err.println("Fehler beim Löschen von Einheiten im Batch: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            System.err.println("Allgemeiner Fehler beim Löschen von Einheiten im Batch: " + e.getMessage());
            return false;
        }
    }

    public List<UnitResponseDto> searchUnitsByCharge(String query) {
        String url = UriComponentsBuilder.fromHttpUrl(backendConfig.getBaseUrl() + "/v1/search/units")
                .queryParam("query", query)
                .toUriString();
        HttpEntity<String> entity = createHttpEntityWithJwt();

        System.out.println("DEBUG UnitService: Requesting units by charge from URL: " + url); 
        try {
            ResponseEntity<UnitResponseDto[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    UnitResponseDto[].class
            );
            if (response.getBody() != null) {
                List<UnitResponseDto> result = Arrays.asList(response.getBody());
                System.out.println("DEBUG UnitService: Received " + result.size() + " units.");
                return result;
            }
        } catch (HttpClientErrorException e) {
            System.err.println("Fehler beim Suchen von Einheiten nach Charge '" + query + "': " + e.getStatusCode() + " " + e.getResponseBodyAsString());
            System.err.println("Backend Response Body: " + e.getResponseBodyAsString()); // NEU
        } catch (Exception e) {
            System.err.println("Allgemeiner Fehler beim Suchen von Einheiten nach Charge: " + e.getMessage());
            e.printStackTrace(); // NEU
        }
        System.out.println("DEBUG UnitService: Returning empty list for units by charge."); // NEU
        return Collections.emptyList();
    }

    public Map<String, Integer> getChargeCountsForMedication(String medId) {
        String url = backendConfig.getBaseUrl() + "/v1/units/medications/" + medId + "/charge-counts";
        HttpEntity<String> entity = createHttpEntityWithJwt();

        System.out.println("DEBUG: Requesting charge counts from URL: " + url);
        try {
            ResponseEntity<Map<String, Integer>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Integer>>() {}
            );
            if (response.getBody() != null) {
                System.out.println("DEBUG: Received charge counts response: " + response.getBody());
                return response.getBody();
            }
        } catch (HttpClientErrorException e) {
            System.err.println("Fehler beim Laden der Chargenanzahl für Medikament ID '" + medId + "': " + e.getStatusCode() + " " + e.getResponseBodyAsString());
            System.err.println("Backend Response Body: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Allgemeiner Fehler beim Laden der Chargenanzahl für Medikament ID: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("DEBUG: Returning empty map for charge counts.");
        return Collections.emptyMap();
    }

    public List<UnitResponseDto> getMyUnits() {
        String url = backendConfig.getBaseUrl() + "/v1/units/mine";
        HttpEntity<String> entity = createHttpEntityWithJwt();

        try {
            ResponseEntity<List<UnitResponseDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<UnitResponseDto>>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.err.println("Fehler beim Laden der eigenen Einheiten: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Allgemeiner Fehler beim Laden der eigenen Einheiten: " + e.getMessage());
        }
        return Collections.emptyList();
    }
}
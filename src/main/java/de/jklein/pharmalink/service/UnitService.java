package de.jklein.pharmalink.service;

import com.google.gson.reflect.TypeToken;
import de.jklein.pharmalink.api.dto.CreateUnitsRequestDto;
import de.jklein.pharmalink.api.dto.UnitResponseDto;
import de.jklein.pharmalink.api.mapper.UnitMapper;
import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.client.ipfs.IpfsClient;
import de.jklein.pharmalink.domain.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UnitService {

    private static final Logger logger = LoggerFactory.getLogger(UnitService.class);

    private final FabricClient fabricClient;
    private final IpfsClient ipfsClient;
    private final UnitMapper unitMapper;

    @Autowired
    public UnitService(FabricClient fabricClient, IpfsClient ipfsClient, UnitMapper unitMapper) {
        this.fabricClient = fabricClient;
        this.ipfsClient = ipfsClient;
        this.unitMapper = unitMapper;
    }

    /**
     * Ruft eine einzelne Unit anhand ihrer ID ab und reichert sie mit Daten aus IPFS an.
     * @param unitId Die ID der abzurufenden Unit.
     * @return Ein Optional, das das angereicherte Unit-DTO enthält.
     */
    public Optional<UnitResponseDto> getEnrichedUnitById(String unitId) {
        try {
            // 1. Rufe die Unit von der Blockchain ab. Dein Chaincode hat die Funktion 'queryUnitById'.
            Unit unit = fabricClient.evaluateTransaction("queryUnitById", unitId, Unit.class);
            if (unit == null) {
                return Optional.empty();
            }

            // 2. Wandle das Domain-Objekt in ein DTO um.
            UnitResponseDto dto = unitMapper.toDto(unit);

            // 3. Prüfe und löse den ipfsLink auf.
            final String originalIpfsLink = unit.getIpfsLink();
            if (originalIpfsLink != null && !originalIpfsLink.isBlank()) {
                try {
                    final String cleanHash = originalIpfsLink.replace("ipfs://", "").trim();
                    if (!cleanHash.isEmpty()) {
                        ipfsClient.get(cleanHash).ifPresent(ipfsBytes -> {
                            try {
                                String jsonContent = new String(ipfsBytes, StandardCharsets.UTF_8);
                                Type dataType = new TypeToken<Map<String, Object>>() {}.getType();
                                Map<String, Object> ipfsData = fabricClient.getGson().fromJson(jsonContent, dataType);
                                dto.setIpfsData(ipfsData);
                            } catch (Exception parseEx) {
                                logger.error("Konnte IPFS JSON-Inhalt für Unit-CID '{}' nicht parsen.", cleanHash, parseEx);
                            }
                        });
                    }
                } catch (Exception ipfsEx) {
                    logger.warn("Fehler beim Abrufen von IPFS-Daten für Unit-Link '{}'. Fehler: {}.", originalIpfsLink, ipfsEx.getMessage());
                }
            }

            return Optional.of(dto);
        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der Unit mit ID '{}'", unitId, e);
            return Optional.empty();
        }
    }

    /**
     * Erstellt eine angegebene Anzahl von Units für ein Medikament.
     * Wenn 'ipfsData' im Request vorhanden ist, wird es in IPFS gespeichert und
     * der resultierende Hash an die Fabric-Transaktion übergeben.
     *
     * @param medId Die ID des Medikaments, für das Units erstellt werden.
     * @param requestDto Das DTO mit den Details (Anzahl, Charge, IPFS-Daten).
     * @return Eine Liste der neu erstellten Units als DTOs.
     * @throws Exception bei Fehlern während der Transaktion.
     */
    public List<UnitResponseDto> createUnitsForMedication(String medId, CreateUnitsRequestDto requestDto) throws Exception {
        String ipfsHash = "";

        // 1. Prüfen, ob ipfsData vorhanden ist und in IPFS speichern.
        if (requestDto.getIpfsData() != null && !requestDto.getIpfsData().isEmpty()) {
            logger.info("Processing 'ipfsData' for new units...");
            String ipfsJson = fabricClient.getGson().toJson(requestDto.getIpfsData());
            byte[] ipfsBytes = ipfsJson.getBytes(StandardCharsets.UTF_8);
            ipfsHash = ipfsClient.add(ipfsBytes);
            logger.info("Successfully created IPFS entry for new units. CID: {}", ipfsHash);
        }

        // 2. Die Chaincode-Funktion 'createUnits' mit den korrekten Argumenten aufrufen.
        String resultJson = fabricClient.submitGenericTransaction(
                "createUnits",
                medId,
                requestDto.getChargeBezeichnung(),
                String.valueOf(requestDto.getAnzahl()),
                ipfsHash
        );

        // 3. Die JSON-Antwort (Array von Units) verarbeiten.
        Type listType = new TypeToken<List<Unit>>() {}.getType();
        List<Unit> createdUnits = fabricClient.getGson().fromJson(resultJson, listType);

        // 4. In DTOs umwandeln und zurückgeben.
        return unitMapper.toDtoList(createdUnits);
    }

    /**
     * Ruft alle Units für eine Medikamenten-ID ab, reichert jede Unit mit IPFS-Daten an
     * und gruppiert das Ergebnis anschließend nach ihrer Charge.
     *
     * @param medId Die ID des Medikaments.
     * @return Eine Map, bei der der Schlüssel die Chargenbezeichnung und der Wert die Liste der zugehörigen,
     * angereicherten Units ist.
     */
    public Map<String, List<UnitResponseDto>> getUnitsByMedIdGroupedByCharge(String medId) {
        try {
            // 1. Rufe die flache Liste aller Units von der Blockchain ab.
            String resultJson = fabricClient.evaluateGenericTransaction("queryUnitsByMedId", medId);
            Type listType = new TypeToken<List<Unit>>() {}.getType();
            List<Unit> units = fabricClient.getGson().fromJson(resultJson, listType);

            // 2. Erstelle eine angereicherte Liste von DTOs, indem durch jedes Element iteriert wird.
            List<UnitResponseDto> enrichedDtos = units.stream()
                    .map(unit -> {
                        // Führe für jede einzelne Unit die Anreicherungslogik aus.
                        // Wir rufen die bereits existierende Methode für eine einzelne Unit wieder auf.
                        // Das verhindert doppelten Code.
                        return this.getEnrichedUnitById(unit.getUnitId())
                                .orElse(null); // Gib null zurück, falls eine Unit nicht gefunden/verarbeitet werden kann
                    })
                    .filter(Objects::nonNull) // Filtere eventuelle Null-Ergebnisse heraus
                    .collect(Collectors.toList());

            // 3. Gruppiere die NUN angereicherte Liste der DTOs nach dem Feld 'chargeBezeichnung'.
            return enrichedDtos.stream()
                    .collect(Collectors.groupingBy(UnitResponseDto::getChargeBezeichnung));

        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der gruppierten Units für medId '{}'", medId, e);
            return Collections.emptyMap();
        }
    }

    /**
     * Führt den Transfer einer Unit an einen neuen Besitzer durch.
     *
     * @param unitId Die ID der zu übertragenden Unit.
     * @param newOwnerActorId Die ID des neuen Besitzers.
     * @return Ein DTO der aktualisierten Unit.
     * @throws Exception bei Fehlern, z.B. wenn der Aufrufer nicht der Besitzer ist.
     */
    public UnitResponseDto transferUnit(String unitId, String newOwnerActorId) throws Exception {
        // 1. Rufe die Chaincode-Funktion 'transferUnit' auf.
        // Die Autorisierung (Besitzprüfung) findet im Chaincode statt.
        String resultJson = fabricClient.submitGenericTransaction(
                "transferUnit",
                unitId,
                newOwnerActorId
        );

        // 2. Wandle die aktualisierte Unit in ein Domain-Objekt um.
        Unit updatedUnit = fabricClient.getGson().fromJson(resultJson, Unit.class);

        // 3. Mappe das Domain-Objekt zu einem DTO für die API-Antwort.
        return unitMapper.toDto(updatedUnit);
    }

    /**
     * Fügt einen neuen Temperaturmesswert zu einer bestehenden Unit hinzu.
     *
     * @param unitId Die ID der Unit.
     * @param temperature Der gemessene Temperaturwert.
     * @param timestamp Der Zeitstempel der Messung (z.B. im ISO 8601 Format).
     * @return Ein DTO der aktualisierten Unit.
     * @throws Exception bei Fehlern, z.B. wenn der Aufrufer nicht der Besitzer ist.
     */
    public UnitResponseDto addTemperatureReading(String unitId, String temperature, String timestamp) throws Exception {
        // 1. Rufe die Chaincode-Funktion 'addTemperatureReading' auf.
        // Die Autorisierungsprüfung (Besitzprüfung) findet im Chaincode statt.
        String resultJson = fabricClient.submitGenericTransaction(
                "addTemperatureReading",
                unitId,
                temperature,
                timestamp
        );

        // 2. Wandle die aktualisierte Unit in ein Domain-Objekt um.
        Unit updatedUnit = fabricClient.getGson().fromJson(resultJson, Unit.class);

        // 3. Mappe das Domain-Objekt zu einem DTO für die API-Antwort.
        return unitMapper.toDto(updatedUnit);
    }
}
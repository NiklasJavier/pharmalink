package de.jklein.pharmalink.service;

import com.google.gson.reflect.TypeToken;
import de.jklein.pharmalink.api.dto.ActorResponseDto;
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

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
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
            Unit unit = fabricClient.evaluateTransaction("queryUnitById", unitId, Unit.class);
            if (unit == null) {
                return Optional.empty();
            }

            UnitResponseDto dto = unitMapper.toDto(unit);

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
     * NEU HINZUGEFÜGT: Ruft alle Units ab, die einem bestimmten Besitzer gehören, und reichert sie an.
     * Diese Methode wird vom AppController für die Dashboard-Ansicht von Großhändlern und Apotheken benötigt.
     *
     * @param ownerId Die ID des Besitzers.
     * @return Eine Liste der angereicherten Units, die dem Besitzer gehören.
     */
    public List<UnitResponseDto> getUnitsByOwner(String ownerId) {
        try {
            // 1. Rufe die flache Liste aller Units für den Besitzer von der Blockchain ab.
            String resultJson = fabricClient.evaluateGenericTransaction("queryUnitsByOwner", ownerId);
            Type listType = new TypeToken<List<Unit>>() {}.getType();
            List<Unit> units = fabricClient.getGson().fromJson(resultJson, listType);

            // 2. Erstelle eine angereicherte Liste von DTOs, indem durch jedes Element iteriert wird.
            return units.stream()
                    .map(unit -> this.getEnrichedUnitById(unit.getUnitId())
                            .orElse(null)) // Rufe die bestehende Anreicherungsmethode wieder auf
                    .filter(Objects::nonNull) // Filtere eventuelle Null-Ergebnisse heraus
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der Units für Besitzer '{}'", ownerId, e);
            return Collections.emptyList();
        }
    }

    public List<UnitResponseDto> createUnitsForMedication(String medId, CreateUnitsRequestDto requestDto) throws Exception {
        String ipfsHash = "";

        if (requestDto.getIpfsData() != null && !requestDto.getIpfsData().isEmpty()) {
            logger.info("Processing 'ipfsData' for new units...");
            String ipfsJson = fabricClient.getGson().toJson(requestDto.getIpfsData());
            byte[] ipfsBytes = ipfsJson.getBytes(StandardCharsets.UTF_8);
            ipfsHash = ipfsClient.add(ipfsBytes);
            logger.info("Successfully created IPFS entry for new units. CID: {}", ipfsHash);
        }

        String resultJson = fabricClient.submitGenericTransaction(
                "createUnits",
                medId,
                requestDto.getChargeBezeichnung(),
                String.valueOf(requestDto.getAnzahl()),
                ipfsHash
        );

        Type listType = new TypeToken<List<Unit>>() {}.getType();
        List<Unit> createdUnits = fabricClient.getGson().fromJson(resultJson, listType);

        return unitMapper.toDtoList(createdUnits);
    }

    public Map<String, List<UnitResponseDto>> getUnitsByMedIdGroupedByCharge(String medId) {
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryUnitsByMedId", medId);
            Type listType = new TypeToken<List<Unit>>() {}.getType();
            List<Unit> units = fabricClient.getGson().fromJson(resultJson, listType);

            List<UnitResponseDto> enrichedDtos = units.stream()
                    .map(unit -> this.getEnrichedUnitById(unit.getUnitId())
                            .orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return enrichedDtos.stream()
                    .collect(Collectors.groupingBy(UnitResponseDto::getChargeBezeichnung));

        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der gruppierten Units für medId '{}'", medId, e);
            return Collections.emptyMap();
        }
    }

    public UnitResponseDto transferUnit(String unitId, String newOwnerActorId) throws Exception {
        String resultJson = fabricClient.submitGenericTransaction(
                "transferUnit",
                unitId,
                newOwnerActorId
        );
        Unit updatedUnit = fabricClient.getGson().fromJson(resultJson, Unit.class);
        return unitMapper.toDto(updatedUnit);
    }

    public UnitResponseDto addTemperatureReading(String unitId, String temperature, String timestamp) throws Exception {
        String resultJson = fabricClient.submitGenericTransaction(
                "addTemperatureReading",
                unitId,
                temperature,
                timestamp
        );
        Unit updatedUnit = fabricClient.getGson().fromJson(resultJson, Unit.class);
        return unitMapper.toDto(updatedUnit);
    }
}
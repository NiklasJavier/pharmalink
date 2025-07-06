package de.jklein.pharmalink.service;

import com.google.gson.reflect.TypeToken;
import de.jklein.pharmalink.api.dto.CreateUnitsRequestDto;
import de.jklein.pharmalink.api.mapper.UnitMapper;
import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.client.ipfs.IpfsClient;
import de.jklein.pharmalink.domain.Unit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * Gibt ein Optional des angereicherten Domain-Objekts zurück.
     * @param unitId Die ID der abzurufenden Unit.
     * @return Ein Optional, das das angereicherte Unit-Domain-Objekt enthält.
     */
    public Optional<Unit> getEnrichedUnitById(String unitId) {
        try {
            Unit unit = fabricClient.evaluateTransaction("queryUnitById", unitId, Unit.class);
            if (unit == null) {
                return Optional.empty();
            }

            final String originalIpfsLink = unit.getIpfsLink();
            if (originalIpfsLink != null && !originalIpfsLink.isBlank()) {
                final String cleanHash = originalIpfsLink.replace("ipfs://", "").trim();

                logger.info("Resolving IPFS link '{}' (cleaned to '{}') for unit '{}'",
                        originalIpfsLink, cleanHash, unitId);

                try {
                    // NEU: Direkte Verwendung von ipfsClient.getObject mit Type-Parameter
                    // Dies nutzt intern den Datenbank-Cache und die Deserialisierungslogik
                    Type dataType = new TypeToken<Map<String, Object>>() {}.getType(); // TypeToken außerhalb der Lambda
                    Map<String, Object> ipfsData = ipfsClient.getObject(cleanHash, dataType);

                    if (ipfsData != null) {
                        unit.setIpfsData(ipfsData);
                        logger.info("Successfully attached IPFS data for CID '{}'", cleanHash);
                    } else {
                        logger.warn("IPFS content for CID '{}' was null after fetching for unit '{}'.", cleanHash, unitId);
                    }
                } catch (IOException e) { // IOException fangen, da getObject sie werfen kann
                    logger.error("Fehler beim Abrufen oder Deserialisieren von IPFS-Inhalt für CID '{}': {}", cleanHash, e.getMessage(), e);
                    // Den Fehler loggen, aber die Unit-Anreicherung fortsetzen
                }
            }

            return Optional.of(unit);
        } catch (Exception e) { // Hier bleibt Exception, um Fabric-Fehler und andere abzufangen
            logger.error("Fehler beim Abrufen der Unit mit ID '{}': {}", unitId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Erstellt Units für ein Medikament. Gibt eine Liste der erstellten Domain-Objekte zurück.
     * @param medId Die ID des Medikaments, für das Units erstellt werden.
     * @param requestDto Das DTO mit den Details (Anzahl, Charge, IPFS-Daten).
     * @return Eine Liste der neu erstellten Units als Domain-Objekte.
     * @throws Exception bei Fehlern während der Transaktion.
     */
    public List<Unit> createUnitsForMedication(String medId, CreateUnitsRequestDto requestDto) throws Exception {
        String ipfsHash = "";

        if (requestDto.getIpfsData() != null && !requestDto.getIpfsData().isEmpty()) {
            logger.info("Processing 'ipfsData' for new units...");
            String ipfsJson = fabricClient.getGson().toJson(requestDto.getIpfsData());
            // NEU: Verwende ipfsClient.addObject, das bereits Caching-Logik enthält
            ipfsHash = ipfsClient.addObject(ipfsJson);
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

        return createdUnits;
    }

    /**
     * Ruft alle Units für eine Medikamenten-ID ab, reichert jede Unit mit IPFS-Daten an
     * und gruppiert das Ergebnis anschließend nach ihrer Charge.
     * Gibt eine Map von Listen der angereicherten Domain-Objekte zurück.
     * @param medId Die ID des Medikaments.
     * @return Eine Map, bei der der Schlüssel die Chargenbezeichnung und der Wert die Liste der zugehörigen,
     * angereicherten Units (Domain-Objekte) ist.
     */
    public Map<String, List<Unit>> getUnitsByMedIdGroupedByCharge(String medId) {
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryUnitsByMedId", medId);
            Type listType = new TypeToken<List<Unit>>() {}.getType();
            List<Unit> units = fabricClient.getGson().fromJson(resultJson, listType);

            List<Unit> enrichedUnits = units.stream()
                    .flatMap(unit -> {
                        try {
                            return this.getEnrichedUnitById(unit.getUnitId()).stream();
                        } catch (Exception e) { // Exception muss hier gefangen werden
                            logger.warn("Fehler beim Anreichern von Unit '{}' für Medikament '{}'. Fehler: {}. Dieser Eintrag wird übersprungen.",
                                    unit.getUnitId(), medId, e.getMessage());
                            return Stream.empty();
                        }
                    })
                    .collect(Collectors.toList());

            return enrichedUnits.stream()
                    .collect(Collectors.groupingBy(Unit::getChargeBezeichnung));

        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der gruppierten Units für medId '{}': {}", medId, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Führt den Transfer einer Unit an einen neuen Besitzer durch.
     * Gibt das aktualisierte Domain-Objekt zurück.
     * @param unitId Die ID der zu übertragenden Unit.
     * @param newOwnerActorId Die ID des neuen Besitzers.
     * @return Das aktualisierte Unit-Domain-Objekt.
     * @throws Exception bei Fehlern, z.B. wenn der Aufrufer nicht der Besitzer ist.
     */
    public Unit transferUnit(String unitId, String newOwnerActorId) throws Exception {
        String resultJson = fabricClient.submitGenericTransaction(
                "transferUnit",
                unitId,
                newOwnerActorId
        );
        Unit updatedUnit = fabricClient.getGson().fromJson(resultJson, Unit.class);
        return updatedUnit;
    }

    /**
     * Fügt einen neuen Temperaturmesswert zu einer bestehenden Unit hinzu.
     * Gibt das aktualisierte Domain-Objekt zurück.
     * @param unitId Die ID der Unit.
     * @param temperature Der gemessene Temperaturwert.
     * @param timestamp Der Zeitstempel der Messung (z.B. im ISO 8601 Format).
     * @return Das aktualisierte Unit-Domain-Objekt.
     * @throws Exception bei Fehlern, z.B. wenn der Aufrufer nicht der Besitzer ist.
     */
    public Unit addTemperatureReading(String unitId, String temperature, String timestamp) throws Exception {
        String resultJson = fabricClient.submitGenericTransaction(
                "addTemperatureReading",
                unitId,
                temperature,
                timestamp
        );
        Unit updatedUnit = fabricClient.getGson().fromJson(resultJson, Unit.class);
        return updatedUnit;
    }

    /**
     * Ruft alle Units ab, die einem bestimmten Eigentümer gehören, und reichert sie mit IPFS-Daten an.
     * Gibt eine Liste der angereicherten Domain-Objekte zurück.
     * Dies ersetzt die vorherige getUnitsByOwner-Methode.
     * @param ownerActorId Die ActorId des Eigentümers.
     * @return Eine Liste von angereicherten Unit-Domain-Objekten.
     */
    public List<Unit> getEnrichedUnitsByOwner(String ownerActorId) {
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryUnitsByOwner", ownerActorId);
            Type listType = new TypeToken<List<Unit>>() {}.getType();
            List<Unit> units = fabricClient.getGson().fromJson(resultJson, listType);

            return units.stream()
                    .flatMap(unit -> {
                        try {
                            return this.getEnrichedUnitById(unit.getUnitId()).stream();
                        } catch (Exception e) { // Exception muss hier gefangen werden
                            logger.warn("Fehler beim Anreichern von Unit '{}' für Eigentümer '{}'. Fehler: {}. Dieser Eintrag wird übersprungen.",
                                    unit.getUnitId(), ownerActorId, e.getMessage());
                            return Stream.empty();
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der Units für Eigentümer '{}': {}", ownerActorId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
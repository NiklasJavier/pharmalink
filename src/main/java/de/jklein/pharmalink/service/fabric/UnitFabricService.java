package de.jklein.pharmalink.service.fabric;

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
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class UnitFabricService {

    private static final Logger logger = LoggerFactory.getLogger(UnitFabricService.class);

    private final FabricClient fabricClient;
    private final IpfsClient ipfsClient;
    private final UnitMapper unitMapper;

    @Autowired
    public UnitFabricService(FabricClient fabricClient, IpfsClient ipfsClient, UnitMapper unitMapper) {
        this.fabricClient = fabricClient;
        this.ipfsClient = ipfsClient;
        this.unitMapper = unitMapper;
    }

    /**
     * Ruft eine einzelne Unit anhand ihrer ID ab und reichert sie mit Daten aus IPFS an.
     * @param unitId Die ID der abzurufenden Unit.
     * @return Ein Optional, das das angereicherte Unit-Objekt enthält.
     */
    public Optional<Unit> getEnrichedUnitById(String unitId) {
        try {
            Unit unit = fabricClient.evaluateTransaction("queryUnitById", Unit.class, unitId);
            return Optional.ofNullable(enrichSingleUnitWithIpfs(unit));
        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der Unit mit ID '{}': {}", unitId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Erstellt Units für ein Medikament und gibt eine Liste der erstellten Domain-Objekte zurück.
     */
    public List<Unit> createUnitsForMedication(String medId, CreateUnitsRequestDto requestDto) throws Exception {
        String ipfsHash = "";
        if (requestDto.getIpfsData() != null && !requestDto.getIpfsData().isEmpty()) {
            logger.info("Processing 'ipfsData' for new units...");
            String ipfsJson = fabricClient.getGson().toJson(requestDto.getIpfsData());
            ipfsHash = ipfsClient.addObject(ipfsJson);
            logger.info("Successfully created IPFS entry for new units. CID: {}", ipfsHash);
        }

        String resultJson = fabricClient.submitGenericTransaction(
                "createUnits", medId, requestDto.getChargeBezeichnung(),
                String.valueOf(requestDto.getAnzahl()), ipfsHash
        );

        Type listType = new TypeToken<List<Unit>>() {}.getType();
        return fabricClient.getGson().fromJson(resultJson, listType);
    }

    /**
     * **OPTIMIERT**: Ruft alle Units für eine Medikamenten-ID ab, reichert sie parallel an und gruppiert sie nach Charge.
     * @param medId Die ID des Medikaments.
     * @return Eine Map, die angereicherte Units nach ihrer Charge gruppiert.
     */
    public Map<String, List<Unit>> getUnitsByMedIdGroupedByCharge(String medId) {
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryUnitsByMedId", medId);
            Type listType = new TypeToken<List<Unit>>() {}.getType();
            List<Unit> units = fabricClient.getGson().fromJson(resultJson, listType);

            List<Unit> enrichedUnits = enrichUnitList(units);

            return enrichedUnits.stream()
                    .collect(Collectors.groupingBy(Unit::getChargeBezeichnung));
        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der gruppierten Units für medId '{}': {}", medId, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * **OPTIMIERT**: Ruft alle Units für einen Eigentümer ab und reichert sie parallel an.
     * @param ownerActorId Die ActorId des Eigentümers.
     * @return Eine Liste von angereicherten Unit-Objekten.
     */
    public List<Unit> getUnitsByOwner(String ownerActorId) {
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryUnitsByOwner", ownerActorId);
            Type listType = new TypeToken<List<Unit>>() {}.getType();
            List<Unit> units = fabricClient.getGson().fromJson(resultJson, listType);
            return enrichUnitList(units);
        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der Units für Eigentümer '{}': {}", ownerActorId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public Unit transferUnit(String unitId, String newOwnerActorId) throws Exception {
        String resultJson = fabricClient.submitGenericTransaction("transferUnit", unitId, newOwnerActorId);
        return fabricClient.getGson().fromJson(resultJson, Unit.class);
    }

    public Unit addTemperatureReading(String unitId, String temperature, String timestamp) throws Exception {
        String resultJson = fabricClient.submitGenericTransaction("addTemperatureReading", unitId, temperature, timestamp);
        return fabricClient.getGson().fromJson(resultJson, Unit.class);
    }

    /**
     * Private Hilfsmethode, die eine Liste von Units parallel mit IPFS-Daten anreichert.
     */
    private List<Unit> enrichUnitList(List<Unit> units) {
        if (units == null || units.isEmpty()) {
            return Collections.emptyList();
        }
        List<CompletableFuture<Unit>> futures = units.stream()
                .map(unit -> CompletableFuture.supplyAsync(() -> enrichSingleUnitWithIpfs(unit)))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Private Hilfsmethode, die ein einzelnes Unit-Objekt mit IPFS-Daten anreichert.
     */
    private Unit enrichSingleUnitWithIpfs(Unit unit) {
        if (unit == null) {
            return null;
        }

        if (StringUtils.hasText(unit.getIpfsLink())) {
            final String cleanHash = unit.getIpfsLink().replace("ipfs://", "").trim();
            try {
                if (!cleanHash.isEmpty()) {
                    Type dataType = new TypeToken<Map<String, Object>>() {}.getType();
                    Map<String, Object> ipfsData = ipfsClient.getObject(cleanHash, dataType);
                    unit.setIpfsData(ipfsData);
                    logger.debug("Successfully enriched Unit {} with IPFS data.", unit.getUnitId());
                }
            } catch (IOException e) {
                logger.warn("Could not fetch or parse IPFS data for Unit {}: {}", unit.getUnitId(), e.getMessage());
            }
        }
        return unit;
    }

    public void deleteUnit(String unitId) throws Exception {
        logger.debug("Sende 'deleteUnit' Transaktion für ID: {}", unitId);
        fabricClient.submitGenericTransaction("deleteUnit", unitId);
        logger.info("Charge {} erfolgreich zur Löschung eingereicht.", unitId);
    }

    public void deleteUnits(List<String> unitIds) throws Exception {
        logger.debug("Sende 'deleteUnits' Transaktion für {} Chargen.", unitIds.size());
        // Die Liste der IDs muss als einzelner JSON-String-Parameter übergeben werden
        String unitIdsJson = fabricClient.getGson().toJson(unitIds);
        fabricClient.submitGenericTransaction("deleteUnits", unitIdsJson);
        logger.info("{} Chargen erfolgreich zur Löschung eingereicht.", unitIds.size());
    }

    public String transferUnitRange(String medId, String chargeBezeichnung, int start, int end, String newOwnerId) throws Exception {
        logger.debug("Sende 'transferUnitRange' Transaktion für Bereich {}-{}", start, end);
        // Zeitstempel wird im Backend generiert
        String timestamp = java.time.Instant.now().toString();

        String result = fabricClient.submitGenericTransaction(
                "transferUnitRange",
                medId,
                chargeBezeichnung,
                String.valueOf(start),
                String.valueOf(end),
                newOwnerId,
                timestamp
        );
        logger.info("Chargenbereich erfolgreich zur Übertragung eingereicht.");
        return result;
    }
}
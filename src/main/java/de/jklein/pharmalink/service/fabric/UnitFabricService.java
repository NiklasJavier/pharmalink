package de.jklein.pharmalink.service.fabric;

import com.google.gson.reflect.TypeToken;
import de.jklein.pharmalink.api.dto.CreateUnitsRequestDto;
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
import java.time.Instant;
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

    @Autowired
    public UnitFabricService(FabricClient fabricClient, IpfsClient ipfsClient) {
        this.fabricClient = fabricClient;
        this.ipfsClient = ipfsClient;
    }

    public Optional<Unit> getEnrichedUnitById(String unitId) {
        try {
            Unit unit = fabricClient.evaluateTransaction("queryUnitById", Unit.class, unitId);
            return Optional.ofNullable(enrichSingleUnitWithIpfs(unit));
        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der Einheit mit ID '{}': {}", unitId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public List<Unit> createUnitsForMedication(String medId, CreateUnitsRequestDto requestDto) throws Exception {
        String ipfsHash = "";
        if (requestDto.getIpfsData() != null && !requestDto.getIpfsData().isEmpty()) {
            String ipfsJson = fabricClient.getGson().toJson(requestDto.getIpfsData());
            ipfsHash = ipfsClient.addObject(ipfsJson);
        }
        String resultJson = fabricClient.submitGenericTransaction(
                "createUnits", medId, requestDto.getChargeBezeichnung(),
                String.valueOf(requestDto.getAnzahl()), ipfsHash
        );
        Type listType = new TypeToken<List<Unit>>() {}.getType();
        return fabricClient.getGson().fromJson(resultJson, listType);
    }

    public List<Unit> getUnitsByOwner(String ownerActorId) {
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryUnitsByOwner", ownerActorId);
            Type listType = new TypeToken<List<Unit>>() {}.getType();
            List<Unit> units = fabricClient.getGson().fromJson(resultJson, listType);
            return enrichUnitList(units);
        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der Einheiten für Eigentümer '{}': {}", ownerActorId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public Map<String, List<Unit>> getUnitsByMedIdGroupedByCharge(String medId) {
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryUnitsByMedId", medId);
            Type listType = new TypeToken<List<Unit>>() {}.getType();
            List<Unit> units = fabricClient.getGson().fromJson(resultJson, listType);
            List<Unit> enrichedUnits = enrichUnitList(units);
            return enrichedUnits.stream().collect(Collectors.groupingBy(Unit::getChargeBezeichnung));
        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der gruppierten Einheiten für Medikamenten-ID '{}': {}", medId, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    public Unit transferUnit(String unitId, String newOwnerActorId) throws Exception {
        String timestamp = Instant.now().toString();
        String resultJson = fabricClient.submitGenericTransaction("transferUnit", unitId, newOwnerActorId, timestamp);
        return fabricClient.getGson().fromJson(resultJson, Unit.class);
    }

    public Unit addTemperatureReading(String unitId, String temperature, String timestamp) throws Exception {
        logger.debug("Sende 'addTemperatureReading'-Transaktion für Einheit-ID: {}", unitId);
        String resultJson = fabricClient.submitGenericTransaction("addTemperatureReading", unitId, temperature, timestamp);
        logger.info("Temperaturmesswert erfolgreich für Einheit {} hinzugefügt.", unitId);
        return fabricClient.getGson().fromJson(resultJson, Unit.class);
    }

    public void deleteUnit(String unitId) throws Exception {
        logger.debug("Sende 'deleteUnits'-Transaktion für einzelne ID: {}", unitId);
        deleteUnits(Collections.singletonList(unitId));
    }



    public void deleteUnits(List<String> unitIds) throws Exception {
        logger.debug("Sende 'deleteUnits'-Transaktion für {} Einheiten.", unitIds.size());
        String unitIdsJson = fabricClient.getGson().toJson(unitIds);
        fabricClient.submitGenericTransaction("deleteUnits", unitIdsJson);
        logger.info("{} Einheiten erfolgreich zur Löschung eingereicht.", unitIds.size());
    }

    public String transferUnitRange(String medId, String chargeBezeichnung, int start, int end, String newOwnerId) throws Exception {
        logger.debug("Sende 'transferUnitRange'-Transaktion für Bereich {}-{}", start, end);
        String timestamp = Instant.now().toString();
        String result = fabricClient.submitGenericTransaction(
                "transferUnitRange", medId, chargeBezeichnung,
                String.valueOf(start), String.valueOf(end), newOwnerId, timestamp
        );
        logger.info("Chargenbereich erfolgreich zur Übertragung eingereicht.");
        return result;
    }

    public Map<String, Integer> getChargeCountsByMedId(String medId) {
        try {
            logger.debug("Rufe 'queryChargeCountsByMedId' für Medikamenten-ID '{}' auf.", medId);
            String resultJson = fabricClient.evaluateGenericTransaction("queryChargeCountsByMedId", medId);
            Type mapType = new TypeToken<Map<String, Integer>>() {}.getType();
            return fabricClient.getGson().fromJson(resultJson, mapType);
        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der Chargenanzahl für Medikamenten-ID '{}': {}", medId, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    private List<Unit> enrichUnitList(List<Unit> units) {
        if (units == null || units.isEmpty()) {
            return Collections.emptyList();
        }
        List<CompletableFuture<Unit>> futures = units.stream()
                .map(unit -> CompletableFuture.supplyAsync(() -> enrichSingleUnitWithIpfs(unit)))
                .collect(Collectors.toList());
        return futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Unit enrichSingleUnitWithIpfs(Unit unit) {
        if (unit == null || !StringUtils.hasText(unit.getIpfsLink())) {
            return unit;
        }
        final String cleanHash = unit.getIpfsLink().replace("ipfs://", "").trim();
        try {
            if (StringUtils.hasText(cleanHash)) {
                Type dataType = new TypeToken<Map<String, Object>>() {}.getType();
                Map<String, Object> ipfsData = ipfsClient.getObject(cleanHash, dataType);
                unit.setIpfsData(ipfsData);
            }
        } catch (IOException e) {
            logger.warn("Konnte IPFS-Daten für Einheit {} nicht abrufen oder verarbeiten: {}", unit.getUnitId(), e.getMessage());
        }
        return unit;
    }
}
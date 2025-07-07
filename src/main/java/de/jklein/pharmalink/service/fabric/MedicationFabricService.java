package de.jklein.pharmalink.service.fabric;

import com.google.gson.reflect.TypeToken;
import de.jklein.pharmalink.api.dto.CreateMedikamentRequestDto;
import de.jklein.pharmalink.api.mapper.MedikamentMapper;
import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.client.ipfs.IpfsClient;
import de.jklein.pharmalink.domain.Medikament;
import org.hyperledger.fabric.client.GatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class MedicationFabricService {

    private static final Logger logger = LoggerFactory.getLogger(MedicationFabricService.class);

    private final FabricClient fabricClient;
    private final IpfsClient ipfsClient;
    private final MedikamentMapper medikamentMapper;

    @Autowired
    public MedicationFabricService(FabricClient fabricClient, IpfsClient ipfsClient, MedikamentMapper medikamentMapper) {
        this.fabricClient = fabricClient;
        this.ipfsClient = ipfsClient;
        this.medikamentMapper = medikamentMapper;
    }

    public Medikament createMedikament(CreateMedikamentRequestDto requestDto) throws Exception {
        String finalInfoblattHash = requestDto.getInfoblattHash() != null ? requestDto.getInfoblattHash() : "";
        String finalIpfsLink = "";

        if (requestDto.getIpfsData() != null && !requestDto.getIpfsData().isEmpty()) {
            logger.info("Processing 'ipfsData' to generate a new IPFS link...");
            String ipfsJson = fabricClient.getGson().toJson(requestDto.getIpfsData());
            finalIpfsLink = ipfsClient.addObject(ipfsJson);
            logger.info("Successfully created new IPFS link: {}", finalIpfsLink);
        }

        String resultJson = fabricClient.submitGenericTransaction(
                "createMedikament",
                requestDto.getBezeichnung(),
                finalInfoblattHash,
                finalIpfsLink
        );

        return fabricClient.getGson().fromJson(resultJson, Medikament.class);
    }

    /**
     * Retrieves a single medication by its ID and enriches it with its IPFS data.
     * @param medId The ID of the medication.
     * @return An Optional containing the enriched medication, or empty if not found.
     */
    public Optional<Medikament> getEnrichedMedikamentById(String medId) {
        try {
            Medikament medikament = fabricClient.evaluateTransaction("queryMedikamentById", Medikament.class, medId);
            return Optional.ofNullable(enrichSingleMedikamentWithIpfs(medikament));
        } catch (Exception e) {
            logger.error("Error fetching medication with ID '{}': {}", medId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * **OPTIMIZED**: Retrieves all medications for a manufacturer and enriches them in parallel.
     * @param herstellerId The ID of the manufacturer.
     * @return A list of enriched medication objects.
     */
    public List<Medikament> getMedikamenteByHerstellerId(String herstellerId) throws GatewayException {
        String resultJson = fabricClient.evaluateGenericTransaction("queryMedikamenteByHerstellerId", herstellerId);
        Type listType = new TypeToken<List<Medikament>>() {}.getType();
        List<Medikament> medikamente = fabricClient.getGson().fromJson(resultJson, listType);
        return enrichMedikamentList(medikamente);
    }

    public Medikament approveMedication(String medId, String newStatus) throws Exception {
        String resultJson = fabricClient.submitGenericTransaction("approveMedikament", medId, newStatus);
        return fabricClient.getGson().fromJson(resultJson, Medikament.class);
    }

    /**
     * **DEPRECATED**: This method is functionally replaced by the more flexible search in `SearchService`.
     * It's kept here for backward compatibility if needed, but new development should use `SearchService`.
     */
    @Deprecated
    public List<Medikament> searchMedicationsByBezeichnung(String searchQuery) {
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryMedikamenteByBezeichnung", searchQuery);
            Type listType = new TypeToken<List<Medikament>>() {}.getType();
            List<Medikament> medikamente = fabricClient.getGson().fromJson(resultJson, listType);
            return enrichMedikamentList(medikamente);
        } catch (Exception e) {
            logger.error("Error searching medications with query '{}': {}", searchQuery, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * **OPTIMIZED**: Retrieves all medications from the blockchain and enriches them in parallel.
     * @return A list of all enriched medication domain objects.
     */
    public List<Medikament> getAllMedikamente() {
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryAllMedikamente");
            Type listType = new TypeToken<List<Medikament>>() {}.getType();
            List<Medikament> medikamente = fabricClient.getGson().fromJson(resultJson, listType);
            return enrichMedikamentList(medikamente);
        } catch (Exception e) {
            logger.error("Error fetching all medications from chaincode: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Private helper to enrich a list of medications with IPFS data in parallel.
     */
    private List<Medikament> enrichMedikamentList(List<Medikament> medikamente) {
        if (medikamente == null || medikamente.isEmpty()) {
            return Collections.emptyList();
        }
        List<CompletableFuture<Medikament>> futures = medikamente.stream()
                .map(med -> CompletableFuture.supplyAsync(() -> enrichSingleMedikamentWithIpfs(med)))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Private helper that enriches a single medication object with IPFS data.
     */
    private Medikament enrichSingleMedikamentWithIpfs(Medikament medikament) {
        if (medikament == null) {
            return null;
        }

        if (StringUtils.hasText(medikament.getIpfsLink())) {
            final String cleanHash = medikament.getIpfsLink().replace("ipfs://", "").trim();
            try {
                if (!cleanHash.isEmpty()) {
                    Type dataType = new TypeToken<Map<String, Object>>() {}.getType();
                    Map<String, Object> ipfsData = ipfsClient.getObject(cleanHash, dataType);
                    medikament.setIpfsData(ipfsData);
                    logger.debug("Successfully enriched Medikament {} with IPFS data.", medikament.getMedId());
                }
            } catch (IOException e) {
                logger.warn("Could not fetch or parse IPFS data for Medikament {}: {}", medikament.getMedId(), e.getMessage());
            }
        }
        return medikament;
    }
}
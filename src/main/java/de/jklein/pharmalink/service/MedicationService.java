package de.jklein.pharmalink.service;

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

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MedicationService {

    private static final Logger logger = LoggerFactory.getLogger(MedicationService.class);

    private final FabricClient fabricClient;
    private final IpfsClient ipfsClient;
    private final MedikamentMapper medikamentMapper;

    @Autowired
    public MedicationService(FabricClient fabricClient, IpfsClient ipfsClient, MedikamentMapper medikamentMapper) {
        this.fabricClient = fabricClient;
        this.ipfsClient = ipfsClient;
        this.medikamentMapper = medikamentMapper;
    }

    public Medikament createMedikament(CreateMedikamentRequestDto requestDto) throws Exception {
        String finalInfoblattHash = "";
        if (requestDto.getInfoblattHash() != null && !requestDto.getInfoblattHash().isEmpty()) {
            finalInfoblattHash = requestDto.getInfoblattHash();
        }

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

        Medikament createdMedikament = fabricClient.getGson().fromJson(resultJson, Medikament.class);
        return createdMedikament;
    }

    public List<Medikament> getMedikamenteByHerstellerId(String herstellerId) throws GatewayException {
        String resultJson = fabricClient.evaluateGenericTransaction(
                "queryMedikamenteByHerstellerId",
                herstellerId
        );

        Type listType = new TypeToken<List<Medikament>>() {}.getType();
        List<Medikament> medikamente = fabricClient.getGson().fromJson(resultJson, listType);

        return medikamente.stream()
                .flatMap(medikament -> {
                    try {
                        return getEnrichedMedikamentById(medikament.getMedId()).stream();
                    } catch (Exception e) {
                        logger.warn("Fehler beim Anreichern von Medikament '{}' für Hersteller '{}'. Fehler: {}. Dieser Eintrag wird übersprungen.",
                                medikament.getMedId(), herstellerId, e.getMessage());
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());
    }

    public Optional<Medikament> getEnrichedMedikamentById(String medId) {
        try {
            Medikament medikament = fabricClient.evaluateTransaction("queryMedikamentById", Medikament.class, medId);
            if (medikament == null) {
                return Optional.empty();
            }

            final String originalIpfsLink = medikament.getIpfsLink();

            if (originalIpfsLink != null && !originalIpfsLink.isBlank()) {
                final String cleanHash = originalIpfsLink.replace("ipfs://", "").trim();

                logger.info("Resolving IPFS link '{}' (cleaned to '{}') for medication '{}'",
                        originalIpfsLink, cleanHash, medId);

                try {
                    Map<String, Object> ipfsData = ipfsClient.getObject(cleanHash, new TypeToken<Map<String, Object>>() {}.getType());

                    if (ipfsData != null) {
                        medikament.setIpfsData(ipfsData);
                        logger.info("Successfully attached IPFS data for CID '{}'", cleanHash);
                    } else {
                        logger.warn("IPFS content for CID '{}' was null after fetching for medication '{}'.", cleanHash, medId);
                    }
                } catch (IOException e) {
                    logger.warn("Fehler beim Abrufen oder Deserialisieren von IPFS-Inhalt für CID '{}': {}. Überspringe Anreicherung.", cleanHash, e.getMessage());
                }
            }

            return Optional.of(medikament);

        } catch (Exception e) {
            logger.error("Fehler beim Abrufen des Medikaments mit ID '{}': {}", medId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Medikament approveMedication(String medId, String newStatus) throws Exception {
        String resultJson = fabricClient.submitGenericTransaction(
                "approveMedikament",
                medId,
                newStatus
        );

        Medikament updatedMedikament = fabricClient.getGson().fromJson(resultJson, Medikament.class);
        return updatedMedikament;
    }

    public List<Medikament> searchMedicationsByBezeichnung(String searchQuery) {
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryMedikamenteByBezeichnung", searchQuery);
            Type listType = new TypeToken<List<Medikament>>() {}.getType();
            List<Medikament> medikamente = fabricClient.getGson().fromJson(resultJson, listType);

            return medikamente.stream()
                    .flatMap(medikament -> this.getEnrichedMedikamentById(medikament.getMedId()).stream())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Fehler bei der Suche nach Medikamenten mit Query '{}': {}", searchQuery, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Ruft alle Medikamente von der Blockchain ab und reichert sie mit ihren IPFS-Daten an.
     *
     * @return Eine Liste von angereicherten Medikament-Domain-Objekten.
     */
    public List<Medikament> getAllMedikamente() {
        try {
            // Annahme: "queryAllMedikamente" existiert im Chaincode und gibt alle Medikamente zurück.
            String resultJson = fabricClient.evaluateGenericTransaction("queryAllMedikamente");
            Type listType = new TypeToken<List<Medikament>>() {}.getType();
            List<Medikament> medikamente = fabricClient.getGson().fromJson(resultJson, listType);

            // Anreicherung der Medikamente mit IPFS-Daten
            return medikamente.stream()
                    .flatMap(medikament -> this.getEnrichedMedikamentById(medikament.getMedId()).stream())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Fehler beim Abrufen aller Medikamente aus dem Chaincode: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
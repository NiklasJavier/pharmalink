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
            logger.info("Verarbeite 'ipfsData', um einen neuen IPFS-Link zu erstellen...");
            String ipfsJson = fabricClient.getGson().toJson(requestDto.getIpfsData());
            finalIpfsLink = ipfsClient.addObject(ipfsJson);
            logger.info("Neuer IPFS-Link erfolgreich erstellt: {}", finalIpfsLink);
        }

        String resultJson = fabricClient.submitGenericTransaction(
                "createMedikament",
                requestDto.getBezeichnung(),
                finalInfoblattHash,
                finalIpfsLink
        );

        return fabricClient.getGson().fromJson(resultJson, Medikament.class);
    }

    public Optional<Medikament> getEnrichedMedikamentById(String medId) {
        try {
            Medikament medikament = fabricClient.evaluateTransaction("queryMedikamentById", Medikament.class, medId);
            return Optional.ofNullable(enrichSingleMedikamentWithIpfs(medikament));
        } catch (Exception e) {
            logger.error("Fehler beim Abrufen des Medikaments mit ID '{}': {}", medId, e.getMessage(), e);
            return Optional.empty();
        }
    }

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

    @Deprecated
    public List<Medikament> searchMedicationsByBezeichnung(String searchQuery) {
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryMedikamenteByBezeichnung", searchQuery);
            Type listType = new TypeToken<List<Medikament>>() {}.getType();
            List<Medikament> medikamente = fabricClient.getGson().fromJson(resultJson, listType);
            return enrichMedikamentList(medikamente);
        } catch (Exception e) {
            logger.error("Fehler bei der Suche nach Medikamenten mit der Anfrage '{}': {}", searchQuery, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<Medikament> getAllMedikamente() {
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryAllMedikamente");
            Type listType = new TypeToken<List<Medikament>>() {}.getType();
            List<Medikament> medikamente = fabricClient.getGson().fromJson(resultJson, listType);
            return enrichMedikamentList(medikamente);
        } catch (Exception e) {
            logger.error("Fehler beim Abrufen aller Medikamente vom Chaincode: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

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

    private Medikament enrichSingleMedikamentWithIpfs(Medikament medikament) {
        if (medikament == null) {
            return null;
        }

        if (StringUtils.hasText(medikament.getIpfsLink())) {
            final String cleanHash = medikament.getIpfsLink().replace("ipfs://", "").trim();
            try {
                if (StringUtils.hasText(cleanHash)) {
                    Type dataType = new TypeToken<Map<String, Object>>() {}.getType();
                    Map<String, Object> ipfsData = ipfsClient.getObject(cleanHash, dataType);
                    medikament.setIpfsData(ipfsData);
                    logger.debug("Medikament {} erfolgreich mit IPFS-Daten angereichert.", medikament.getMedId());
                }
            } catch (IOException e) {
                logger.warn("Konnte IPFS-Daten für Medikament {} nicht abrufen oder verarbeiten: {}", medikament.getMedId(), e.getMessage());
            }
        }
        return medikament;
    }

    public Medikament updateMedikament(String medId, String bezeichnung, String infoblattHash, Map<String, Object> ipfsData) throws Exception {
        String finalIpfsLink = "";

        if (ipfsData != null && !ipfsData.isEmpty()) {
            logger.info("Verarbeite neue 'ipfsData' für Medikament-ID: {}", medId);
            String ipfsJson = fabricClient.getGson().toJson(ipfsData);
            finalIpfsLink = ipfsClient.addObject(ipfsJson);
            logger.info("Neuer IPFS-Link für Update erstellt: {}", finalIpfsLink);
        }

        logger.debug("Sende 'updateMedikament'-Transaktion für ID: {}", medId);
        String resultJson = fabricClient.submitGenericTransaction(
                "updateMedikament",
                medId,
                bezeichnung,
                infoblattHash,
                finalIpfsLink
        );
        return fabricClient.getGson().fromJson(resultJson, Medikament.class);
    }

    public void deleteMedikamentIfNoUnits(String medId) throws Exception {
        logger.debug("Sende 'deleteMedikamentIfNoUnits'-Transaktion für ID: {}", medId);
        fabricClient.submitGenericTransaction("deleteMedikamentIfNoUnits", medId);
        logger.info("Medikament {} erfolgreich zur bedingten Löschung eingereicht.", medId);
    }
}
package de.jklein.pharmalink.service;

import com.google.gson.reflect.TypeToken;
import de.jklein.pharmalink.api.dto.CreateMedikamentRequestDto;
import de.jklein.pharmalink.api.dto.MedikamentResponseDto;
import de.jklein.pharmalink.api.dto.UnitResponseDto;
import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.client.ipfs.IpfsClient;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import de.jklein.pharmalink.api.mapper.MedikamentMapper;
import de.jklein.pharmalink.api.mapper.UnitMapper;
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

@Service
public class MedicationService {

    private static final Logger logger = LoggerFactory.getLogger(MedicationService.class);

    private final FabricClient fabricClient;
    private final IpfsClient ipfsClient;
    private final MedikamentMapper medikamentMapper;
    private final UnitMapper unitMapper;

    @Autowired
    public MedicationService(FabricClient fabricClient, IpfsClient ipfsClient, MedikamentMapper medikamentMapper, UnitMapper unitMapper) {
        this.fabricClient = fabricClient;
        this.ipfsClient = ipfsClient;
        this.medikamentMapper = medikamentMapper;
        this.unitMapper = unitMapper;
    }

    /**
     * Legt ein neues Medikament im Ledger an.
     * Verarbeitet die Eingaben aus dem DTO und ruft die Chaincode-Funktion mit der
     * korrekten Argumentenreihenfolge (bezeichnung, infoblattHash, ipfsLink) auf.
     *
     * @param requestDto Das DTO mit den Daten des neuen Medikaments.
     * @return Ein DTO des neu erstellten Medikaments.
     * @throws Exception bei Fehlern während der Transaktion.
     */
    public MedikamentResponseDto createMedikament(CreateMedikamentRequestDto requestDto) throws Exception {
        // Variable für den Hash des Infoblatts initialisieren.
        String finalInfoblattHash = "";
        if (requestDto.getInfoblattHash() != null && !requestDto.getInfoblattHash().isEmpty()) {
            finalInfoblattHash = requestDto.getInfoblattHash();
        }

        // Variable für den Hash der dynamischen JSON-Daten (ipfsData) initialisieren.
        String finalIpfsLink = "";
        if (requestDto.getIpfsData() != null && !requestDto.getIpfsData().isEmpty()) {
            logger.info("Processing 'ipfsData' to generate a new IPFS link...");
            String ipfsJson = fabricClient.getGson().toJson(requestDto.getIpfsData());
            byte[] ipfsBytes = ipfsJson.getBytes(StandardCharsets.UTF_8);
            finalIpfsLink = ipfsClient.add(ipfsBytes);
            logger.info("Successfully created new IPFS link: {}", finalIpfsLink);
        }

        // Die Fabric-Transaktion mit den drei Argumenten in der korrekten Reihenfolge aufrufen.
        // 1. bezeichnung
        // 2. infoblattHash
        // 3. ipfsLink
        String resultJson = fabricClient.submitGenericTransaction(
                "createMedikament",
                requestDto.getBezeichnung(),
                finalInfoblattHash,
                finalIpfsLink
        );

        // Die Antwort von Fabric verarbeiten und als DTO zurückgeben.
        Medikament createdMedikament = fabricClient.getGson().fromJson(resultJson, Medikament.class);
        return medikamentMapper.toDto(createdMedikament);
    }

    /**
     * Ruft alle Medikamente für eine gegebene Hersteller-ID ab und reichert jedes
     * einzelne mit den zugehörigen Daten aus IPFS an.
     * Die Logik ist nun robuster, um ungültige Hashes zu ignorieren.
     *
     * @param herstellerId Die ID des Herstellers.
     * @return Eine Liste von angereicherten Medikament-DTOs.
     * @throws GatewayException bei Fehlern in der Fabric-Kommunikation.
     */
    public List<MedikamentResponseDto> getMedikamenteByHerstellerId(String herstellerId) throws GatewayException {
        String resultJson = fabricClient.evaluateGenericTransaction(
                "queryMedikamenteByHerstellerId",
                herstellerId
        );

        Type listType = new TypeToken<List<Medikament>>() {}.getType();
        List<Medikament> medikamente = fabricClient.getGson().fromJson(resultJson, listType);

        return medikamente.stream()
                .map(medikament -> {
                    MedikamentResponseDto dto = medikamentMapper.toDto(medikament);
                    final String originalIpfsLink = medikament.getIpfsLink();

                    if (originalIpfsLink != null && !originalIpfsLink.isBlank()) {
                        // Kapsle den gesamten Auflösungsversuch in einen try-catch-Block.
                        try {
                            final String cleanHash = originalIpfsLink.replace("ipfs://", "").trim();

                            // Wenn der Hash nach dem Säubern leer ist (z.B. nur "ipfs://"), überspringe ihn.
                            if (cleanHash.isEmpty()) {
                                return dto;
                            }

                            ipfsClient.get(cleanHash).ifPresent(ipfsBytes -> {
                                try {
                                    String jsonContent = new String(ipfsBytes, StandardCharsets.UTF_8);
                                    Type dataType = new TypeToken<Map<String, Object>>() {}.getType();
                                    Map<String, Object> ipfsData = fabricClient.getGson().fromJson(jsonContent, dataType);
                                    dto.setIpfsData(ipfsData);
                                } catch (Exception parseException) {
                                    logger.error("Konnte IPFS JSON-Inhalt für CID '{}' nicht parsen.", cleanHash, parseException);
                                }
                            });
                            // GEÄNDERT: Fange JEDE Exception ab (IOException, ungültige Hashes etc.)
                        } catch (Exception e) {
                            logger.warn("Fehler beim Abrufen oder Verarbeiten von IPFS-Daten für den Link '{}'. Fehler: {}. Dieser Eintrag wird übersprungen.",
                                    originalIpfsLink, e.getMessage());
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }



    /**
     * Ruft ein Medikament ab und reichert es mit Daten aus IPFS an.
     * Die Logik wurde erweitert, um IPFS-Präfixe zu entfernen und die Abfrage robuster zu machen.
     *
     * @param medId Die ID des Medikaments.
     * @return Ein Optional, das das angereicherte DTO enthält.
     */
    public Optional<MedikamentResponseDto> getEnrichedMedikamentById(String medId) {
        try {
            Medikament medikament = fabricClient.evaluateTransaction("queryMedikamentById", medId, Medikament.class);
            if (medikament == null) {
                return Optional.empty();
            }

            MedikamentResponseDto dto = medikamentMapper.toDto(medikament);

            final String originalIpfsLink = medikament.getIpfsLink();

            if (originalIpfsLink != null && !originalIpfsLink.isBlank()) {
                // NEU: Säubern des Hashes von gängigen Präfixen.
                final String cleanHash = originalIpfsLink.replace("ipfs://", "").trim();

                logger.info("Resolving IPFS link '{}' (cleaned to '{}') for medication '{}'",
                        originalIpfsLink, cleanHash, medId);

                ipfsClient.get(cleanHash).ifPresent(ipfsBytes -> {
                    try {
                        String jsonContent = new String(ipfsBytes, StandardCharsets.UTF_8);
                        Type type = new TypeToken<Map<String, Object>>() {}.getType();
                        Map<String, Object> ipfsData = fabricClient.getGson().fromJson(jsonContent, type);

                        dto.setIpfsData(ipfsData);
                        logger.info("Successfully attached IPFS data for CID '{}'", cleanHash);
                    } catch (Exception e) {
                        logger.error("Failed to parse IPFS JSON content for CID '{}'", cleanHash, e);
                    }
                });
            }

            return Optional.of(dto);

        } catch (Exception e) {
            logger.error("Failed to retrieve medication with ID '{}'", medId, e);
            return Optional.empty();
        }
    }

    /**
     * NEUE METHODE: Erstellt Units und gibt eine Liste der entsprechenden DTOs zurück.
     *
     * @param medId             Die ID des Medikaments.
     * @param chargeBezeichnung Die Bezeichnung der Charge.
     * @param anzahl            Die Anzahl der zu erstellenden Units.
     * @param ipfsLink          Ein optionaler Link zu IPFS.
     * @return Eine Liste der erstellten Units als DTOs.
     * @throws Exception Wirft eine Exception bei Fehlern während der Transaktion.
     */
    public List<UnitResponseDto> createUnitsForMedication(String medId, String chargeBezeichnung, int anzahl, String ipfsLink) throws Exception {
        // 1. Rufe die generische Submit-Methode deines FabricClient auf.
        // Die Argumente müssen exakt der Signatur im Chaincode entsprechen.
        String resultJson = fabricClient.submitGenericTransaction(
                "createUnits",
                medId,
                chargeBezeichnung,
                String.valueOf(anzahl), // Chaincode-Argumente sind typischerweise Strings
                ipfsLink != null ? ipfsLink : "" // Sicherstellen, dass kein null übergeben wird
        );

        // 2. Prüfen, ob eine sinnvolle Antwort vom Chaincode kam.
        if (resultJson == null || resultJson.isEmpty() || resultJson.equals("[]")) {
            return Collections.emptyList();
        }

        // 3. Die JSON-Antwort (ein Array von Units) in Domain-Objekte umwandeln.
        Type listType = new TypeToken<List<Unit>>() {}.getType();
        List<Unit> createdUnits = fabricClient.getGson().fromJson(resultJson, listType);

        // 4. Die Liste der Domain-Objekte in eine Liste von DTOs für die API-Antwort mappen.
        return unitMapper.toDtoList(createdUnits);
    }

    /**
     * Ändert den Status eines Medikaments (z.B. Freigabe durch eine Behörde).
     *
     * @param medId Die ID des zu ändernden Medikaments.
     * @param newStatus Der neue Status ("freigegeben" oder "abgelehnt").
     * @return Ein DTO des aktualisierten Medikaments.
     * @throws Exception bei Fehlern, z.B. wenn der Aufrufer nicht berechtigt ist.
     */
    public MedikamentResponseDto approveMedication(String medId, String newStatus) throws Exception {
        // 1. Rufe die Chaincode-Funktion 'approveMedikament' auf.
        // Die Autorisierungsprüfung (Rolle "behoerde") findet im Chaincode statt.
        String resultJson = fabricClient.submitGenericTransaction(
                "approveMedikament",
                medId,
                newStatus
        );

        // 2. Wandle das aktualisierte Medikament in ein Domain-Objekt um.
        Medikament updatedMedikament = fabricClient.getGson().fromJson(resultJson, Medikament.class);

        // 3. Mappe das Domain-Objekt zu einem DTO für die Antwort.
        return medikamentMapper.toDto(updatedMedikament);
    }

    /**
     * Sucht nach Medikamenten anhand eines Teils ihrer Bezeichnung.
     * Jedes gefundene Medikament wird mit IPFS-Daten angereichert.
     *
     * @param searchQuery Der Suchtext.
     * @return Eine Liste von passenden, angereicherten Medikament-DTOs.
     */
    public List<MedikamentResponseDto> searchMedicationsByBezeichnung(String searchQuery) {
        try {
            // Ruft die Chaincode-Funktion über den FabricClient auf.
            String resultJson = fabricClient.evaluateGenericTransaction("queryMedikamenteByBezeichnung", searchQuery);
            Type listType = new TypeToken<List<Medikament>>() {}.getType();
            List<Medikament> medikamente = fabricClient.getGson().fromJson(resultJson, listType);

            // Reichert jedes gefundene Medikament an.
            return medikamente.stream()
                    .map(medikament -> this.getEnrichedMedikamentById(medikament.getMedId()).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Fehler bei der Suche nach Medikamenten mit Query '{}'", searchQuery, e);
            return Collections.emptyList();
        }
    }
}
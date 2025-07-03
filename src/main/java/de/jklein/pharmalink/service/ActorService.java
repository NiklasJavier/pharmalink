package de.jklein.pharmalink.service;

import com.google.gson.reflect.TypeToken;
import de.jklein.pharmalink.api.dto.ActorResponseDto;
import de.jklein.pharmalink.api.mapper.ActorMapper;
import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.client.ipfs.IpfsClient; // NEU: Import für IpfsClient
import de.jklein.pharmalink.domain.Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActorService {

    private static final Logger logger = LoggerFactory.getLogger(ActorService.class);

    private final FabricClient fabricClient;
    private final IpfsClient ipfsClient;     // NEU: Feld für den IpfsClient
    private final ActorMapper actorMapper;   // NEU: Feld für den ActorMapper

    @Autowired
    public ActorService(FabricClient fabricClient, IpfsClient ipfsClient, ActorMapper actorMapper) { // NEU: Im Konstruktor hinzugefügt
        this.fabricClient = fabricClient;
        this.ipfsClient = ipfsClient;
        this.actorMapper = actorMapper;
    }

    /**
     * Ruft einen Akteur anhand seiner ID ab und reichert ihn mit Daten aus IPFS an.
     * @param actorId Die ID des abzurufenden Akteurs.
     * @return Ein Optional, das das angereicherte DTO enthält.
     */
    public Optional<ActorResponseDto> getEnrichedActorById(String actorId) {
        try {
            Actor actor = fabricClient.evaluateTransaction("queryActorById", actorId, Actor.class);
            if (actor == null) {
                return Optional.empty();
            }

            ActorResponseDto dto = actorMapper.toDto(actor);

            final String originalIpfsLink = actor.getIpfsLink();
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
                                logger.error("Konnte IPFS JSON-Inhalt für CID '{}' nicht parsen.", cleanHash, parseEx);
                            }
                        });
                    }
                } catch (Exception ipfsEx) {
                    logger.warn("Fehler beim Abrufen von IPFS-Daten für den Link '{}'. Fehler: {}. Dieser Eintrag wird übersprungen.",
                            originalIpfsLink, ipfsEx.getMessage());
                }
            }

            return Optional.of(dto);

        } catch (Exception e) {
            logger.error("Fehler beim Abrufen des Akteurs mit ID '{}'", actorId, e);
            return Optional.empty();
        }
    }

    /**
     * Ruft alle Akteure mit einer bestimmten Rolle von der Blockchain ab.
     * @param role Die Rolle, nach der gefiltert werden soll (z.B. "hersteller").
     * @return Eine Liste von Actor-DTOs, die der Rolle entsprechen.
     */
    public List<ActorResponseDto> getActorsByRole(String role) {
        try {
            // 1. Rufe die Chaincode-Funktion 'queryActorsByRole' über den FabricClient auf.
            String resultJson = fabricClient.evaluateGenericTransaction("queryActorsByRole", role);

            // 2. Wandle das Ergebnis (JSON-Array) in eine Liste von Domain-Objekten um.
            Type listType = new TypeToken<List<Actor>>() {}.getType();
            List<Actor> actors = fabricClient.getGson().fromJson(resultJson, listType);

            // 3. Mappe die Domain-Objekte zu DTOs für die API-Antwort.
            return actors.stream()
                    .map(actorMapper::toDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der Akteure für die Rolle '{}'", role, e);
            // Gib bei einem Fehler eine leere Liste zurück, um die API stabil zu halten.
            return Collections.emptyList();
        }
    }
}
package de.jklein.pharmalink.service;

import com.google.gson.reflect.TypeToken;
import de.jklein.pharmalink.api.dto.ActorResponseDto;
import de.jklein.pharmalink.api.mapper.ActorMapper;
import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.client.ipfs.IpfsClient;
import de.jklein.pharmalink.domain.Actor; // Import des Domain-Objekts
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream; // Import für Stream

@Service
public class ActorService {

    private static final Logger logger = LoggerFactory.getLogger(ActorService.class);

    private final FabricClient fabricClient;
    private final IpfsClient ipfsClient;
    private final ActorMapper actorMapper; // Mapper bleibt für Konvertierung zu/von DTOs

    @Autowired
    public ActorService(FabricClient fabricClient, IpfsClient ipfsClient, ActorMapper actorMapper) {
        this.fabricClient = fabricClient;
        this.ipfsClient = ipfsClient;
        this.actorMapper = actorMapper;
    }

    /**
     * Ruft einen Akteur anhand seiner ID ab und reichert ihn mit Daten aus IPFS an.
     * Gibt ein Optional des angereicherten Domain-Objekts zurück.
     * @param actorId Die ID des abzurufenden Akteurs.
     * @return Ein Optional, das das angereicherte Actor-Domain-Objekt enthält.
     */
    public Optional<Actor> getEnrichedActorById(String actorId) { // Rückgabetyp geändert
        try {
            Actor actor = fabricClient.evaluateTransaction("queryActorById", actorId, Actor.class);
            if (actor == null) {
                return Optional.empty();
            }

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
                                actor.setIpfsData(ipfsData); // IPFS-Daten direkt im Domain-Objekt speichern
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

            return Optional.of(actor); // Angereichertes Domain-Objekt zurückgeben

        } catch (Exception e) {
            logger.error("Fehler beim Abrufen des Akteurs mit ID '{}'", actorId, e);
            return Optional.empty();
        }
    }

    /**
     * Ruft alle Akteure mit einer bestimmten Rolle von der Blockchain ab und reichert sie an.
     * Gibt eine Liste von angereicherten Actor-Domain-Objekten zurück.
     * @param role Die Rolle, nach der gefiltert werden soll (z.B. "hersteller").
     * @return Eine Liste von Actor-Domain-Objekten, die der Rolle entsprechen.
     */
    public List<Actor> getActorsByRole(String role) { // Rückgabetyp geändert
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryActorsByRole", role);

            Type listType = new TypeToken<List<Actor>>() {}.getType();
            List<Actor> actors = fabricClient.getGson().fromJson(resultJson, listType);

            // Anreicherung direkt im Domain-Objekt
            return actors.stream()
                    .flatMap(actor -> this.getEnrichedActorById(actor.getActorId()).stream())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der Akteure für die Rolle '{}'", role, e);
            return Collections.emptyList();
        }
    }

    /**
     * Sucht nach Herstellern, deren Bezeichnung einen bestimmten Text enthält.
     * Gibt eine Liste von passenden, angereicherten Domain-Objekten zurück.
     * @param searchQuery Der Suchtext.
     * @return Eine Liste von passenden Hersteller-Domain-Objekten.
     */
    public List<Actor> searchHerstellerByBezeichnung(String searchQuery) { // Rückgabetyp geändert
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryActorsByBezeichnung", searchQuery);
            Type listType = new TypeToken<List<Actor>>() {}.getType();
            List<Actor> actors = fabricClient.getGson().fromJson(resultJson, listType);

            // Filtere die Liste im Java-Code, um nur Hersteller zu behalten und reicher sie an.
            return actors.stream()
                    .filter(actor -> "hersteller".equalsIgnoreCase(actor.getRole()))
                    .flatMap(actor -> this.getEnrichedActorById(actor.getActorId()).stream())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Fehler bei der Suche nach Herstellern mit Query '{}'", searchQuery, e);
            return Collections.emptyList();
        }
    }

    // Zusätzliche Getter für den Mapper, falls dieser nicht direkt injiziert werden kann (z.B. im AppDataInitializer)
    public ActorMapper getActorMapper() {
        return actorMapper;
    }
}
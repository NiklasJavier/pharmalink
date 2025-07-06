package de.jklein.pharmalink.service;

import com.google.gson.reflect.TypeToken;
import de.jklein.pharmalink.api.dto.ActorResponseDto; // Behalten, falls für andere Controller oder spezifische DTO-Nutzung benötigt
import de.jklein.pharmalink.api.mapper.ActorMapper;
import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.client.ipfs.IpfsClient;
import de.jklein.pharmalink.domain.Actor; // Import Domain-Objekt
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException; // NEU: Import für IOException
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ActorService {

    private static final Logger logger = LoggerFactory.getLogger(ActorService.class);

    private final FabricClient fabricClient;
    private final IpfsClient ipfsClient;
    private final ActorMapper actorMapper;

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
    public Optional<Actor> getEnrichedActorById(String actorId) {
        try {
            Actor actor = fabricClient.evaluateTransaction("queryActorById", actorId, Actor.class);
            if (actor == null) {
                return Optional.empty();
            }

            final String originalIpfsLink = actor.getIpfsLink();
            if (originalIpfsLink != null && !originalIpfsLink.isBlank()) {
                final String cleanHash = originalIpfsLink.replace("ipfs://", "").trim();

                logger.info("Resolving IPFS link '{}' (cleaned to '{}') for actor '{}'",
                        originalIpfsLink, cleanHash, actorId);

                try {
                    // NEU: Direkte Verwendung von ipfsClient.getObject mit Type-Parameter
                    // Dies nutzt intern den Datenbank-Cache und die Deserialisierungslogik
                    Type dataType = new TypeToken<Map<String, Object>>() {}.getType(); // TypeToken außerhalb der Lambda
                    Map<String, Object> ipfsData = ipfsClient.getObject(cleanHash, dataType);

                    if (ipfsData != null) {
                        actor.setIpfsData(ipfsData);
                        logger.info("Successfully attached IPFS data for CID '{}'", cleanHash);
                    } else {
                        logger.warn("IPFS content for CID '{}' was null after fetching for actor '{}'.", cleanHash, actorId);
                    }
                } catch (IOException e) { // IOException fangen, da getObject sie werfen kann
                    logger.error("Fehler beim Abrufen oder Deserialisieren von IPFS-Inhalt für CID '{}': {}", cleanHash, e.getMessage(), e);
                    // Den Fehler loggen, aber die Akteur-Anreicherung fortsetzen
                }
            }

            return Optional.of(actor);

        } catch (Exception e) { // Hier bleibt Exception, um Fabric-Fehler und andere abzufangen
            logger.error("Fehler beim Abrufen des Akteurs mit ID '{}': {}", actorId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Ruft alle Akteure mit einer bestimmten Rolle von der Blockchain ab und reichert sie an.
     * Gibt eine Liste von angereicherten Actor-Domain-Objekten zurück.
     * @param role Die Rolle, nach der gefiltert werden soll (z.B. "hersteller").
     * @return Eine Liste von Actor-Domain-Objekten, die der Rolle entsprechen.
     */
    public List<Actor> getActorsByRole(String role) {
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryActorsByRole", role);

            Type listType = new TypeToken<List<Actor>>() {}.getType();
            List<Actor> actors = fabricClient.getGson().fromJson(resultJson, listType);

            return actors.stream()
                    .flatMap(actor -> {
                        try {
                            return this.getEnrichedActorById(actor.getActorId()).stream();
                        } catch (Exception e) { // Exception muss hier gefangen werden, da flatMap keine Checked Exception zulässt
                            logger.warn("Fehler beim Anreichern von Akteur '{}' für Rolle '{}'. Fehler: {}. Dieser Eintrag wird übersprungen.",
                                    actor.getActorId(), role, e.getMessage());
                            return Stream.empty();
                        }
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der Akteure für die Rolle '{}': {}", role, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Sucht nach Herstellern, deren Bezeichnung einen bestimmten Text enthält.
     * Gibt eine Liste von passenden, angereicherten Domain-Objekten zurück.
     * @param searchQuery Der Suchtext.
     * @return Eine Liste von passenden Hersteller-Domain-Objekten.
     */
    public List<Actor> searchHerstellerByBezeichnung(String searchQuery) {
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryActorsByBezeichnung", searchQuery);
            Type listType = new TypeToken<List<Actor>>() {}.getType();
            List<Actor> actors = fabricClient.getGson().fromJson(resultJson, listType);

            return actors.stream()
                    .filter(actor -> "hersteller".equalsIgnoreCase(actor.getRole()))
                    .flatMap(actor -> {
                        try {
                            return this.getEnrichedActorById(actor.getActorId()).stream();
                        } catch (Exception e) { // Exception muss hier gefangen werden
                            logger.warn("Fehler beim Anreichern von Hersteller '{}' für Suche '{}'. Fehler: {}. Dieser Eintrag wird übersprungen.",
                                    actor.getActorId(), searchQuery, e.getMessage());
                            return Stream.empty();
                        }
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Fehler bei der Suche nach Herstellern mit Query '{}': {}", searchQuery, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public ActorMapper getActorMapper() {
        return actorMapper;
    }
}
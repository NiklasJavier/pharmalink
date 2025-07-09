package de.jklein.pharmalink.service.fabric;

import com.google.gson.reflect.TypeToken;
import de.jklein.pharmalink.api.mapper.ActorMapper;
import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.client.ipfs.IpfsClient;
import de.jklein.pharmalink.domain.Actor;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ActorFabricService {

    private static final Logger logger = LoggerFactory.getLogger(ActorFabricService.class);

    private final FabricClient fabricClient;
    private final IpfsClient ipfsClient;
    private final ActorMapper actorMapper;

    @Autowired
    public ActorFabricService(FabricClient fabricClient, IpfsClient ipfsClient, ActorMapper actorMapper) {
        this.fabricClient = fabricClient;
        this.ipfsClient = ipfsClient;
        this.actorMapper = actorMapper;
    }

    /**
     * Ruft einen Akteur anhand seiner ID ab und reichert ihn mit Daten aus IPFS an.
     * @param actorId Die ID des abzurufenden Akteurs.
     * @return Ein Optional, das den angereicherten Actor enthält.
     */
    public Optional<Actor> getEnrichedActorById(String actorId) {
        try {
            Actor actor = fabricClient.evaluateTransaction("queryActorById", Actor.class, actorId);
            return Optional.ofNullable(enrichSingleActorWithIpfs(actor));
        } catch (Exception e) {
            logger.error("Fehler beim Abrufen des Akteurs mit ID '{}': {}", actorId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * **OPTIMIERT**: Ruft alle Akteure mit einer bestimmten Rolle ab und reichert sie parallel mit IPFS-Daten an.
     * @param role Die Rolle, nach der gefiltert werden soll.
     * @return Eine Liste von angereicherten Actor-Objekten.
     */
    public List<Actor> getActorsByRole(String role) {
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryActorsByRole", role);
            Type listType = new TypeToken<List<Actor>>() {}.getType();
            List<Actor> actors = fabricClient.getGson().fromJson(resultJson, listType);

            // Führe die IPFS-Anreicherung für alle Akteure parallel aus
            List<CompletableFuture<Actor>> futures = actors.stream()
                    .map(actor -> CompletableFuture.supplyAsync(() -> enrichSingleActorWithIpfs(actor)))
                    .collect(Collectors.toList());

            // Warte auf den Abschluss aller Futures und sammle die Ergebnisse
            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der Akteure für die Rolle '{}': {}", role, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Ruft alle im System vorhandenen Akteure ab.
     * Diese Methode ist nützlich für den AppDataInitializer.
     * @return Eine Liste aller angereicherten Akteure.
     */
    public List<Actor> getAllActors() {
        // Annahme: Es gibt eine Chaincode-Funktion, die alle Akteure zurückgibt.
        // Andernfalls könnte man `queryActorsByRole` für jede bekannte Rolle aufrufen.
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryAllActors");

            Type listType = new TypeToken<List<Actor>>() {}.getType();
            List<Actor> actors = fabricClient.getGson().fromJson(resultJson, listType);

            List<CompletableFuture<Actor>> futures = actors.stream()
                    .map(actor -> CompletableFuture.supplyAsync(() -> enrichSingleActorWithIpfs(actor)))
                    .collect(Collectors.toList());

            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Fehler beim Abrufen aller Akteure: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }


    /**
     * Private Hilfsmethode, die ein einzelnes Actor-Objekt mit IPFS-Daten anreichert.
     * @param actor Das zu anreichernde Actor-Objekt.
     * @return Der angereicherte Actor oder null, wenn der Input null war.
     */
    private Actor enrichSingleActorWithIpfs(Actor actor) {
        if (actor == null) {
            return null;
        }

        if (StringUtils.hasText(actor.getIpfsLink())) {
            final String cleanHash = actor.getIpfsLink().replace("ipfs://", "").trim();
            try {
                if (!cleanHash.isEmpty()) {
                    Type dataType = new TypeToken<Map<String, Object>>() {}.getType();
                    Map<String, Object> ipfsData = ipfsClient.getObject(cleanHash, dataType);
                    actor.setIpfsData(ipfsData);
                    logger.debug("Successfully enriched Actor {} with IPFS data.", actor.getActorId());
                }
            } catch (IOException e) {
                logger.warn("Could not fetch or parse IPFS data for Actor {}: {}", actor.getActorId(), e.getMessage());
            }
        }
        return actor;
    }

    public Actor updateActor(String actorId, String name, String email, Map<String, Object> ipfsData) throws Exception {
        String finalIpfsLink = "";

        // 1. IPFS-Daten verarbeiten, falls vorhanden
        if (ipfsData != null && !ipfsData.isEmpty()) {
            logger.info("Verarbeite neue 'ipfsData' für Actor-ID: {}", actorId);
            try {
                // Konvertiere die Map in einen JSON-String
                String ipfsJson = fabricClient.getGson().toJson(ipfsData);
                // Lade das JSON-Objekt auf IPFS hoch und erhalte den Hash
                String ipfsHash = ipfsClient.addObject(ipfsJson);
                if (StringUtils.hasText(ipfsHash)) {
                    finalIpfsLink = ipfsHash;
                    logger.info("Neuer IPFS-Link für Actor-Update erstellt: {}", finalIpfsLink);
                }
            } catch (Exception e) {
                logger.error("Fehler beim Hochladen der IPFS-Daten für Actor {}: {}", actorId, e.getMessage(), e);
                throw new RuntimeException("Fehler beim IPFS-Upload.", e);
            }
        }

        // 2. Transaktion an den Chaincode senden
        logger.debug("Sende 'updateActor' Transaktion für ID: {}", actorId);
        String resultJson = fabricClient.submitGenericTransaction(
                "updateActor",
                actorId,
                name,
                email,
                finalIpfsLink
        );

        // 3. Ergebnis deserialisieren, anreichern und zurückgeben
        Actor updatedActor = fabricClient.getGson().fromJson(resultJson, Actor.class);
        return enrichSingleActorWithIpfs(updatedActor);
    }



    public ActorMapper getActorMapper() {
        return actorMapper;
    }
}
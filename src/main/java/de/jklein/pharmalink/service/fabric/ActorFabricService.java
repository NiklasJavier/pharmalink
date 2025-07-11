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
import java.util.Objects;
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

    public List<Actor> getActorsByRole(String role) {
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryActorsByRole", role);
            Type listType = new TypeToken<List<Actor>>() {}.getType();
            List<Actor> actors = fabricClient.getGson().fromJson(resultJson, listType);
            return enrichActorList(actors);
        } catch (Exception e) {
            logger.error("Fehler beim Abrufen der Akteure für die Rolle '{}': {}", role, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<Actor> getAllActors() {
        try {
            String resultJson = fabricClient.evaluateGenericTransaction("queryAllActors");
            Type listType = new TypeToken<List<Actor>>() {}.getType();
            List<Actor> actors = fabricClient.getGson().fromJson(resultJson, listType);
            return enrichActorList(actors);
        } catch (Exception e) {
            logger.error("Fehler beim Abrufen aller Akteure: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<Actor> enrichActorList(List<Actor> actors) {
        if (actors == null || actors.isEmpty()) {
            return Collections.emptyList();
        }
        List<CompletableFuture<Actor>> futures = actors.stream()
                .map(actor -> CompletableFuture.supplyAsync(() -> enrichSingleActorWithIpfs(actor)))
                .collect(Collectors.toList());
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Actor enrichSingleActorWithIpfs(Actor actor) {
        if (actor == null) {
            return null;
        }
        if (StringUtils.hasText(actor.getIpfsLink())) {
            final String cleanHash = actor.getIpfsLink().replace("ipfs://", "").trim();
            try {
                if (StringUtils.hasText(cleanHash)) {
                    Type dataType = new TypeToken<Map<String, Object>>() {}.getType();
                    Map<String, Object> ipfsData = ipfsClient.getObject(cleanHash, dataType);
                    actor.setIpfsData(ipfsData);
                    logger.debug("Akteur {} erfolgreich mit IPFS-Daten angereichert.", actor.getActorId());
                }
            } catch (IOException e) {
                logger.warn("Konnte IPFS-Daten für Akteur {} nicht abrufen oder verarbeiten: {}", actor.getActorId(), e.getMessage());
            }
        }
        return actor;
    }

    public Actor updateActor(String actorId, String name, String email, Map<String, Object> ipfsData) throws Exception {
        String finalIpfsLink = "";

        if (ipfsData != null && !ipfsData.isEmpty()) {
            logger.info("Verarbeite neue 'ipfsData' für Akteur-ID: {}", actorId);
            try {
                String ipfsJson = fabricClient.getGson().toJson(ipfsData);
                String ipfsHash = ipfsClient.addObject(ipfsJson);
                if (StringUtils.hasText(ipfsHash)) {
                    finalIpfsLink = ipfsHash;
                    logger.info("Neuer IPFS-Link für Akteur-Update erstellt: {}", finalIpfsLink);
                }
            } catch (Exception e) {
                logger.error("Fehler beim Hochladen der IPFS-Daten für Akteur {}: {}", actorId, e.getMessage(), e);
                throw new RuntimeException("Fehler beim IPFS-Upload.", e);
            }
        }

        logger.debug("Sende 'updateActor'-Transaktion für ID: {}", actorId);
        String resultJson = fabricClient.submitGenericTransaction(
                "updateActor",
                actorId,
                name,
                email,
                finalIpfsLink
        );
        Actor updatedActor = fabricClient.getGson().fromJson(resultJson, Actor.class);
        return enrichSingleActorWithIpfs(updatedActor);
    }

    public Optional<Actor> getEnrichedActorById(String actorId) {
        try {
            Actor actor = fabricClient.evaluateTransaction("queryActorById", Actor.class, actorId);
            return Optional.ofNullable(enrichSingleActorWithIpfs(actor));
        } catch (Exception e) {
            logger.error("Fehler beim Abrufen des Akteurs mit ID '{}': {}", actorId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public ActorMapper getActorMapper() {
        return actorMapper;
    }
}
// src/main/java/de/jklein/pharmalink/runner/AppDataInitializer.java
package de.jklein.pharmalink.runner;

import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.client.fabric.dto.ActorDto;
import de.jklein.pharmalink.service.CurrentActorHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AppFabricInitializer implements CommandLineRunner {

    private final FabricClient fabricClient;
    private final CurrentActorHolder actorHolder;

    @Value("${ipfs.email}")
    private String userEmail;

    @Value("${ipfs.ipfs-link}")
    private String userIpfsLink;

    @Autowired
    public AppFabricInitializer(FabricClient fabricClient, CurrentActorHolder actorHolder) {
        this.fabricClient = fabricClient;
        this.actorHolder = actorHolder;
    }

    @Override
    public void run(String... args) {
        try {
            String actorJson = fabricClient.submitGenericTransaction("initCall", userEmail, userIpfsLink);

            ActorDto initializedActor = fabricClient.getGson().fromJson(actorJson, ActorDto.class);
            actorHolder.setCurrentActor(initializedActor);

            System.out.println("\ninitCall erfolgreich ausgeführt. Initialisierter Akteur:");
            System.out.println("   ID: " + initializedActor.getActorId());
            System.out.println("   Rolle: " + initializedActor.getRole());
            System.out.println("   E-Mail: " + initializedActor.getEmail());
            System.out.println("   IPFS Link: " + (initializedActor.getIpfsLink() != null && !initializedActor.getIpfsLink().isEmpty() ? initializedActor.getIpfsLink() : "N/A"));

        } catch (Exception e) {
            System.err.println("Fehler bei der Ausführung von initCall:");
            e.printStackTrace();
        }
    }
}
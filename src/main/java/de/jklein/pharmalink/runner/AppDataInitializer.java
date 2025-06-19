// src/main/java/de/jklein/pharmalink/runner/AppDataInitializer.java
package de.jklein.pharmalink.runner;

import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.dto.ActorDto;
import de.jklein.pharmalink.service.CurrentActorHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AppDataInitializer implements CommandLineRunner {

    private final FabricClient fabricClient;
    private final CurrentActorHolder actorHolder;

    @Value("${user.email}")
    private String userEmail;

    @Value("${user.ipfs-link}")
    private String userIpfsLink;

    @Autowired
    public AppDataInitializer(FabricClient fabricClient, CurrentActorHolder actorHolder) {
        this.fabricClient = fabricClient;
        this.actorHolder = actorHolder;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n========================================================");
        System.out.println("▶️  FÜHRE initCall FÜR DEN ANWENDUNGSBENUTZER BEIM START AUS...");
        System.out.println("========================================================");
        try {
            // Dies funktioniert, sobald submitGenericTransaction einen String zurückgibt
            String actorJson = fabricClient.submitGenericTransaction("initCall", userEmail, userIpfsLink);

            // Dies funktioniert, sobald getGson() in FabricClient existiert
            ActorDto initializedActor = fabricClient.getGson().fromJson(actorJson, ActorDto.class);
            actorHolder.setCurrentActor(initializedActor);

            System.out.println("\n✅ initCall erfolgreich ausgeführt. Initialisierter Akteur:");
            System.out.println("   ID: " + initializedActor.getActorId());
            System.out.println("   Rolle: " + initializedActor.getRole());
            System.out.println("   E-Mail: " + initializedActor.getEmail());
            System.out.println("   IPFS Link: " + (initializedActor.getIpfsLink() != null && !initializedActor.getIpfsLink().isEmpty() ? initializedActor.getIpfsLink() : "N/A"));

        } catch (Exception e) {
            System.err.println("\n❌ Fehler bei der Ausführung von initCall:");
            e.printStackTrace();
        }
        System.out.println("\n========================================================");
    }
}
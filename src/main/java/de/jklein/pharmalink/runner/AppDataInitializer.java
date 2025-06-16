package de.jklein.pharmalink.runner;

import de.jklein.pharmalink.client.fabric.FabricClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AppDataInitializer implements CommandLineRunner {

    @Autowired
    private FabricClient fabricClient; // Nutzt den generischen Client

    @Override
    public void run(String... args) {
        System.out.println("\n========================================================");
        System.out.println("▶️  FÜHRE InitLedger BEIM START AUS...");
        System.out.println("========================================================");
        try {
            // Ruft die InitLedger-Funktion im Chaincode auf, um Beispieldaten zu erstellen
            fabricClient.submitGenericTransaction("InitLedger");
            System.out.println("\n✅ InitLedger erfolgreich ausgeführt.");
        } catch (Exception e) {
            System.err.println("\n❌ Fehler bei der Ausführung von InitLedger:");
            e.printStackTrace();
        }
        System.out.println("\n========================================================");
    }
}
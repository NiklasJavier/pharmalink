package de.jklein.pharmalink.service;

import com.google.gson.Gson;
import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.domain.Medication;
import de.jklein.pharmalink.web.dto.CreateMedicationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MedicationService {

    @Autowired
    private FabricClient fabricClient;

    private final Gson gson = new Gson();

    /**
     * Erstellt ein neues Medikamenten-Asset.
     */
    public Medication createMedication(CreateMedicationRequest request) throws Exception {
        String assetId = "med:" + UUID.randomUUID();
        Medication medication = new Medication(
                assetId,
                request.owner(),
                request.name(),
                request.dosage(),
                request.manufacturer(),
                request.appraisedValue()
        );

        // Der Aufruf hier ist bereits korrekt (3 Argumente)
        fabricClient.submitCreateTransaction("CreateAsset", medication.getAssetId(), medication);

        return medication;
    }

    /**
     * Findet ein Medikament anhand seiner ID und gibt es als typisiertes Objekt zurück.
     */
    public Medication findById(String assetId) throws Exception {
        System.out.println("--> Lese Asset mit ID: " + assetId);

        // KORREKTUR HIER: Der Aufruf passt jetzt zur Methode im FabricClient (3 Argumente)
        Medication medication = fabricClient.evaluateTransaction("ReadAsset", assetId, Medication.class);

        if (medication == null) {
            System.out.println("*** Asset mit ID " + assetId + " nicht gefunden.");
        } else {
            System.out.println("*** Gelesenes Asset: " + medication);
        }

        return medication;
    }
}
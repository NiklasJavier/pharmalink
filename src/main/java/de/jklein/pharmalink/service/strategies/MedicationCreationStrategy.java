package de.jklein.pharmalink.service.strategies;

import com.google.gson.Gson;
import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.domain.Medication;
import de.jklein.pharmalink.domain.TrackableAsset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MedicationCreationStrategy implements AssetCreationStrategy {

    @Autowired
    private FabricClient fabricClient;

    private final Gson gson = new Gson();

    @Override
    public boolean supports(String assetType) {
        return "MEDICATION".equalsIgnoreCase(assetType);
    }

    @Override
    public void createAsset(TrackableAsset asset) throws Exception {
        if (!(asset instanceof Medication medication)) {
            throw new IllegalArgumentException("Asset muss vom Typ Medication sein für diese Strategie.");
        }

        System.out.println("MedicationCreationStrategy wird ausgeführt für Asset: " + medication.getAssetId());
        String medicationJson = gson.toJson(medication);

        // KORREKTUR: Wir rufen die existierende, generische Methode im FabricClient auf.
        // Annahme laut vorherigem Code: Chaincode-Funktion ist "CreateAsset".
        fabricClient.submitGenericTransaction("CreateAsset", medication.getAssetId(), medicationJson);
    }
}
package de.jklein.pharmalink.web.rest;

import de.jklein.pharmalink.domain.TrackableAsset;
import de.jklein.pharmalink.service.AssetFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;

@RestController
@RequestMapping("/api/assets")
public class AssetResource {

    @Autowired
    private AssetFactory assetFactory;

    /**
     * Ein einziger, dynamischer Endpunkt zum Erstellen verschiedener Asset-Typen.
     * Spring & Jackson deserialisieren den Request Body automatisch in das richtige
     * Unterklassen-Objekt (Medication, Shipment, etc.) basierend auf dem "assetType"-Feld im JSON.
     * @param asset Das deserialisierte Asset-Objekt.
     * @return HTTP 201 Created mit einem Link zum (hypothetischen) neuen Asset.
     */
    @PostMapping("/create")
    public ResponseEntity<String> createGenericAsset(@RequestBody TrackableAsset asset) {
        try {
            assetFactory.createAsset(asset);
            // Erstellt eine Erfolgsantwort mit dem Status 201 Created.
            return ResponseEntity.created(new URI("/api/assets/" + asset.getAssetId()))
                    .body("Asset '" + asset.getAssetId() + "' vom Typ '" + asset.getAssetType() + "' erfolgreich erstellt.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Ein interner Fehler ist aufgetreten: " + e.getMessage());
        }
    }
}
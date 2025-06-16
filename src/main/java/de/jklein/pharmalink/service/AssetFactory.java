package de.jklein.pharmalink.service;

import de.jklein.pharmalink.domain.TrackableAsset;
import de.jklein.pharmalink.service.strategies.AssetCreationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssetFactory {

    private final List<AssetCreationStrategy> strategies;

    // Spring injiziert hier automatisch eine Liste aller Beans, die das Interface implementieren
    @Autowired
    public AssetFactory(List<AssetCreationStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * Findet die passende Strategie für das gegebene Asset und führt sie aus.
     * @param asset Das Asset, das erstellt werden soll.
     */
    public void createAsset(TrackableAsset asset) throws Exception {
        // Finde die passende Strategie für den Asset-Typ
        Optional<AssetCreationStrategy> strategy = strategies.stream()
                .filter(s -> s.supports(asset.getAssetType()))
                .findFirst();

        if (strategy.isPresent()) {
            strategy.get().createAsset(asset);
        } else {
            throw new IllegalArgumentException("Keine passende Strategie für den Asset-Typ gefunden: " + asset.getAssetType());
        }
    }
}
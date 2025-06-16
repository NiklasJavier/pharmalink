package de.jklein.pharmalink.service.strategies;

import de.jklein.pharmalink.domain.TrackableAsset;

/**
 * Definiert den Vertrag für eine Strategie, die weiß, wie ein bestimmter Asset-Typ erstellt wird.
 */
public interface AssetCreationStrategy {

    /**
     * Prüft, ob diese Strategie für den gegebenen Typnamen zuständig ist.
     * @param assetType Der Typname aus dem JSON (z.B. "MEDICATION").
     * @return true, wenn zuständig, sonst false.
     */
    boolean supports(String assetType);

    /**
     * Führt die Erstellungslogik für das gegebene Asset aus.
     * @param asset Das deserialisierte Asset-Objekt.
     */
    void createAsset(TrackableAsset asset) throws Exception;
}
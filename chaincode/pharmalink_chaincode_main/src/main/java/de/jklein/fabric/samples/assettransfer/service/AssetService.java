// src/main/java/de/jklein/fabric/samples/assettransfer/service/AssetService.java
package de.jklein.fabric.samples.assettransfer.service;

import com.owlike.genson.Genson; //
import de.jklein.fabric.samples.assettransfer.model.IAsset;
import org.hyperledger.fabric.contract.Context; //
import org.hyperledger.fabric.shim.ChaincodeException; //

import java.util.Objects; //

/**
 * Service zum Abstrahieren der direkten Ledger-Interaktionen.
 * Verwaltet das Speichern, Abrufen und Prüfen der Existenz von IAsset-Objekten.
 */
public class AssetService {

    final Genson genson; //

    /**
     * Konstruktor für den AssetService.
     * @param genson Die Genson-Instanz zur Serialisierung/Deserialisierung.
     */
    public AssetService(final Genson genson) { //
        Objects.requireNonNull(genson, "Genson instance cannot be null"); //
        this.genson = genson; //
    }

    /**
     * Speichert ein IAsset-Objekt im Ledger.
     *
     * @param ctx Der Transaktionskontext.
     * @param asset Das zu speichernde Asset-Objekt (muss IAsset implementieren).
     * @throws ChaincodeException wenn die Serialisierung fehlschlägt.
     */
    public void putAsset(final Context ctx, final IAsset asset) {
        if (asset == null) {
            throw new ChaincodeException("Asset kann nicht null sein", "ASSET_NULL_ERROR");
        }
        if (asset.getKey() == null || asset.getKey().isEmpty()) {
            throw new ChaincodeException("Asset-Schlüssel kann nicht null oder leer sein", "ASSET_KEY_ERROR");
        }
        try {
            String json = genson.serialize(asset); //
            ctx.getStub().putStringState(asset.getKey(), json); //
        } catch (Exception e) {
            throw new ChaincodeException(String.format("Fehler beim Serialisieren von Asset mit Schlüssel '%s': %s", asset.getKey(), e.getMessage()), "ASSET_SERIALIZATION_ERROR");
        }
    }

    /**
     * Ruft ein Asset anhand seines Schlüssels aus dem Ledger ab und konvertiert es in den angegebenen Typ.
     *
     * @param <T> Der Zieltyp, muss IAsset implementieren.
     * @param ctx Der Transaktionskontext.
     * @param key Der Schlüssel des abzurufenden Assets.
     * @param assetType Die Klasse des Zieltyps.
     * @return Das deserialisierte Asset-Objekt oder null, wenn es nicht existiert.
     * @throws ChaincodeException wenn das Asset nicht gefunden wird oder die Deserialisierung fehlschlägt.
     */
    public <T extends IAsset> T getAssetByKey(final Context ctx, final String key, final Class<T> assetType) {
        String json = ctx.getStub().getStringState(key); //
        if (json == null || json.isEmpty()) { //
            throw new ChaincodeException(String.format("Asset mit Schlüssel '%s' vom Typ '%s' existiert nicht.", key, assetType.getSimpleName()), "ASSET_NOT_FOUND"); //
        }
        try {
            T asset = genson.deserialize(json, assetType); //
            if (asset == null) {
                throw new ChaincodeException(String.format("Deserialisiertes Asset '%s' ist null", key), "ASSET_DESERIALIZATION_ERROR");
            }
            return asset;
        } catch (Exception e) {
            throw new ChaincodeException(String.format("Fehler beim Deserialisieren von Asset '%s' als Typ '%s': %s", key, assetType.getSimpleName(), e.getMessage()), "ASSET_DESERIALIZATION_ERROR");
        }
    }

    /**
     * Prüft, ob ein Asset mit dem angegebenen Schlüssel im Ledger existiert.
     *
     * @param ctx Der Transaktionskontext.
     * @param key Der zu prüfende Schlüssel.
     * @return true, wenn das Asset existiert, sonst false.
     */
    public boolean assetExists(final Context ctx, final String key) {
        String json = ctx.getStub().getStringState(key); //
        return (json != null && !json.isEmpty()); //
    }

    /**
     * Löscht ein Asset mit dem angegebenen Schlüssel aus dem Ledger.
     *
     * @param ctx Der Transaktionskontext.
     * @param key Der Schlüssel des zu löschenden Assets.
     */
    public void deleteAsset(final Context ctx, final String key) {
        ctx.getStub().delState(key); //
    }
}

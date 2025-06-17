// src/main/java/de/jklein/fabric/samples/assettransfer/permission/AssetStatusException.java
package de.jklein.fabric.samples.assettransfer.permission;

import org.hyperledger.fabric.shim.ChaincodeException; //

/**
 * Spezielle Exception für Asset-Status-Fehler.
 */
public class AssetStatusException extends ChaincodeException { //

    private static final String DEFAULT_TYPE = "INVALID_ASSET_STATE"; //

    /**
     * Erstellt eine neue AssetStatusException mit der angegebenen Nachricht.
     *
     * @param message Die Fehlermeldung
     */
    public AssetStatusException(final String message) {
        super(message, DEFAULT_TYPE); //
    }

    /**
     * Erstellt eine neue AssetStatusException mit der angegebenen Nachricht und einem spezifischen Typ.
     * @param message Die Fehlermeldung
     * @param type Der spezifische Fehler-Typ (z.B. "ASSET_BLOCKED_BY_TAG")
     */
    public AssetStatusException(final String message, final String type) {
        super(message, type);
    }

    /**
     * Erstellt eine neue AssetStatusException mit einer formatierten Nachricht über den
     * erwarteten und tatsächlichen Status eines Assets.
     *
     * @param assetId ID des betroffenen Assets
     * @param expectedStatus Der erwartete Status
     * @param actualStatus Der tatsächliche Status
     * @return Eine neue AssetStatusException
     */
    public static AssetStatusException createForStatus(final String assetId, final String expectedStatus, final String actualStatus) {
        String message = String.format("Asset %s muss im Status '%s' sein, ist aber '%s'.", //
                assetId, expectedStatus, actualStatus != null ? actualStatus : "<kein Status>"); //
        return new AssetStatusException(message); //
    }
}

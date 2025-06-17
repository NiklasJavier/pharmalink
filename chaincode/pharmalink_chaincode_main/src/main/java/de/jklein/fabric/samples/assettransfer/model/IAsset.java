// src/main/java/de/jklein/fabric/samples/assettransfer/model/IAsset.java
package de.jklein.fabric.samples.assettransfer.model;

/**
 * Gemeinsames Interface für alle primären Chaincode-Assets,
 * die im Ledger persistiert werden. Ermöglicht polymorphe Operationen
 * durch generische Services.
 */
public interface IAsset {

    /**
     * Gibt den eindeutigen Schlüssel des Assets zurück, unter dem es im Ledger gespeichert wird.
     * @return Der Ledger-Schlüssel des Assets.
     */
    String getKey();

    /**
     * Gibt den aktuellen Status des Assets zurück (z.B. ERSTELLT, FREIGEGEBEN, PENDING_TRANSFER).
     * @return Der Status des Assets.
     */
    String getStatus();

    /**
     * Gibt den regulatorischen Status des Assets zurück (optional, kann auch über Tags abgebildet werden).
     * @return Der regulatorische Status des Assets.
     */
    String getRegulatoryStatus();

    /**
     * Prüft, ob das Asset derzeit durch einen regulatorischen Tag blockiert ist.
     * Muss von implementierenden Klassen basierend auf deren Tag-Listen implementiert werden.
     * @return true, wenn das Asset blockiert ist, sonst false.
     */
    boolean isLockedByRegulator();

    /**
     * Gibt die Akteur-ID zurück, die mit diesem Asset als primärer Verantwortlicher (z.B. Ersteller, aktueller Besitzer)
     * assoziiert ist, falls zutreffend. Dies ist eine generische Methode, die je nach Asset variieren kann.
     * @return Die assoziierte Akteur-ID oder null, falls nicht zutreffend.
     */
    String getAssociatedActorId();
}

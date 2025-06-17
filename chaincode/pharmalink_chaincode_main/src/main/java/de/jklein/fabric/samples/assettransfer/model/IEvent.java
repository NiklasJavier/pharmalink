// src/main/java/de/jklein/fabric/samples/assettransfer/model/IEvent.java
package de.jklein.fabric.samples.assettransfer.model;

/**
 * Gemeinsames Interface für alle Event-Objekte, die im Ledger geloggt werden.
 * Ermöglicht Polymorphismus für den EventService.
 */
public interface IEvent {

    /**
     * Gibt den eindeutigen Schlüssel des Events im Ledger zurück.
     * @return Der Ledger-Schlüssel des Events.
     */
    String getEventId();

    /**
     * Gibt den Zeitstempel des Events zurück.
     * @return Der Zeitstempel im ISO-Format.
     */
    String getTimestamp();

    /**
     * Gibt den Schlüssel des Assets zurück, auf das sich das Event bezieht.
     * @return Der Schlüssel des betroffenen Assets.
     */
    String getRelatedAssetKey();

    /**
     * Gibt die Akteur-ID zurück, die das Event ausgelöst hat.
     * @return Die Akteur-ID des auslösenden Akteurs.
     */
    String getTriggeringActorId();

    /**
     * Gibt den Typ des Events zurück (z.B. "MedicationCreated", "TransferCompleted").
     * @return Der Event-Typ als String.
     */
    String getEventType();
}

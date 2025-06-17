// src/main/java/de/jklein/fabric/samples/assettransfer/permission/RoleConstants.java
package de.jklein.fabric.samples.assettransfer.permission;

/**
 * Konstanten für die verschiedenen Rollen im System und Asset-Status.
 * Zentralisiert die Definitionen, um Tippfehler zu vermeiden.
 */
public final class RoleConstants {

    private RoleConstants() {
        // Verhindert Instanziierung
    }

    // Rollen
    public static final String HERSTELLER = "hersteller"; //
    public static final String GROSSHAENDLER = "grosshaendler"; //
    public static final String LIEFERANT = GROSSHAENDLER; // Alias für Großhändler
    public static final String APOTHEKE = "apotheke"; //
    public static final String BEHOERDE = "behoerde"; //

    // Akteur-Lebenszyklus-Status
    public static final String PENDING_APPROVAL = "pending_approval";
    public static final String APPROVED = "approved";
    public static final String SUSPENDED = "suspended";
    public static final String RETIRED = "retired";


    // Asset-Lebenszyklus-Status
    public static final String ERSTELLT = "Erstellt"; //
    public static final String FREIGEGEBEN = "Freigegeben"; //
    public static final String GESPERRT = "Gesperrt"; //
    public static final String PENDING_TRANSFER = "pending_transfer"; // Neu für Zwei-Phasen-Transfer
    public static final String TRANSFERRED = "Transferred"; // Finaler Status nach erfolgreichem Transfer (optional, kann auch FREIGEGEBEN bleiben)
}

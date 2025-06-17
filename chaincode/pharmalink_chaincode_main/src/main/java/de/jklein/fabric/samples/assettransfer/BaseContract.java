// src/main/java/de/jklein/fabric/samples/assettransfer/BaseContract.java
package de.jklein.fabric.samples.assettransfer;

import com.owlike.genson.Genson; //
import de.jklein.fabric.samples.assettransfer.permission.AuthorizationService;
import de.jklein.fabric.samples.assettransfer.service.ActorService;
import de.jklein.fabric.samples.assettransfer.service.AssetService;
import de.jklein.fabric.samples.assettransfer.service.EventService;
import org.hyperledger.fabric.contract.ContractInterface; //

/**
 * Basisklasse für alle Smart Contracts im System.
 * Stellt gemeinsame Funktionalität bereit, die von allen spezifischen Contracts verwendet wird.
 * Injiziert zentrale Service-Abhängigkeiten.
 */
public abstract class BaseContract implements ContractInterface { //

    // Genson Instanz für JSON-Serialisierung/Deserialisierung
    protected final Genson genson = new Genson();

    // Injezierte Services
    protected final AuthorizationService authService;
    protected final AssetService assetService;
    protected final ActorService actorService;
    protected final EventService eventService;


    /**
     * Standardkonstruktor für Hyperledger Fabric, der von der Fabric-Laufzeit aufgerufen wird.
     * Services müssen hier manuell instanziiert werden, da Fabric keine automatische DI unterstützt.
     * In einer echten Umgebung können diese Singleton-Instanzen sein.
     */
    public BaseContract() {
        // Services instanziieren.
        // Genson ist eine einfache Instanz ohne Abhängigkeiten.
        // AssetService benötigt Genson.
        // ActorService benötigt AssetService.
        // EventService benötigt AssetService.
        // AuthorizationService benötigt ActorService und AssetService.
        // Die Reihenfolge ist wichtig, um NullPointerExceptions zu vermeiden.

        this.assetService = new AssetService(this.genson);
        this.actorService = new ActorService(this.assetService);
        this.eventService = new EventService(this.assetService);
        this.authService = new AuthorizationService(this.actorService, this.assetService);
    }

    /**
     * Konstruktor für Testzwecke oder manuelle Injektion von Mock-Services.
     *
     * @param authService Der AuthorizationService.
     * @param assetService Der AssetService.
     * @param actorService Der ActorService.
     * @param eventService Der EventService.
     */
    public BaseContract(final AuthorizationService authService, final AssetService assetService, final ActorService actorService, final EventService eventService) {
        this.authService = authService;
        this.assetService = assetService;
        this.actorService = actorService;
        this.eventService = eventService;
    }

    // Die folgenden Hilfsmethoden aus der ursprünglichen BaseContract.java können entfernt werden,
    // da ihre Funktionalität nun im AssetService gekapselt ist.
    // protected void putState(final Context ctx, final String key, final Object asset) { ... }
    // protected <T> T getState(final Context ctx, final String key, final Class<T> valueType) { ... }
    // protected boolean assetExists(final Context ctx, final String key) { ... }
    // protected void deleteState(final Context ctx, final String key) { ... }

}

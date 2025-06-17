// src/main/java/de/jklein/fabric/samples/assettransfer/service/ActorService.java
package de.jklein.fabric.samples.assettransfer.service;

import de.jklein.fabric.samples.assettransfer.model.Actor;
import de.jklein.fabric.samples.assettransfer.permission.RoleConstants;
import org.hyperledger.fabric.contract.Context; //
import org.hyperledger.fabric.shim.ChaincodeException; //

import java.util.Objects; //

/**
 * Service zur Verwaltung von Akteuren in der Lieferkette.
 * Beinhaltet Registrierung, Freigabe und Statusabfragen von Akteuren.
 */
public class ActorService {

    private final AssetService assetService; // Abhängigkeit zum AssetService für Persistenz

    private static final String ACTOR_KEY_PREFIX = "ACTOR_";
    private static final String ACTOR_ALREADY_EXISTS = "ACTOR_ALREADY_EXISTS";
    private static final String ACTOR_NOT_FOUND = "ACTOR_NOT_FOUND";
    private static final String INVALID_ACTOR_STATE = "INVALID_ACTOR_STATE";
    private static final String ACCESS_DENIED_ACTOR_MANAGEMENT = "ACCESS_DENIED_ACTOR_MANAGEMENT";


    /**
     * Konstruktor für den ActorService.
     * @param assetService Der AssetService zur Interaktion mit dem Ledger.
     */
    public ActorService(final AssetService assetService) {
        Objects.requireNonNull(assetService, "AssetService cannot be null"); //
        this.assetService = assetService;
    }

    /**
     * Registriert einen neuen Akteur im System.
     * @param ctx Der Transaktionskontext.
     * @param actorId Die eindeutige ID des Akteurs (vom Aufrufer vorgeschlagen).
     * @param actorName Der Name des Akteurs/der Organisation.
     * @param roleType Die Rolle des Akteurs (z.B. RoleConstants.HERSTELLER).
     * @param publicKey Der öffentliche Schlüssel des Akteurs-Zertifikats (Base64-kodiert).
     * @return Das neu erstellte Actor-Objekt.
     * @throws ChaincodeException wenn der Akteur bereits existiert.
     */
    public Actor registerNewActor(final Context ctx, final String actorId, final String actorName, final String roleType, final String publicKey) {
        String actorKey = ACTOR_KEY_PREFIX + actorId;

        // Prüfen, ob der Akteur bereits existiert
        if (assetService.assetExists(ctx, actorKey)) { //
            throw new ChaincodeException(String.format("Akteur '%s' existiert bereits.", actorId), ACTOR_ALREADY_EXISTS); //
        }

        // Akteur-Status basierend auf der Rolle setzen
        String initialStatus;
        if (RoleConstants.BEHOERDE.equalsIgnoreCase(roleType)) { // Regulator erhält sofort APPROVED Status
            initialStatus = RoleConstants.APPROVED; //
        } else { // Alle anderen müssen von der Behörde freigegeben werden
            initialStatus = RoleConstants.PENDING_APPROVAL; //
        }

        Actor newActor = new Actor(actorId, actorName, roleType, initialStatus, publicKey);
        assetService.putAsset(ctx, newActor);

        return newActor;
    }

    /**
     * Genehmigt die Registrierung eines Akteurs durch Setzen seines Status auf 'APPROVED'.
     * Nur von der Behörde aufrufbar (Prüfung im AuthorizationService).
     * @param ctx Der Transaktionskontext.
     * @param actorId Die ID des zu genehmigenden Akteurs.
     * @return Das aktualisierte Actor-Objekt.
     * @throws ChaincodeException wenn der Akteur nicht existiert oder nicht im Status PENDING_APPROVAL ist.
     */
    public Actor approveActorRegistration(final Context ctx, final String actorId) {
        String actorKey = ACTOR_KEY_PREFIX + actorId;

        Actor actorToApprove = getActorById(ctx, actorId);

        // Prüfung, ob der Akteur den richtigen Status zum Genehmigen hat
        if (!actorToApprove.getStatus().equals(RoleConstants.PENDING_APPROVAL)) { //
            throw new ChaincodeException(String.format("Akteur '%s' kann nicht genehmigt werden, da der Status nicht '%s' ist. Aktueller Status: '%s'.",
                    actorId, RoleConstants.PENDING_APPROVAL, actorToApprove.getStatus()), INVALID_ACTOR_STATE); //
        }

        // Erstellen eines neuen Akteur-Objekts mit aktualisiertem Status (Immutable-Pattern)
        Actor approvedActor = new Actor(
                actorToApprove.getActorId(),
                actorToApprove.getActorName(),
                actorToApprove.getRoleType(),
                RoleConstants.APPROVED, //
                actorToApprove.getPublicKey()
        );
        assetService.putAsset(ctx, approvedActor);

        return approvedActor;
    }

    /**
     * Ruft ein Akteur-Objekt anhand seiner ID ab.
     * @param ctx Der Transaktionskontext.
     * @param actorId Die ID des Akteurs.
     * @return Das Actor-Objekt.
     * @throws ChaincodeException wenn der Akteur nicht gefunden wird.
     */
    public Actor getActorById(final Context ctx, final String actorId) {
        String actorKey = ACTOR_KEY_PREFIX + actorId;
        return assetService.getAssetByKey(ctx, actorKey, Actor.class);
    }

    /**
     * Prüft, ob ein Akteur den Status 'APPROVED' hat.
     * @param ctx Der Transaktionskontext.
     * @param actorId Die ID des zu prüfenden Akteurs.
     * @return true, wenn der Akteur 'APPROVED' ist, sonst false.
     */
    public boolean isActorCurrentlyApproved(final Context ctx, final String actorId) {
        try {
            Actor actor = getActorById(ctx, actorId);
            return RoleConstants.APPROVED.equals(actor.getStatus()); //
        } catch (ChaincodeException e) {
            if (ACTOR_NOT_FOUND.equals(e.getPayload())) { // Prüfen auf spezifischen Fehlercode
                return false; // Akteur nicht gefunden, also auch nicht APPROVED
            }
            throw e; // Andere Fehler weiterwerfen
        }
    }

    /**
     * Gibt die Rolle eines Akteurs anhand seiner ID zurück.
     * @param ctx Der Transaktionskontext.
     * @param actorId Die ID des Akteurs.
     * @return Die Rolle des Akteurs.
     * @throws ChaincodeException wenn der Akteur nicht gefunden wird.
     */
    public String getActorRoleType(final Context ctx, final String actorId) {
        Actor actor = getActorById(ctx, actorId);
        return actor.getRoleType();
    }

    /**
     * Ermittelt die Akteur-ID des Aufrufers basierend auf seiner Client Identity.
     * Diese Methode ist entscheidend, um die aufrufende Akteur-ID zu validieren.
     * @param ctx Der Transaktionskontext.
     * @return Die Akteur-ID des Aufrufers.
     * @throws ChaincodeException wenn die Akteur-ID nicht ermittelt werden kann.
     */
    public String getCallerActorId(final Context ctx) {
        // Annahme: Die Client Identity ID des Aufrufers ist direkt seine registrierte Akteur-ID.
        // In realen Fabric-Szenarien ist die Client Identity ID (z.B. x509::/C=US/ST=NC/O=org1.example.com/CN=admin::/C=US/ST=NC/O=org1.example.com/CN=admin)
        // und das Attribut "role" zwei verschiedene Dinge.
        // Für dieses Beispiel gehen wir davon aus, dass die ID des Zertifikats, die der Chaincode sieht,
        // direkt als 'actorId' im Ledger registriert ist.
        // Alternativ müsste man hier einen Lookup-Mechanismus implementieren, der
        // die Client Identity ID zu einer Actor ID im Ledger mappt.
        String callerId = ctx.getClientIdentity().getId(); //
        if (callerId == null || callerId.isEmpty()) {
            throw new ChaincodeException("Konnte Akteur-ID des Aufrufers nicht ermitteln.", "CALLER_ID_MISSING");
        }
        return callerId;
    }
}

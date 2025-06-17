// src/main/java/de/jklein/fabric/samples/assettransfer/permission/AuthorizationService.java
package de.jklein.fabric.samples.assettransfer.permission;

import de.jklein.fabric.samples.assettransfer.model.Actor;
import de.jklein.fabric.samples.assettransfer.model.Batch;
import de.jklein.fabric.samples.assettransfer.model.Medication;
import de.jklein.fabric.samples.assettransfer.model.MedicationUnit;
import de.jklein.fabric.samples.assettransfer.model.Tag;
import de.jklein.fabric.samples.assettransfer.service.ActorService;
import de.jklein.fabric.samples.assettransfer.service.AssetService;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;

import java.util.Objects;

/**
 * Eine zentrale Klasse zur Verwaltung aller Berechtigungsprüfungen im Chaincode.
 */
public class AuthorizationService {

    private static final String ROLE_ATTRIBUTE = "role";
    private static final String ACCESS_DENIED_ROLE = "ACCESS_DENIED_ROLE";
    private static final String ACCESS_DENIED_NO_ROLE = "ACCESS_DENIED_NO_ROLE";
    private static final String ACCESS_DENIED_OWNERSHIP = "ACCESS_DENIED_OWNERSHIP";
    private static final String ACCESS_DENIED_CREATORSHIP = "ACCESS_DENIED_CREATORSHIP";
    private static final String ACCESS_DENIED_ACTOR_NOT_APPROVED = "ACCESS_DENIED_ACTOR_NOT_APPROVED";
    private static final String ASSET_BLOCKED_BY_REGULATOR = "ASSET_BLOCKED_BY_REGULATOR";

    // Abhängigkeiten, die per Konstruktor-Injektion bereitgestellt werden
    private final ActorService actorService;
    private final AssetService assetService;

    /**
     * Konstruktor für den AuthorizationService.
     * Abhängigkeiten (Services) werden hier injiziert.
     * @param actorService Service zur Verwaltung und Abfrage von Akteuren.
     * @param assetService Service zum Abrufen von Assets aus dem Ledger (z.B. für Tag-Prüfungen).
     */
    public AuthorizationService(final ActorService actorService, final AssetService assetService) {
        Objects.requireNonNull(actorService, "ActorService cannot be null");
        Objects.requireNonNull(assetService, "AssetService cannot be null");
        this.actorService = actorService;
        this.assetService = assetService;
    }

    /**
     * Prüft, ob der Aufrufer den Status 'APPROVED' im Ledger hat.
     * Diese Prüfung sollte für fast jede Transaktion nach der Registrierung erfolgen.
     * @param ctx Der Transaktionskontext.
     * @throws ChaincodeException wenn der Aufrufer nicht im Status 'APPROVED' ist.
     */
    public void requireActorApproved(final Context ctx) {
        String callerActorId = actorService.getCallerActorId(ctx);
        Actor callerActor = actorService.getActorById(ctx, callerActorId); // Annahme: getActorById wirft Exception, wenn nicht gefunden

        if (!callerActor.getStatus().equals(RoleConstants.APPROVED)) {
            String err = String.format("Zugriff verweigert. Akteur '%s' ist nicht '%s'. Aktueller Status: '%s'.",
                    callerActorId, RoleConstants.APPROVED, callerActor.getStatus());
            throw new ChaincodeException(err, ACCESS_DENIED_ACTOR_NOT_APPROVED);
        }
    }

    /**
     * Prüft, ob der Aufrufer eine der angegebenen Rollen besitzt.
     * Wirft eine ChaincodeException, wenn keine der Rollen zutrifft.
     * @param ctx Der Transaktionskontext.
     * @param requiredRoles Eine Liste von erlaubten Rollen (aus RoleConstants).
     * @throws ChaincodeException Wenn der Aufrufer keine zugewiesene Rolle hat oder keine der erforderlichen Rollen besitzt.
     */
    public void requireAnyOfRoles(final Context ctx, final String... requiredRoles) {
        String callerRole = ctx.getClientIdentity().getAttributeValue(ROLE_ATTRIBUTE);

        if (callerRole == null || callerRole.isEmpty()) {
            String err = "Zugriff verweigert. Der Aufrufer hat keine zugewiesene Rolle im Zertifikat.";
            throw new ChaincodeException(err, ACCESS_DENIED_NO_ROLE);
        }

        for (String requiredRole : requiredRoles) {
            if (requiredRole.equalsIgnoreCase(callerRole)) {
                return; // Berechtigung erteilt, die Methode sofort verlassen.
            }
        }

        // Wenn die Schleife durchläuft, ohne eine Übereinstimmung zu finden:
        String err = String.format("Zugriff verweigert. Der Aufrufer mit Rolle '%s' hat nicht eine der erforderlichen Rollen: %s",
                callerRole, String.join(", ", requiredRoles));
        throw new ChaincodeException(err, ACCESS_DENIED_ROLE);
    }

    /**
     * Prüft, ob der Aufrufer der Transaktion der eingetragene Ersteller eines Medikaments ist.
     * Dies wird für Aktionen verwendet, bei denen nur der ursprüngliche Hersteller (Product Designer) agieren darf.
     * @param ctx Der Transaktionskontext.
     * @param assetCreatorActorId Die Akteur-ID des Erstellers des Assets (Medikaments).
     * @param assetKey Der Schlüssel des betroffenen Assets (für bessere Fehlermeldungen).
     * @throws ChaincodeException Wenn der Aufrufer nicht der Ersteller ist.
     */
    public void requireCallerIsCreator(final Context ctx, final String assetCreatorActorId, final String assetKey) {
        String callerActorId = actorService.getCallerActorId(ctx);

        if (assetCreatorActorId == null || !assetCreatorActorId.equals(callerActorId)) {
            String err = String.format("Zugriff verweigert. Nur der Ersteller ('%s') darf diese Aktion für Asset '%s' ausführen. Aufrufer: '%s'.",
                    assetCreatorActorId, assetKey, callerActorId);
            throw new ChaincodeException(err, ACCESS_DENIED_CREATORSHIP);
        }
    }

    /**
     * Prüft, ob der Aufrufer der Transaktion der eingetragene aktuelle Besitzer des Assets (einer MedicationUnit) ist.
     * @param ctx Der Transaktionskontext.
     * @param assetCurrentOwnerActorId Die Akteur-ID des aktuellen Besitzers des Assets.
     * @param assetKey Der Schlüssel des betroffenen Assets (für bessere Fehlermeldungen).
     * @throws ChaincodeException Wenn der Aufrufer nicht der aktuelle Besitzer ist.
     */
    public void requireCallerIsCurrentOwner(final Context ctx, final String assetCurrentOwnerActorId, final String assetKey) {
        String callerActorId = actorService.getCallerActorId(ctx);

        if (assetCurrentOwnerActorId == null || !assetCurrentOwnerActorId.equals(callerActorId)) {
            String err = String.format("Zugriff verweigert. Nur der Besitzer ('%s') darf diese Aktion für Asset '%s' ausführen. Aufrufer: '%s'.",
                    assetCurrentOwnerActorId, assetKey, callerActorId);
            throw new ChaincodeException(err, ACCESS_DENIED_OWNERSHIP);
        }
    }

    /**
     * Prüft, ob ein Asset einen bestimmten Status hat.
     * @param assetKey Der Schlüssel des Assets.
     * @param actualStatus Der aktuelle Status des Assets.
     * @param expectedStatus Der erwartete Status.
     * @throws AssetStatusException wenn der Status nicht dem erwarteten entspricht
     */
    public void requireAssetStatus(final String assetKey, final String actualStatus, final String expectedStatus) {
        if (actualStatus == null || !actualStatus.equalsIgnoreCase(expectedStatus)) {
            throw AssetStatusException.createForStatus(assetKey, expectedStatus, actualStatus);
        }
    }

    /**
     * Prüft, ob ein Asset nicht im angegebenen Status ist.
     * @param assetKey Der Schlüssel des Assets.
     * @param actualStatus Der aktuelle Status des Assets.
     * @param forbiddenStatus Der verbotene Status.
     * @throws AssetStatusException wenn der Status dem verbotenen entspricht
     */
    public void requireAssetNotStatus(final String assetKey, final String actualStatus, final String forbiddenStatus) {
        if (actualStatus != null && actualStatus.equalsIgnoreCase(forbiddenStatus)) {
            String message = String.format("Asset %s darf nicht im Status '%s' sein für diese Aktion.",
                    assetKey, forbiddenStatus);
            throw new AssetStatusException(message);
        }
    }

    /**
     * Prüft, ob eine Charge oder ein Medikament durch einen regulatorischen Tag gesperrt ist.
     * @param ctx Der Transaktionskontext.
     * @param assetKey Der Schlüssel des Batch- oder Medication-Assets.
     * @throws AssetStatusException wenn ein Sperr-Tag gefunden wird.
     * @throws ChaincodeException wenn das Asset nicht existiert oder nicht vom erwarteten Typ ist.
     */
    public void requireAssetNotBlockedByRegulator(final Context ctx, final String assetKey) {
        // Diese Methode muss zwischen Medication und Batch unterscheiden, da sie unterschiedliche Tag-Listen haben
        // und die Logik unterschiedlich sein kann.

        if (assetKey.startsWith("MED_")) {
            Medication medication = assetService.getAssetByKey(ctx, assetKey, Medication.class);
            // Überprüfe den Status des Medikaments
            if (medication.getStatus().equalsIgnoreCase(RoleConstants.GESPERRT)) {
                throw new AssetStatusException(String.format("Medikament %s ist vom Regulator gesperrt (Status: %s).", assetKey, medication.getStatus()), ASSET_BLOCKED_BY_REGULATOR);
            }
            // Überprüfe Klassifizierungstags auf blockierende Wirkung
            for (Tag tag : medication.getClassificationTags()) { // Korrigiert: AssetTag zu Tag
                if (tag.isBlocking()) {
                    String message = String.format("Medikament '%s' ist durch Klassifizierungstag '%s' vom Akteur '%s' gesperrt.",
                            assetKey, tag.getTagName(), tag.getActorId());
                    throw new AssetStatusException(message, ASSET_BLOCKED_BY_REGULATOR);
                }
            }
        } else if (assetKey.startsWith("BATCH_")) {
            Batch batch = assetService.getAssetByKey(ctx, assetKey, Batch.class);
            // Überprüfe den Status der Charge
            if (batch.getCurrentBatchStatus().equalsIgnoreCase(RoleConstants.GESPERRT)) {
                throw new AssetStatusException(String.format("Charge %s ist vom Regulator gesperrt (Status: %s).", assetKey, batch.getCurrentBatchStatus()), ASSET_BLOCKED_BY_REGULATOR);
            }
            // Überprüfe regulatorische Tags auf blockierende Wirkung
            for (Tag tag : batch.getRegulatoryTags()) { // Korrigiert: AssetTag zu Tag
                if (tag.isBlocking()) {
                    String message = String.format("Charge '%s' ist durch regulatorischen Tag '%s' vom Akteur '%s' gesperrt.",
                            assetKey, tag.getTagName(), tag.getActorId());
                    throw new AssetStatusException(message, ASSET_BLOCKED_BY_REGULATOR);
                }
            }
        } else if (assetKey.startsWith("UNIT_")) {
            // Für Units: Prüfe die übergeordnete Batch auf Sperrung
            MedicationUnit unit = assetService.getAssetByKey(ctx, assetKey, MedicationUnit.class);
            // Rekursiver Aufruf für die Batch (um Tags der Batch zu prüfen)
            requireAssetNotBlockedByRegulator(ctx, unit.getBatchKey());
            // Prüfe auch den direkten Status der Unit selbst
            if (unit.getUnitStatus().equalsIgnoreCase(RoleConstants.GESPERRT)) {
                throw new AssetStatusException(String.format("Einheit %s ist vom Regulator gesperrt (Status: %s).", assetKey, unit.getUnitStatus()), ASSET_BLOCKED_BY_REGULATOR);
            }
        } else {
            throw new ChaincodeException("Unbekannter oder nicht unterstützter Asset-Typ für regulatorische Blockierungsprüfung: " + assetKey, "INVALID_ASSET_TYPE_FOR_BLOCK");
        }
    }
}

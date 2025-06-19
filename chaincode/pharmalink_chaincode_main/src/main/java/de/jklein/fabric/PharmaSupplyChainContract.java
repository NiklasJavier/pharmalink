package de.jklein.fabric;

import de.jklein.fabric.models.Actor;
import de.jklein.fabric.models.Medikament;
import de.jklein.fabric.models.Unit; // Importiere die neue Unit-Klasse
import de.jklein.fabric.utils.JsonUtil;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant; // Import für Zeitstempel
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Contract(
        name = "PharmaSupplyChainContract",
        info = @Info(
                title = "Pharma Supply Chain Chaincode",
                description = "Chaincode für die Verwaltung von Akteuren und Medikamenten in einer pharmazeutischen Lieferkette",
                version = "1.0.0-SNAPSHOT"))
@Default
public final class PharmaSupplyChainContract implements ContractInterface {

    private enum PharmaSupplyChainErrors {
        ACTOR_NOT_FOUND,
        ACTOR_ALREADY_EXISTS,
        INVALID_ARGUMENT,
        UNAUTHORIZED_ACCESS,
        MISSING_CERT_ATTRIBUTE,
        MEDIKAMENT_NOT_FOUND,
        MEDIKAMENT_ALREADY_EXISTS,
        INVALID_MEDIKAMENT_STATUS_CHANGE,
        UNIT_NOT_FOUND, // Hinzugefügt
        UNIT_ALREADY_EXISTS, // Hinzugefügt
        MEDIKAMENT_NOT_APPROVED, // Hinzugefügt
        INVALID_UNIT_OWNER // Hinzugefügt
    }

    // Präfix für den Unit-Zähler im Ledger
    private static final String UNIT_COUNTER_PREFIX = "unitCounter_";

    /**
     * Prüft, ob ein Akteur existiert.
     *
     * @param ctx     Der Transaktionskontext.
     * @param actorId Die ID des zu prüfenden Akteurs.
     * @return true, wenn der Akteur existiert, false sonst.
     */
    private boolean actorExists(final Context ctx, final String actorId) {
        byte[] actorState = ctx.getStub().getState(actorId);
        return actorState != null && actorState.length > 0;
    }

    /**
     * Hilfsmethode zur Überprüfung der Rolle des aufrufenden Akteurs.
     *
     * @param ctx Der Transaktionskontext.
     * @param requiredRole Die erforderliche Rolle.
     * @throws ChaincodeException Wenn der aufrufende Akteur nicht die erforderliche Rolle hat.
     */
    private void verifyCallingActorRole(final Context ctx, final String requiredRole) {
        String callingActorId = ctx.getClientIdentity().getId();
        // KORREKTUR: byte[] zu String Konvertierung
        byte[] actorStateBytes = ctx.getStub().getState(callingActorId);
        if (actorStateBytes == null || actorStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Akteur %s nicht gefunden", callingActorId), PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }
        Actor callingActor = JsonUtil.fromJson(new String(actorStateBytes, StandardCharsets.UTF_8), Actor.class);

        if (callingActor == null || !callingActor.getRole().equalsIgnoreCase(requiredRole)) {
            String errorMessage = String.format("Akteur %s ist nicht berechtigt. Erforderliche Rolle: %s. Aktuelle Rolle: %s",
                    callingActorId, requiredRole, callingActor != null ? callingActor.getRole() : "Nicht gefunden");
            throw new ChaincodeException(errorMessage, PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }
    }

    /**
     * Erstellt einen neuen Akteur im Ledger.
     *
     * @param ctx        Der Transaktionskontext.
     * @param actorId    Die ID des Akteurs.
     * @param role       Die Rolle des Akteurs (z.B. "hersteller", "grosshaendler", "apotheke", "behoerde").
     * @param email      Die E-Mail-Adresse des Akteurs.
     * @param ipfsLink   Ein optionaler IPFS-Link für weitere Informationen.
     * @return Der erstellte Akteur als JSON-String.
     * Example: {"function":"createActor","Args":["actor1","hersteller","test@example.com","Qm..."]}
     */
    @Transaction()
    public String createActor(final Context ctx, final String actorId, final String role, final String email, final String ipfsLink) {
        if (actorExists(ctx, actorId)) {
            throw new ChaincodeException(String.format("Akteur %s existiert bereits", actorId), PharmaSupplyChainErrors.ACTOR_ALREADY_EXISTS.toString());
        }

        // Beispiel für Rollenüberprüfung: Nur ein "behoerde"-Akteur darf neue Akteure erstellen
        verifyCallingActorRole(ctx, "behoerde");

        Actor actor = new Actor(actorId, role, email, ipfsLink);
        ctx.getStub().putState(actorId, JsonUtil.toJson(actor).getBytes(StandardCharsets.UTF_8));
        return JsonUtil.toJson(actor);
    }

    /**
     * Fragt einen Akteur anhand seiner ID ab.
     *
     * @param ctx     Der Transaktionskontext.
     * @param actorId Die ID des Akteurs.
     * @return Der Akteur als JSON-String.
     * Example: {"function":"queryActor","Args":["actor1"]}
     */
    @Transaction()
    public String queryActor(final Context ctx, final String actorId) {
        byte[] actorState = ctx.getStub().getState(actorId);

        if (actorState == null || actorState.length == 0) {
            throw new ChaincodeException(String.format("Akteur %s nicht gefunden", actorId), PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }

        return new String(actorState, StandardCharsets.UTF_8);
    }

    /**
     * Aktualisiert die Informationen eines vorhandenen Akteurs.
     *
     * @param ctx        Der Transaktionskontext.
     * @param actorId    Die ID des zu aktualisierenden Akteurs.
     * @param newRole    Die neue Rolle des Akteurs.
     * @param newEmail   Die neue E-Mail-Adresse des Akteurs.
     * @param newIpfsLink Der neue optionale IPFS-Link.
     * @return Der aktualisierte Akteur als JSON-String.
     * Example: {"function":"updateActor","Args":["actor1","hersteller","new_email@example.com","newQm..."]}
     */
    @Transaction()
    public String updateActor(final Context ctx, final String actorId, final String newRole, final String newEmail, final String newIpfsLink) {
        byte[] actorStateBytes = ctx.getStub().getState(actorId); // KORREKTUR: getState liefert byte[]

        if (actorStateBytes == null || actorStateBytes.length == 0) { // Prüfen auf null oder leer
            throw new ChaincodeException(String.format("Akteur %s nicht gefunden", actorId), PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }

        // KORREKTUR: byte[] zu String Konvertierung
        Actor existingActor = JsonUtil.fromJson(new String(actorStateBytes, StandardCharsets.UTF_8), Actor.class);

        // Nur der Akteur selbst oder eine Behörde darf seine Informationen aktualisieren
        String callingActorId = ctx.getClientIdentity().getId();
        // KORREKTUR: byte[] zu String Konvertierung
        byte[] callingActorStateBytes = ctx.getStub().getState(callingActorId);
        if (callingActorStateBytes == null || callingActorStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Aufrufender Akteur %s nicht gefunden.", callingActorId), PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }
        Actor callingActor = JsonUtil.fromJson(new String(callingActorStateBytes, StandardCharsets.UTF_8), Actor.class);
        if (!Objects.equals(callingActorId, actorId) && !callingActor.getRole().equalsIgnoreCase("behoerde")) {
            throw new ChaincodeException("Nicht autorisiert, diesen Akteur zu aktualisieren.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        existingActor.setRole(newRole);
        existingActor.setEmail(newEmail);
        existingActor.setIpfsLink(newIpfsLink);

        ctx.getStub().putState(actorId, JsonUtil.toJson(existingActor).getBytes(StandardCharsets.UTF_8));
        return JsonUtil.toJson(existingActor);
    }

    /**
     * Löscht einen Akteur aus dem Ledger.
     *
     * @param ctx     Der Transaktionskontext.
     * @param actorId Die ID des zu löschenden Akteurs.
     * Example: {"function":"deleteActor","Args":["actor1"]}
     */
    @Transaction()
    public void deleteActor(final Context ctx, final String actorId) {
        if (!actorExists(ctx, actorId)) {
            throw new ChaincodeException(String.format("Akteur %s nicht gefunden", actorId), PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }

        // Nur ein "behoerde"-Akteur darf Akteure löschen
        verifyCallingActorRole(ctx, "behoerde");

        ctx.getStub().delState(actorId);
    }

    /**
     * Fragt alle Akteure im Ledger ab.
     *
     * @param ctx Der Transaktionskontext.
     * @return Eine JSON-Zeichenkette aller Akteure.
     * Example: {"function":"queryAllActors","Args":[]}
     */
    @Transaction()
    public String queryAllActors(final Context ctx) {
        List<Actor> actorList = new ArrayList<>();
        // Hier wird ein Rich Query verwendet, um alle Akteure zu finden (docType = "actor")
        String queryString = "{\"selector\":{\"docType\":\"actor\"}}";
        final QueryResultsIterator<org.hyperledger.fabric.shim.ledger.KeyValue> resultsIterator = ctx.getStub().getQueryResult(queryString);

        for (final org.hyperledger.fabric.shim.ledger.KeyValue kv : resultsIterator) {
            // kv.getStringValue() liefert bereits einen String
            final Actor actor = JsonUtil.fromJson(kv.getStringValue(), Actor.class);
            actorList.add(actor);
        }

        return JsonUtil.toJson(actorList);
    }

    // ====================================================================================================
    // Hinzugefügt: Akteur-Funktionen (aus ursprünglichem Code übernommen, aber korrigiert)
    // ====================================================================================================

    /**
     * Registriert einen neuen Akteur im Ledger oder gibt dessen Informationen zurück, wenn er bereits existiert.
     * Die Actor ID wird auf Basis des SHA-256 Hashs der Client-Identität (resultierend aus MSPID und der eindeutigen
     * ID des Zertifikats des aufrufenden Akteurs, wie von Fabric zurückgegeben) und der aus dem Zertifikat bezogenen Rolle gebildet:
     * Rolle-SHA256(MSPID-ClientIdentityID). Dies gewährleistet eine konsistente ID-Generierung über verschiedene Peers hinweg,
     * solange die zugrunde liegende Client-Identität (Zertifikat und MSPID) gleich bleibt.
     *
     * Die Rolle des Akteurs wird direkt aus dem 'role'-Attribut des X.509-Zertifikats des Aufrufers gelesen.
     *
     * @param ctx Der Smart Contract Kontext.
     * @param email E-Mail des Akteurs.
     * @param ipfsLink Optionaler IPFS Link für weitere Attribute des Akteurs. Kann leer sein.
     * @return Die Informationen des registrierten oder bereits existierenden Akteurs als JSON-String.
     * @throws ChaincodeException Wenn ein Hash-Fehler auftritt, das 'role'-Attribut im Zertifikat fehlt
     * oder die aus dem Zertifikat gelesene Rolle keine gültige Affiliation darstellt.
     *
     * Beispiel für Aufruf:
     * {"function":"initCall","Args":["max.mustermann@example.com","QmWgX..."]}
     * {"function":"initCall","Args":["erika.musterfrau@example.com",""]}
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String initCall(final Context ctx, final String email, final String ipfsLink) {
        final ChaincodeStub stub = ctx.getStub();
        final String mspId = ctx.getClientIdentity().getMSPID();
        final String clientId = ctx.getClientIdentity().getId();

        final String certRoleRaw = ctx.getClientIdentity().getAttributeValue("role");

        if (certRoleRaw == null || certRoleRaw.isEmpty()) {
            final String errorMessage = "Client-Zertifikat enthält kein 'role'-Attribut. Registrierung nicht möglich.";
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, PharmaSupplyChainErrors.MISSING_CERT_ATTRIBUTE.toString());
        }

        String actualRoleFromCert = "";
        if (certRoleRaw.contains(":")) {
            actualRoleFromCert = certRoleRaw.substring(0, certRoleRaw.indexOf(':'));
        } else {
            actualRoleFromCert = certRoleRaw;
        }

        boolean isValidRoleAffiliation = false;
        final String[] allowedRoles = {"hersteller", "grosshaendler", "apotheke", "behoerde"};
        for (final String allowedRole : allowedRoles) {
            if (allowedRole.equalsIgnoreCase(actualRoleFromCert)) {
                isValidRoleAffiliation = true;
                break;
            }
        }

        if (!isValidRoleAffiliation) {
            final String errorMessage = String.format("Die aus dem Zertifikat gelesene Rolle '%s' ist keine gültige Lieferketten-Affiliation. Erlaubte Rollen sind: %s", actualRoleFromCert, String.join(", ", allowedRoles));
            throw new ChaincodeException(errorMessage, PharmaSupplyChainErrors.INVALID_ARGUMENT.toString());
        }


        final String combinedIdForHash = mspId + "-" + clientId;
        final String actorSha = generateSha256(combinedIdForHash);
        final String actorId = actualRoleFromCert.toLowerCase() + "-" + actorSha;

        // KORREKTUR: getStringState ist ok, da es einen String liefert
        final String actorState = stub.getStringState(actorId);

        if (!actorState.isEmpty()) {
            System.out.println("Akteur mit ID " + actorId + " ist bereits registriert. Rückgabe der Informationen.");
            return actorState;
        }

        final Actor newActor = new Actor(actorId, actualRoleFromCert.toLowerCase(), email, ipfsLink);
        final String newActorJson = JsonUtil.toJson(newActor);
        stub.putStringState(actorId, newActorJson);

        System.out.println("Neuer Akteur registriert: " + newActorJson);
        return newActorJson;
    }

    /**
     * Fragt die Informationen eines spezifischen Akteurs anhand seiner Actor ID ab.
     *
     * @param ctx Der Smart Contract Kontext.
     * @param actorId Die eindeutige ID des Akteurs.
     * @return Die Akteur-Informationen als JSON-String.
     * @throws ChaincodeException Wenn der Akteur nicht gefunden wird.
     *
     * Beispiel für Aufruf:
     * {"function":"queryActorById","Args":["hersteller-a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2"]}
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryActorById(final Context ctx, final String actorId) {
        final ChaincodeStub stub = ctx.getStub();
        // KORREKTUR: getStringState ist ok, da es einen String liefert
        final String actorState = stub.getStringState(actorId);

        if (actorState.isEmpty()) {
            final String errorMessage = String.format("Akteur mit ID %s nicht gefunden", actorId);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }
        return actorState;
    }

    /**
     * Fragt einen Akteur anhand seiner E-Mail-Adresse ab.
     * OPTIMIERUNG FÜR COUCHDB: Nutzt einen 'email'-Selektor und erfordert einen entsprechenden Index.
     *
     * @param ctx Der Smart Contract Kontext.
     * @param email Die E-Mail-Adresse des gesuchten Akteurs.
     * @return Die Informationen des gefundenen Akteurs als JSON-String.
     * @throws ChaincodeException Wenn kein Akteur mit der angegebenen E-Mail-Adresse gefunden wird.
     *
     * Beispiel für Aufruf:
     * {"function":"queryActorByEmail","Args":["max.mustermann@example.com"]}
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryActorByEmail(final Context ctx, final String email) {
        final ChaincodeStub stub = ctx.getStub();
        final List<Actor> actorList = new ArrayList<>();

        final String queryString = String.format("{\"selector\":{\"email\":\"%s\", \"docType\":\"actor\"}}", email);
        final QueryResultsIterator<org.hyperledger.fabric.shim.ledger.KeyValue> resultsIterator = stub.getQueryResult(queryString);

        for (final org.hyperledger.fabric.shim.ledger.KeyValue kv : resultsIterator) {
            // kv.getStringValue() liefert bereits einen String
            final Actor actor = JsonUtil.fromJson(kv.getStringValue(), Actor.class);
            actorList.add(actor);
        }

        if (actorList.isEmpty()) {
            final String errorMessage = String.format("Akteur mit E-Mail '%s' nicht gefunden.", email);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }
        return JsonUtil.toJson(actorList.get(0));
    }


    /**
     * Fragt alle Akteure mit einer spezifischen Rolle ab.
     * OPTIMIERUNG FÜR COUCHDB: Nutzt einen 'role'-Selektor und erfordert einen entsprechenden Index.
     *
     * @param ctx Der Smart Contract Kontext.
     * @param role Die Rolle, nach der gefiltert werden soll (z.B. "hersteller").
     * @return Eine Liste von Akteuren mit der angegebenen Rolle als JSON-Array von Actor-Objekten.
     *
     * Beispiel für Aufruf:
     * {"function":"queryActorsByRole","Args":["hersteller"]}
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryActorsByRole(final Context ctx, final String role) {
        final ChaincodeStub stub = ctx.getStub();
        final List<Actor> actorList = new ArrayList<>();

        final String queryString = String.format("{\"selector\":{\"role\":\"%s\", \"docType\":\"actor\"}}", role.toLowerCase());
        final QueryResultsIterator<org.hyperledger.fabric.shim.ledger.KeyValue> resultsIterator = stub.getQueryResult(queryString);

        for (final org.hyperledger.fabric.shim.ledger.KeyValue kv : resultsIterator) {
            final Actor actor = JsonUtil.fromJson(kv.getStringValue(), Actor.class);
            actorList.add(actor);
        }

        return JsonUtil.toJson(actorList);
    }

    /**
     * Aktualisiert die IPFS-Link-Informationen für einen bestehenden Akteur.
     * Es wird überprüft, ob der aufrufende Akteur der Eigentümer des Profils ist, das aktualisiert werden soll.
     *
     * @param ctx Der Smart Contract Kontext.
     * @param actorId Die ID des Akteurs, dessen IPFS-Link aktualisiert werden soll.
     * @param newIpfsLink Der neue IPFS-Link.
     * @return Der aktualisierte Akteur als JSON-String.
     * @throws ChaincodeException Wenn der Akteur nicht gefunden wird oder der Aufrufer nicht autorisiert ist.
     *
     * Beispiel für Aufruf:
     * {"function":"updateActorIpfsLink","Args":["hersteller-a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2","QmUpdatedIpfsLink..."]}
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String updateActorIpfsLink(final Context ctx, final String actorId, final String newIpfsLink) {
        final ChaincodeStub stub = ctx.getStub();
        final String mspId = ctx.getClientIdentity().getMSPID();
        final String clientId = ctx.getClientIdentity().getId();

        byte[] actorStateBytes = stub.getState(actorId); // getState liefert byte[]
        if (actorStateBytes == null || actorStateBytes.length == 0) { // Prüfen auf null oder leer
            final String errorMessage = String.format("Akteur mit ID %s nicht gefunden", actorId);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }

        // KORREKTUR: byte[] zu String Konvertierung
        final Actor existingActor = JsonUtil.fromJson(new String(actorStateBytes, StandardCharsets.UTF_8), Actor.class);

        // Überprüfen der Berechtigung: Nur der Akteur selbst darf sein Profil aktualisieren
        final String expectedActorShaSuffix = generateSha256(mspId + "-" + clientId);
        final String expectedActorId = existingActor.getRole().toLowerCase() + "-" + expectedActorShaSuffix;

        if (!actorId.equals(expectedActorId)) {
            final String errorMessage = "Nicht autorisiert: Nur der Eigentümer des Profils darf es aktualisieren.";
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        existingActor.setIpfsLink(newIpfsLink);

        final String updatedActorJson = JsonUtil.toJson(existingActor);
        stub.putStringState(actorId, updatedActorJson);

        System.out.println("Akteur IPFS Link aktualisiert: " + updatedActorJson);
        return updatedActorJson;
    }

    // ====================================================================================================
    // MEDIKAMENT-FUNKTIONEN (aus ursprünglichem Code übernommen, aber korrigiert)
    // ====================================================================================================

    /**
     * Erstellt ein neues Medikament im Ledger. Nur Hersteller dürfen Medikamente anlegen.
     *
     * @param ctx Der Smart Contract Kontext.
     * @param bezeichnung Die Bezeichnung des Medikaments.
     * @param infoblattHash Der Hash des Infoblatts (On-Chain-Referenz).
     * @param ipfsLink Der IPFS Link zu weiteren Off-Chain-Informationen des Infoblatts.
     * @return Das erstellte Medikament als JSON-String.
     * @throws ChaincodeException Wenn der Aufrufer kein Hersteller ist, das Medikament bereits existiert
     * oder die Hersteller-ID nicht gefunden wird.
     *
     * Beispiel für Aufruf:
     * {"function":"createMedikament","Args":["Paracetamol 500mg","a1b2c3d4e5f6...","QmHashdesInfoblatts"]}
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String createMedikament(final Context ctx, final String bezeichnung, final String infoblattHash, final String ipfsLink) {
        final ChaincodeStub stub = ctx.getStub();
        final String mspId = ctx.getClientIdentity().getMSPID();
        final String clientId = ctx.getClientIdentity().getId();

        // 1. Hersteller-ID des Aufrufers ermitteln und Rolle prüfen
        final String certRoleRaw = ctx.getClientIdentity().getAttributeValue("role");
        if (certRoleRaw == null || certRoleRaw.isEmpty()) {
            throw new ChaincodeException("Client-Zertifikat enthält kein 'role'-Attribut.", PharmaSupplyChainErrors.MISSING_CERT_ATTRIBUTE.toString());
        }
        final String actualRoleFromCert = certRoleRaw.contains(":") ? certRoleRaw.substring(0, certRoleRaw.indexOf(':')) : certRoleRaw;

        if (!"hersteller".equalsIgnoreCase(actualRoleFromCert)) {
            throw new ChaincodeException("Nicht autorisiert: Nur Hersteller dürfen Medikamente anlegen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        final String herstellerId = actualRoleFromCert.toLowerCase() + "-" + generateSha256(mspId + "-" + clientId);
        byte[] herstellerActorStateBytes = stub.getState(herstellerId); // getState liefert byte[]
        if (herstellerActorStateBytes == null || herstellerActorStateBytes.length == 0) { // Prüfen auf null oder leer
            throw new ChaincodeException("Die Hersteller-ID des Aufrufers ist nicht im System registriert.", PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }

        // 2. Medikamenten-ID generieren: MED-SHA256(HerstellerID-Bezeichnung)
        final String combinedMedIdHashInput = herstellerId + "-" + bezeichnung;
        final String medSha = generateSha256(combinedMedIdHashInput);
        final String medId = "MED-" + medSha;

        byte[] medikamentStateBytes = stub.getState(medId); // getState liefert byte[]
        if (medikamentStateBytes != null && medikamentStateBytes.length > 0) { // Prüfen auf existierend
            throw new ChaincodeException(String.format("Medikament mit ID '%s' existiert bereits.", medId), PharmaSupplyChainErrors.MEDIKAMENT_ALREADY_EXISTS.toString());
        }

        // 3. Medikament-Objekt erstellen und im Ledger speichern
        final Medikament newMedikament = new Medikament(medId, herstellerId, bezeichnung, ipfsLink);
        newMedikament.setInfoblattHash(infoblattHash);

        final String newMedikamentJson = JsonUtil.toJson(newMedikament);
        stub.putStringState(medId, newMedikamentJson);

        // Hinzugefügt: Initialisiere den Unit-Zähler für dieses Medikament im Ledger
        stub.putStringState(UNIT_COUNTER_PREFIX + medId, "0");

        System.out.println("Neues Medikament angelegt: " + newMedikamentJson);
        return newMedikamentJson;
    }

    /**
     * Genehmigt oder lehnt ein Medikament ab. Nur Akteure mit der Rolle "behoerde" dürfen dies tun.
     * Referenziert den Genehmiger und das Genehmigungsdatum im Medikament.
     *
     * @param ctx Der Smart Contract Kontext.
     * @param medId Die ID des zu genehmigenden/ablehnenden Medikaments.
     * @param newStatus Der neue Status (z.B. "freigegeben" oder "abgelehnt").
     * @return Das aktualisierte Medikament als JSON-String.
     * @throws ChaincodeException Wenn der Aufrufer keine Behörde ist, das Medikament nicht gefunden wird
     * oder der Status ungültig ist.
     *
     * Beispiel für Aufruf (von einer Behörde):
     * {"function":"approveMedikament","Args":["MED-a1b2c3d4e5f6...","freigegeben"]}
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String approveMedikament(final Context ctx, final String medId, final String newStatus) {
        final ChaincodeStub stub = ctx.getStub();
        final String mspId = ctx.getClientIdentity().getMSPID();
        final String clientId = ctx.getClientIdentity().getId();

        // 1. Rolle des Aufrufers überprüfen und ActorId ermitteln
        final String certRoleRaw = ctx.getClientIdentity().getAttributeValue("role");
        if (certRoleRaw == null || certRoleRaw.isEmpty()) {
            throw new ChaincodeException("Client-Zertifikat enthält kein 'role'-Attribut.", PharmaSupplyChainErrors.MISSING_CERT_ATTRIBUTE.toString());
        }
        final String actualRoleFromCert = certRoleRaw.contains(":") ? certRoleRaw.substring(0, certRoleRaw.indexOf(':')) : certRoleRaw;

        if (!"behoerde".equalsIgnoreCase(actualRoleFromCert)) {
            throw new ChaincodeException("Nicht autorisiert: Nur Behörden dürfen Medikamente genehmigen/ablehnen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }
        final String approverActorId = actualRoleFromCert.toLowerCase() + "-" + generateSha256(mspId + "-" + clientId);


        // 2. Medikament laden
        byte[] medikamentStateBytes = stub.getState(medId); // getState liefert byte[]
        if (medikamentStateBytes == null || medikamentStateBytes.length == 0) { // Prüfen auf null oder leer
            throw new ChaincodeException(String.format("Medikament mit ID '%s' nicht gefunden.", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }
        // KORREKTUR: byte[] zu String Konvertierung
        final Medikament existingMedikament = JsonUtil.fromJson(new String(medikamentStateBytes, StandardCharsets.UTF_8), Medikament.class);

        // 3. Status validieren und setzen
        final String lowerCaseNewStatus = newStatus.toLowerCase();
        if (!("freigegeben".equals(lowerCaseNewStatus) || "abgelehnt".equals(lowerCaseNewStatus))) {
            throw new ChaincodeException("Ungültiger Status: Der Status muss 'freigegeben' oder 'abgelehnt' sein.", PharmaSupplyChainErrors.INVALID_MEDIKAMENT_STATUS_CHANGE.toString());
        }

        existingMedikament.setStatus(lowerCaseNewStatus);
        existingMedikament.setApprovedById(approverActorId); // Genehmiger referenzieren
        // 4. Medikament im Ledger speichern
        final String updatedMedikamentJson = JsonUtil.toJson(existingMedikament);
        stub.putStringState(medId, updatedMedikamentJson);

        System.out.println("Medikamentstatus aktualisiert: " + updatedMedikamentJson);
        return updatedMedikamentJson;
    }

    /**
     * Aktualisiert grundlegende Informationen eines Medikaments. Nur der Hersteller, der das Medikament angelegt hat, darf dies tun.
     *
     * @param ctx Der Smart Contract Kontext.
     * @param medId Die ID des zu aktualisierenden Medikaments.
     * @param newBezeichnung Die neue Bezeichnung (kann leer sein, wenn keine Änderung).
     * @param newInfoblattHash Der neue Infoblatt-Hash (kann leer sein).
     * @param newIpfsLink Der neue IPFS Link (kann leer sein).
     * @return Das aktualisierte Medikament als JSON-String.
     * @throws ChaincodeException Wenn der Aufrufer nicht der Ersteller des Medikaments ist,
     * das Medikament nicht gefunden wird, oder andere Fehler auftreten.
     *
     * Beispiel für Aufruf (vom Hersteller):
     * {"function":"updateMedikament","Args":["MED-a1b2c3d4e5f6...","Paracetamol Forte","neuerhash","neueripfslink"]}
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String updateMedikament(final Context ctx, final String medId, final String newBezeichnung, final String newInfoblattHash, final String newIpfsLink) {
        final ChaincodeStub stub = ctx.getStub();
        final String mspId = ctx.getClientIdentity().getMSPID();
        final String clientId = ctx.getClientIdentity().getId();

        // 1. Rolle des Aufrufers und dessen ActorId ermitteln
        final String certRoleRaw = ctx.getClientIdentity().getAttributeValue("role");
        if (certRoleRaw == null || certRoleRaw.isEmpty()) {
            throw new ChaincodeException("Client-Zertifikat enthält kein 'role'-Attribut.", PharmaSupplyChainErrors.MISSING_CERT_ATTRIBUTE.toString());
        }
        final String actualRoleFromCert = certRoleRaw.contains(":") ? certRoleRaw.substring(0, certRoleRaw.indexOf(':')) : certRoleRaw;

        final String invokerActorId = actualRoleFromCert.toLowerCase() + "-" + generateSha256(mspId + "-" + clientId);

        // 2. Medikament laden
        byte[] medikamentStateBytes = stub.getState(medId); // getState liefert byte[]
        if (medikamentStateBytes == null || medikamentStateBytes.length == 0) { // Prüfen auf null oder leer
            throw new ChaincodeException(String.format("Medikament mit ID '%s' nicht gefunden.", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }
        // KORREKTUR: byte[] zu String Konvertierung
        final Medikament existingMedikament = JsonUtil.fromJson(new String(medikamentStateBytes, StandardCharsets.UTF_8), Medikament.class);

        // 3. Berechtigungsprüfung: Nur der ursprüngliche Hersteller darf bearbeiten
        if (!existingMedikament.getHerstellerId().equals(invokerActorId)) {
            throw new ChaincodeException("Nicht autorisiert: Nur der anlegende Hersteller darf dieses Medikament bearbeiten.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        // 4. Felder aktualisieren (nur wenn neue Werte ungleich leer sind)
        if (newBezeichnung != null && !newBezeichnung.isEmpty()) {
            existingMedikament.setBezeichnung(newBezeichnung);
        }
        if (newInfoblattHash != null && !newInfoblattHash.isEmpty()) {
            existingMedikament.setInfoblattHash(newInfoblattHash);
        }
        if (newIpfsLink != null && !newIpfsLink.isEmpty()) {
            existingMedikament.setIpfsLink(newIpfsLink);
        }

        // 5. Medikament im Ledger speichern
        final String updatedMedikamentJson = JsonUtil.toJson(existingMedikament);
        stub.putStringState(medId, updatedMedikamentJson);

        System.out.println("Medikament aktualisiert: " + updatedMedikamentJson);
        return updatedMedikamentJson;
    }


    /**
     * Fügt einem Medikament einen Tag hinzu oder aktualisiert diesen.
     * Nur der Hersteller (Ersteller) oder eine Behörde (falls die Rolle im Zertifikat stimmt) dürfen Tags setzen.
     *
     * @param ctx Der Smart Contract Kontext.
     * @param medId Die ID des Medikaments.
     * @param tagValue Der Wert des Tags, der gesetzt werden soll.
     * @return Das aktualisierte Medikament als JSON-String.
     * @throws ChaincodeException Wenn das Medikament nicht gefunden wird, der Aufrufer nicht autorisiert ist,
     * oder andere Fehler auftreten.
     *
     * Beispiel für Aufruf (vom Hersteller):
     * {"function":"addMedikamentTag","Args":["MED-a1b2c3d4e5f6...","Produktion Charge X erfolgreich abgeschlossen"]}
     * Beispiel für Aufruf (von Behörde):
     * {"function":"addMedikamentTag","Args":["MED-a1b2c3d4e5f6...","Zulassung 2024-06-19 erteilt"]}
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String addMedikamentTag(final Context ctx, final String medId, final String tagValue) {
        final ChaincodeStub stub = ctx.getStub();
        final String mspId = ctx.getClientIdentity().getMSPID();
        final String clientId = ctx.getClientIdentity().getId();

        // 1. Rolle des Aufrufers und dessen ActorId ermitteln
        final String certRoleRaw = ctx.getClientIdentity().getAttributeValue("role");
        if (certRoleRaw == null || certRoleRaw.isEmpty()) {
            throw new ChaincodeException("Client-Zertifikat enthält kein 'role'-Attribut.", PharmaSupplyChainErrors.MISSING_CERT_ATTRIBUTE.toString());
        }
        final String actualRoleFromCert = certRoleRaw.contains(":") ? certRoleRaw.substring(0, certRoleRaw.indexOf(':')) : certRoleRaw;

        final String invokerActorId = actualRoleFromCert.toLowerCase() + "-" + generateSha256(mspId + "-" + clientId);

        // 2. Medikament laden
        byte[] medikamentStateBytes = stub.getState(medId); // getState liefert byte[]
        if (medikamentStateBytes == null || medikamentStateBytes.length == 0) { // Prüfen auf null oder leer
            throw new ChaincodeException(String.format("Medikament mit ID '%s' nicht gefunden.", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }
        // KORREKTUR: byte[] zu String Konvertierung
        final Medikament existingMedikament = JsonUtil.fromJson(new String(medikamentStateBytes, StandardCharsets.UTF_8), Medikament.class);

        // 3. Berechtigungsprüfung und Tag setzen
        final Map<String, String> currentTags = existingMedikament.getTags();
        if ("hersteller".equalsIgnoreCase(actualRoleFromCert)) {
            // Nur der Ersteller-Hersteller darf Tags als "hersteller" setzen
            if (!existingMedikament.getHerstellerId().equals(invokerActorId)) {
                throw new ChaincodeException("Nicht autorisiert: Nur der anlegende Hersteller darf Tags für dieses Medikament setzen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
            }
            currentTags.put("hersteller", tagValue); // Key "hersteller" für Hersteller-Tags
        } else if ("behoerde".equalsIgnoreCase(actualRoleFromCert)) {
            currentTags.put("behoerde", tagValue); // Key "behoerde" für Behörden-Tags
        } else {
            throw new ChaincodeException("Nicht autorisiert: Nur Hersteller oder Behörden dürfen Tags setzen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        existingMedikament.setTags(currentTags);
        // 4. Medikament im Ledger speichern
        final String updatedMedikamentJson = JsonUtil.toJson(existingMedikament);
        stub.putStringState(medId, updatedMedikamentJson);

        System.out.println("Medikament-Tag aktualisiert: " + updatedMedikamentJson);
        return updatedMedikamentJson;
    }


    /**
     * Fragt ein spezifisches Medikament anhand seiner ID ab.
     *
     * @param ctx Der Smart Contract Kontext.
     * @param medId Die eindeutige ID des Medikaments (z.B. "MED-a1b2c3d4e5f6...").
     * @return Die Medikamenten-Informationen als JSON-String.
     * @throws ChaincodeException Wenn das Medikament nicht gefunden wird.
     *
     * Beispiel für Aufruf:
     * {"function":"queryMedikamentById","Args":["MED-a1b2c3d4e5f6..."]}
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryMedikamentById(final Context ctx, final String medId) {
        final ChaincodeStub stub = ctx.getStub();
        byte[] medikamentStateBytes = stub.getState(medId); // getState liefert byte[]

        if (medikamentStateBytes == null || medikamentStateBytes.length == 0) { // Prüfen auf null oder leer
            final String errorMessage = String.format("Medikament mit ID '%s' nicht gefunden.", medId);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }
        return new String(medikamentStateBytes, StandardCharsets.UTF_8);
    }

    /**
     * Fragt alle Medikamente ab, die von einem bestimmten Hersteller angelegt wurden.
     * OPTIMIERUNG FÜR COUCHDB: Nutzt einen 'herstellerId'-Selektor und erfordert einen entsprechenden Index.
     *
     * @param ctx Der Smart Contract Kontext.
     * @param herstellerId Die Actor ID des Herstellers.
     * @return Eine Liste von Medikamenten als JSON-Array von Medikament-Objekten.
     *
     * Beispiel für Aufruf:
     * {"function":"queryMedikamenteByHerstellerId","Args":["hersteller-a1b2c3d4e5f6..."]}
     */
    // KORREKTUR: Typ auf EVALUATE gesetzt
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryMedikamenteByHerstellerId(final Context ctx, final String herstellerId) {
        final ChaincodeStub stub = ctx.getStub();
        final List<Medikament> medikamentList = new ArrayList<>();

        final String queryString = String.format("{\"selector\":{\"herstellerId\":\"%s\", \"docType\":\"medikament\"}}", herstellerId);
        final QueryResultsIterator<org.hyperledger.fabric.shim.ledger.KeyValue> resultsIterator = stub.getQueryResult(queryString);

        for (final org.hyperledger.fabric.shim.ledger.KeyValue kv : resultsIterator) {
            final Medikament medikament = JsonUtil.fromJson(kv.getStringValue(), Medikament.class);
            medikamentList.add(medikament);
        }

        return JsonUtil.toJson(medikamentList);
    }


    /**
     * Löscht ein Medikament. Nur Behörden oder der anlegende Hersteller (wenn Status "angelegt") dürfen Medikamente löschen.
     *
     * @param ctx    Der Transaktionskontext.
     * @param medId Die ID des zu löschenden Medikaments.
     * Example: {"function":"deleteMedikament","Args":["MED-HASH123"]}
     */
    @Transaction()
    public void deleteMedikament(final Context ctx, final String medId) {
        byte[] medikamentStateBytes = ctx.getStub().getState(medId); // getState liefert byte[]

        if (medikamentStateBytes == null || medikamentStateBytes.length == 0) { // Prüfen auf null oder leer
            throw new ChaincodeException(String.format("Medikament %s nicht gefunden", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }

        // KORREKTUR: byte[] zu String Konvertierung
        Medikament existingMedikament = JsonUtil.fromJson(new String(medikamentStateBytes, StandardCharsets.UTF_8), Medikament.class);
        String callingActorId = ctx.getClientIdentity().getId();
        // KORREKTUR: byte[] zu String Konvertierung
        byte[] callingActorStateBytes = ctx.getStub().getState(callingActorId);
        if (callingActorStateBytes == null || callingActorStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Aufrufender Akteur %s nicht gefunden.", callingActorId), PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }
        Actor callingActor = JsonUtil.fromJson(new String(callingActorStateBytes, StandardCharsets.UTF_8), Actor.class);

        // Nur Behörde oder der anlegende Hersteller, wenn der Status "angelegt" ist, darf löschen
        if (!(callingActor.getRole().equalsIgnoreCase("behoerde")
                ||
                (callingActor.getRole().equalsIgnoreCase("hersteller") && Objects.equals(callingActorId, existingMedikament.getHerstellerId()) && existingMedikament.getStatus().equalsIgnoreCase("angelegt")))) {
            throw new ChaincodeException("Nicht autorisiert, dieses Medikament zu löschen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        ctx.getStub().delState(medId);
        // Hinzugefügt: Auch den Unit-Zähler löschen, wenn das Medikament gelöscht wird
        ctx.getStub().delState(UNIT_COUNTER_PREFIX + medId);
    }


    // ====================================================================================================
    // Hinzugefügt: UNIT-FUNKTIONEN
    // ====================================================================================================

    /**
     * Erstellt eine angegebene Anzahl von Einheiten für ein genehmigtes Medikament.
     * Nur der Hersteller des Medikaments darf Einheiten erstellen.
     *
     * @param ctx             Der Transaktionskontext.
     * @param medId           Die ID des Medikaments.
     * @param chargeBezeichnung Die Bezeichnung der Charge.
     * @param anzahl          Die Anzahl der zu erstellenden Einheiten.
     * @param ipfsLink        Ein optionaler IPFS-Link für die Einheiten.
     * @return Eine JSON-Zeichenkette der erstellten Einheiten.
     * Example: {"function":"createUnits","Args":["MED-HASH123","Charge-XYZ","10","Qm..."]}
     */
    @Transaction()
    public String createUnits(final Context ctx, final String medId, final String chargeBezeichnung,
                              final int anzahl, final String ipfsLink) {

        byte[] medikamentStateBytes = ctx.getStub().getState(medId); // getState liefert byte[]
        if (medikamentStateBytes == null || medikamentStateBytes.length == 0) { // Prüfen auf null oder leer
            throw new ChaincodeException(String.format("Medikament %s nicht gefunden", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }

        // KORREKTUR: byte[] zu String Konvertierung
        Medikament existingMedikament = JsonUtil.fromJson(new String(medikamentStateBytes, StandardCharsets.UTF_8), Medikament.class);

        // Überprüfen, ob der aufrufende Akteur der Hersteller des Medikaments ist
        String callingActorId = ctx.getClientIdentity().getId();
        // KORREKTUR: byte[] zu String Konvertierung
        byte[] callingActorStateBytes = ctx.getStub().getState(callingActorId);
        if (callingActorStateBytes == null || callingActorStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Aufrufender Akteur %s nicht gefunden.", callingActorId), PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }
        Actor callingActor = JsonUtil.fromJson(new String(callingActorStateBytes, StandardCharsets.UTF_8), Actor.class);

        if (callingActor == null || !Objects.equals(callingActorId, existingMedikament.getHerstellerId())) {
            throw new ChaincodeException("Nur der Hersteller des Medikaments darf Einheiten erstellen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        // Überprüfen, ob das Medikament freigegeben ist
        if (!existingMedikament.getStatus().equalsIgnoreCase("freigegeben")) {
            throw new ChaincodeException(String.format("Medikament %s ist nicht freigegeben und kann daher nicht in Einheiten unterteilt werden.", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_APPROVED.toString());
        }

        // Aktuellen Zähler für dieses Medikament aus dem Ledger abrufen
        byte[] counterBytes = ctx.getStub().getState(UNIT_COUNTER_PREFIX + medId);
        int currentUnitCounter = 0;
        if (counterBytes != null && counterBytes.length > 0) {
            currentUnitCounter = Integer.parseInt(new String(counterBytes, StandardCharsets.UTF_8));
        }

        List<Unit> createdUnits = new ArrayList<>();

        for (int i = 0; i < anzahl; i++) {
            currentUnitCounter++;
            // Format der UnitID: MedikamentID-ChargeBezeichnung-Zähler (4-stellig)
            String unitId = medId + "-" + chargeBezeichnung + "-" + String.format("%04d", currentUnitCounter);

            if (unitExists(ctx, unitId)) {
                // Dies sollte normalerweise nicht passieren, wenn der Zähler korrekt hochgezählt wird
                throw new ChaincodeException(String.format("Einheit %s existiert bereits. Inkonsistenter Zählerstand.", unitId), PharmaSupplyChainErrors.UNIT_ALREADY_EXISTS.toString());
            }

            Unit newUnit = new Unit(unitId, medId, chargeBezeichnung, ipfsLink, callingActorId);
            ctx.getStub().putState(unitId, JsonUtil.toJson(newUnit).getBytes(StandardCharsets.UTF_8));
            createdUnits.add(newUnit);
        }

        // Aktualisiere den Zähler für dieses Medikament im Ledger
        ctx.getStub().putState(UNIT_COUNTER_PREFIX + medId, String.valueOf(currentUnitCounter).getBytes(StandardCharsets.UTF_8));

        return JsonUtil.toJson(createdUnits);
    }

    /**
     * Fügt einer spezifischen Einheit Temperaturmesswerte hinzu.
     * Nur der aktuelle Eigentümer der Einheit darf Temperaturdaten hinzufügen.
     *
     * @param ctx         Der Transaktionskontext.
     * @param unitId      Die ID der Einheit.
     * @param temperature Der Temperaturwert als String (z.B. "25.5").
     * @return Die aktualisierte Einheit als JSON-String.
     * Example: {"function":"addTemperatureReading","Args":["MED-HASH123-Charge-XYZ-0001","25.5"]}
     */
    @Transaction()
    public String addTemperatureReading(final Context ctx, final String unitId, final String temperature) {
        byte[] unitStateBytes = ctx.getStub().getState(unitId); // getState liefert byte[]

        if (unitStateBytes == null || unitStateBytes.length == 0) { // Prüfen auf null oder leer
            throw new ChaincodeException(String.format("Einheit %s nicht gefunden", unitId), PharmaSupplyChainErrors.UNIT_NOT_FOUND.toString());
        }

        // KORREKTUR: byte[] zu String Konvertierung
        Unit existingUnit = JsonUtil.fromJson(new String(unitStateBytes, StandardCharsets.UTF_8), Unit.class);

        // Überprüfen, ob der aufrufende Akteur der aktuelle Eigentümer der Einheit ist
        String callingActorId = ctx.getClientIdentity().getId();
        if (!Objects.equals(callingActorId, existingUnit.getCurrentOwnerActorId())) {
            throw new ChaincodeException("Nur der aktuelle Eigentümer der Einheit darf Temperaturdaten hinzufügen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        // Aktuellen Zeitstempel hinzufügen
        String timestamp = Instant.now().toString(); // ISO 8601 Format
        existingUnit.addTemperatureReading(timestamp, temperature);

        ctx.getStub().putState(unitId, JsonUtil.toJson(existingUnit).getBytes(StandardCharsets.UTF_8));
        return JsonUtil.toJson(existingUnit);
    }

    /**
     * Transferiert den Besitz einer Einheit vom aktuellen Eigentümer zu einem neuen Eigentümer.
     * Nur der aktuelle Eigentümer darf eine Einheit transferieren.
     *
     * @param ctx            Der Transaktionskontext.
     * @param unitId         Die ID der zu transferierenden Einheit.
     * @param newOwnerActorId Die ActorId des neuen Eigentümers.
     * @return Die aktualisierte Einheit als JSON-String.
     * Example: {"function":"transferUnit","Args":["MED-HASH123-Charge-XYZ-0001","grosshaendler2"]}
     */
    @Transaction()
    public String transferUnit(final Context ctx, final String unitId, final String newOwnerActorId) {
        byte[] unitStateBytes = ctx.getStub().getState(unitId); // getState liefert byte[]
        if (unitStateBytes == null || unitStateBytes.length == 0) { // Prüfen auf null oder leer
            throw new ChaincodeException(String.format("Einheit %s nicht gefunden", unitId), PharmaSupplyChainErrors.UNIT_NOT_FOUND.toString());
        }

        // KORREKTUR: byte[] zu String Konvertierung
        Unit existingUnit = JsonUtil.fromJson(new String(unitStateBytes, StandardCharsets.UTF_8), Unit.class);

        // Überprüfen, ob der aufrufende Akteur der aktuelle Eigentümer der Einheit ist
        String callingActorId = ctx.getClientIdentity().getId();
        if (!Objects.equals(callingActorId, existingUnit.getCurrentOwnerActorId())) {
            throw new ChaincodeException("Nur der aktuelle Eigentümer der Einheit darf den Besitz übertragen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        // Überprüfen, ob der neue Eigentümer ein existierender Akteur ist
        if (!actorExists(ctx, newOwnerActorId)) {
            throw new ChaincodeException(String.format("Neuer Eigentümer Akteur %s nicht gefunden.", newOwnerActorId), PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }

        existingUnit.setCurrentOwnerActorId(newOwnerActorId);
        ctx.getStub().putState(unitId, JsonUtil.toJson(existingUnit).getBytes(StandardCharsets.UTF_8));
        return JsonUtil.toJson(existingUnit);
    }


    /**
     * Fragt eine einzelne Einheit anhand ihrer UnitID ab.
     *
     * @param ctx    Der Transaktionskontext.
     * @param unitId Die ID der Einheit.
     * @return Die Einheit als JSON-String.
     * Example: {"function":"queryUnitById","Args":["MED-f7b7e8d9c0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7-Charge-XYZ-0001"]}
     */
    @Transaction()
    public String queryUnitById(final Context ctx, final String unitId) {
        byte[] unitStateBytes = ctx.getStub().getState(unitId); // getState liefert byte[]

        if (unitStateBytes == null || unitStateBytes.length == 0) { // Prüfen auf null oder leer
            throw new ChaincodeException(String.format("Einheit %s nicht gefunden", unitId), PharmaSupplyChainErrors.UNIT_NOT_FOUND.toString());
        }

        return new String(unitStateBytes, StandardCharsets.UTF_8);
    }

    /**
     * Fragt alle Einheiten ab, die zu einem bestimmten Medikament gehören.
     *
     * @param ctx    Der Transaktionskontext.
     * @param medId Die ID des Medikaments.
     * @return Eine JSON-Zeichenkette aller Einheiten des Medikaments.
     * Example: {"function":"queryUnitsByMedId","Args":["MED-f7b7e8d9c0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7"]}
     */
    @Transaction()
    public String queryUnitsByMedId(final Context ctx, final String medId) {
        List<Unit> unitList = new ArrayList<>();
        // Rich Query, um alle Einheiten mit einem bestimmten medId zu finden (docType = "unit")
        String queryString = String.format("{\"selector\":{\"docType\":\"unit\",\"medId\":\"%s\"}}", medId);
        final QueryResultsIterator<org.hyperledger.fabric.shim.ledger.KeyValue> resultsIterator = ctx.getStub().getQueryResult(queryString);

        for (final org.hyperledger.fabric.shim.ledger.KeyValue kv : resultsIterator) {
            final Unit unit = JsonUtil.fromJson(kv.getStringValue(), Unit.class);
            unitList.add(unit);
        }
        return JsonUtil.toJson(unitList);
    }

    /**
     * Fragt alle Einheiten ab, die einem bestimmten Akteur gehören.
     *
     * @param ctx        Der Transaktionskontext.
     * @param ownerActorId Die ActorId des Eigentümers.
     * @return Eine JSON-Zeichenkette aller Einheiten, die dem Akteur gehören.
     * Example: {"function":"queryUnitsByOwner","Args":["hersteller1"]}
     */
    @Transaction()
    public String queryUnitsByOwner(final Context ctx, final String ownerActorId) {
        List<Unit> unitList = new ArrayList<>();
        // Rich Query, um alle Einheiten zu finden, die einem bestimmten Eigentümer gehören (docType = "unit")
        String queryString = String.format("{\"selector\":{\"docType\":\"unit\",\"currentOwnerActorId\":\"%s\"}}", ownerActorId);
        final QueryResultsIterator<org.hyperledger.fabric.shim.ledger.KeyValue> resultsIterator = ctx.getStub().getQueryResult(queryString);

        for (final org.hyperledger.fabric.shim.ledger.KeyValue kv : resultsIterator) {
            final Unit unit = JsonUtil.fromJson(kv.getStringValue(), Unit.class);
            unitList.add(unit);
        }
        return JsonUtil.toJson(unitList);
    }

    /**
     * Prüft, ob eine Einheit existiert.
     *
     * @param ctx    Der Transaktionskontext.
     * @param unitId Die ID der zu prüfenden Einheit.
     * @return true, wenn die Einheit existiert, false sonst.
     */
    private boolean unitExists(final Context ctx, final String unitId) {
        byte[] unitState = ctx.getStub().getState(unitId);
        return unitState != null && unitState.length > 0;
    }


    /**
     * Generiert einen SHA-256 Hash aus einem String.
     * @param input Der zu hashende String.
     * @return Der SHA-256 Hash als Hex-String.
     * @throws ChaincodeException Wenn ein Fehler bei der Hash-Generierung auftritt.
     */
    private String generateSha256(final String input) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hexString = new StringBuilder();
            for (final byte b : hash) {
                final String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (final Exception e) {
            throw new ChaincodeException("Fehler beim Generieren des SHA-256 Hashs: " + e.getMessage(), e.getClass().getSimpleName());
        }
    }
}

package de.jklein.fabric;

import de.jklein.fabric.models.Actor;
import de.jklein.fabric.models.Medikament;
import de.jklein.fabric.models.Unit;
import de.jklein.fabric.utils.JsonUtil;
import de.jklein.fabric.events.PharmaEvent;
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
        UNIT_NOT_FOUND,
        UNIT_ALREADY_EXISTS,
        MEDIKAMENT_NOT_APPROVED,
        INVALID_UNIT_OWNER
    }

    private static final String UNIT_COUNTER_PREFIX = "unitCounter_";
    private static final String EVENT_NAME = "PharmalinkDataEvent";

    /**
     * Hilfsmethode: Prüft, ob ein Akteur im Ledger existiert.
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
     * Hilfsmethode: Ermittelt und gibt das Actor-Objekt des aufrufenden Akteurs zurück.
     * Leitet die ActorId korrekt aus der Client-Identität ab (Rolle-SHA256(MSPID-ClientIdentityID)).
     *
     * @param ctx Der Smart Contract Kontext.
     * @return Das Actor-Objekt des aufrufenden Akteurs.
     * @throws ChaincodeException Wenn das 'role'-Attribut im Zertifikat fehlt oder der Akteur nicht im Ledger gefunden wird.
     */
    private Actor getCallingActorFromContext(final Context ctx) {
        final String mspId = ctx.getClientIdentity().getMSPID();
        final String clientId = ctx.getClientIdentity().getId();

        final String certRoleRaw = ctx.getClientIdentity().getAttributeValue("role");
        if (certRoleRaw == null || certRoleRaw.isEmpty()) {
            throw new ChaincodeException("Client-Zertifikat enthält kein 'role'-Attribut.", PharmaSupplyChainErrors.MISSING_CERT_ATTRIBUTE.toString());
        }
        String actualRoleFromCert = "";
        if (certRoleRaw.contains(":")) {
            actualRoleFromCert = certRoleRaw.substring(0, certRoleRaw.indexOf(':'));
        } else {
            actualRoleFromCert = certRoleRaw;
        }

        final String combinedIdForHash = mspId + "-" + clientId;
        final String actorSha = generateSha256(combinedIdForHash);
        final String callingActorId = actualRoleFromCert.toLowerCase() + "-" + actorSha;

        byte[] actorStateBytes = ctx.getStub().getState(callingActorId);
        if (actorStateBytes == null || actorStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Aufrufender Akteur '%s' nicht im Ledger gefunden. Bitte registrieren Sie sich zuerst mit 'initCall'.", callingActorId), PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }
        return JsonUtil.fromJson(new String(actorStateBytes, StandardCharsets.UTF_8), Actor.class);
    }

    /**
     * Hilfsmethode zur Überprüfung der Rolle des aufrufenden Akteurs.
     *
     * @param ctx Der Transaktionskontext.
     * @param requiredRole Die erforderliche Rolle.
     * @throws ChaincodeException Wenn der aufrufende Akteur nicht die erforderliche Rolle hat.
     */
    private void verifyCallingActorRole(final Context ctx, final String requiredRole) {
        Actor callingActor = getCallingActorFromContext(ctx);

        if (!callingActor.getRole().equalsIgnoreCase(requiredRole)) {
            String errorMessage = String.format("Akteur %s ist nicht berechtigt. Erforderliche Rolle: %s. Aktuelle Rolle: %s",
                    callingActor.getActorId(), requiredRole, callingActor.getRole());
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
    public String createActor(final Context ctx, final String actorId, final String bezeichnung, final String role, final String email, final String ipfsLink) {
        if (actorExists(ctx, actorId)) {
            throw new ChaincodeException(String.format("Akteur %s existiert bereits", actorId), PharmaSupplyChainErrors.ACTOR_ALREADY_EXISTS.toString());
        }

        verifyCallingActorRole(ctx, "behoerde");

        Actor actor = new Actor(actorId, bezeichnung, role, email, ipfsLink);
        ctx.getStub().putState(actorId, JsonUtil.toJson(actor).getBytes(StandardCharsets.UTF_8));

        PharmaEvent event = new PharmaEvent("Actor", actorId, "CREATED");
        ctx.getStub().setEvent(EVENT_NAME, JsonUtil.toJson(event).getBytes(StandardCharsets.UTF_8));

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
    public String updateActor(final Context ctx, final String actorId, final String newBezeichnung, final String newRole, final String newEmail, final String newIpfsLink) {
        byte[] actorStateBytes = ctx.getStub().getState(actorId);

        if (actorStateBytes == null || actorStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Akteur %s nicht gefunden", actorId), PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }

        Actor existingActor = JsonUtil.fromJson(new String(actorStateBytes, StandardCharsets.UTF_8), Actor.class);

        Actor callingActor = getCallingActorFromContext(ctx);
        if (!Objects.equals(callingActor.getActorId(), actorId) && !callingActor.getRole().equalsIgnoreCase("behoerde")) {
            throw new ChaincodeException("Nicht autorisiert, diesen Akteur zu aktualisieren.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        existingActor.setBezeichnung(newBezeichnung);
        existingActor.setRole(newRole);
        existingActor.setEmail(newEmail);
        existingActor.setIpfsLink(newIpfsLink);

        ctx.getStub().putState(actorId, JsonUtil.toJson(existingActor).getBytes(StandardCharsets.UTF_8));

        PharmaEvent event = new PharmaEvent("Actor", actorId, "UPDATED");
        ctx.getStub().setEvent(EVENT_NAME, JsonUtil.toJson(event).getBytes(StandardCharsets.UTF_8));

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
        verifyCallingActorRole(ctx, "behoerde");
        ctx.getStub().delState(actorId);
        PharmaEvent event = new PharmaEvent("Actor", actorId, "DELETED");
        ctx.getStub().setEvent(EVENT_NAME, JsonUtil.toJson(event).getBytes(StandardCharsets.UTF_8));
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
        String queryString = "{\"selector\":{\"docType\":\"actor\"}}";
        final QueryResultsIterator<org.hyperledger.fabric.shim.ledger.KeyValue> resultsIterator = ctx.getStub().getQueryResult(queryString);
        for (final org.hyperledger.fabric.shim.ledger.KeyValue kv : resultsIterator) {
            final Actor actor = JsonUtil.fromJson(kv.getStringValue(), Actor.class);
            actorList.add(actor);
        }
        return JsonUtil.toJson(actorList);
    }

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
    public String initCall(final Context ctx, final String bezeichnung, final String email, final String ipfsLink) {
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

        final String actorState = stub.getStringState(actorId);
        String newActorJson;
        String operationType;

        if (!actorState.isEmpty()) {
            System.out.println("Akteur mit ID " + actorId + " ist bereits registriert. Rückgabe der Informationen.");
            // Akteur-Objekt aus dem bestehenden Zustand erstellen, um es für ein potenzielles Update zu verwenden
            Actor existingActor = JsonUtil.fromJson(actorState, Actor.class);
            boolean changed = false;
            if (!Objects.equals(existingActor.getBezeichnung(), bezeichnung)) {
                existingActor.setBezeichnung(bezeichnung);
                changed = true;
            }
            if (!Objects.equals(existingActor.getEmail(), email)) {
                existingActor.setEmail(email);
                changed = true;
            }
            if (!Objects.equals(existingActor.getIpfsLink(), ipfsLink)) {
                existingActor.setIpfsLink(ipfsLink);
                changed = true;
            }

            if (changed) {
                stub.putStringState(actorId, JsonUtil.toJson(existingActor));
                newActorJson = JsonUtil.toJson(existingActor);
                operationType = "UPDATED";
                PharmaEvent event = new PharmaEvent("Actor", actorId, operationType);
                stub.setEvent(EVENT_NAME, JsonUtil.toJson(event).getBytes(StandardCharsets.UTF_8));
            } else {
                newActorJson = actorState;
                operationType = "NO_CHANGE"; // Kein Event, da keine Änderung
            }
            return newActorJson;

        } else {
            final Actor newActor = new Actor(actorId, bezeichnung, actualRoleFromCert.toLowerCase(), email, ipfsLink);
            newActorJson = JsonUtil.toJson(newActor);
            stub.putStringState(actorId, newActorJson);

            operationType = "CREATED";
            // Event emittieren
            PharmaEvent event = new PharmaEvent("Actor", actorId, operationType);
            stub.setEvent(EVENT_NAME, JsonUtil.toJson(event).getBytes(StandardCharsets.UTF_8));

            System.out.println("Neuer Akteur registriert: " + newActorJson);
            return newActorJson;
        }
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

        byte[] actorStateBytes = stub.getState(actorId);
        if (actorStateBytes == null || actorStateBytes.length == 0) {
            final String errorMessage = String.format("Akteur mit ID %s nicht gefunden", actorId);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }

        final Actor existingActor = JsonUtil.fromJson(new String(actorStateBytes, StandardCharsets.UTF_8), Actor.class);

        Actor callingActor = getCallingActorFromContext(ctx);
        if (!Objects.equals(actorId, callingActor.getActorId())) {
            final String errorMessage = "Nicht autorisiert: Nur der Eigentümer des Profils darf es aktualisieren.";
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        existingActor.setIpfsLink(newIpfsLink);

        final String updatedActorJson = JsonUtil.toJson(existingActor);
        stub.putStringState(actorId, updatedActorJson);

        PharmaEvent event = new PharmaEvent("Actor", actorId, "UPDATED");
        ctx.getStub().setEvent(EVENT_NAME, JsonUtil.toJson(event).getBytes(StandardCharsets.UTF_8));

        System.out.println("Akteur IPFS Link aktualisiert: " + updatedActorJson);
        return updatedActorJson;
    }

    /**
     * Erstellt ein neues Medikament im Ledger. Nur Hersteller dürfen Medikamente anlegen.
     *
     * @param ctx Der Smart Contract Kontext.
     * @param bezeichnung Die Bezeichnung des Medikaments.
     * @param infoblattHash Der Hash des Infoblatt.
     * @param ipfsLink Der IPFS Link zu weiteren Off-Chain-Informationen des Infoblatt.
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
        Actor callingActor = getCallingActorFromContext(ctx);

        if (!"hersteller".equalsIgnoreCase(callingActor.getRole())) {
            throw new ChaincodeException("Nicht autorisiert: Nur Hersteller dürfen Medikamente anlegen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        final String herstellerId = callingActor.getActorId();

        final String combinedMedIdHashInput = herstellerId + "-" + bezeichnung + "-" + infoblattHash + "-" + ipfsLink;
        final String medSha = generateSha256(combinedMedIdHashInput);
        final String medId = "MED-" + medSha;

        byte[] medikamentStateBytes = stub.getState(medId);
        if (medikamentStateBytes != null && medikamentStateBytes.length > 0) {
            throw new ChaincodeException(
                    String.format("Ein Medikament mit der Bezeichnung '%s' von Hersteller '%s' existiert bereits. Bitte geben Sie eine andere Bezeichnung an.", bezeichnung, herstellerId),
                    PharmaSupplyChainErrors.MEDIKAMENT_ALREADY_EXISTS.toString()
            );
        }

        final Medikament newMedikament = new Medikament(medId, herstellerId, bezeichnung, ipfsLink);
        newMedikament.setInfoblattHash(infoblattHash);

        final String newMedikamentJson = JsonUtil.toJson(newMedikament);
        stub.putStringState(medId, newMedikamentJson);

        stub.putStringState(UNIT_COUNTER_PREFIX + medId, "0");

        PharmaEvent event = new PharmaEvent("Medikament", medId, "CREATED");
        ctx.getStub().setEvent(EVENT_NAME, JsonUtil.toJson(event).getBytes(StandardCharsets.UTF_8));

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
        Actor callingActor = getCallingActorFromContext(ctx);

        if (!"behoerde".equalsIgnoreCase(callingActor.getRole())) {
            throw new ChaincodeException("Nicht autorisiert: Nur Behörden dürfen Medikamente genehmigen/ablehnen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }
        final String approverActorId = callingActor.getActorId();


        byte[] medikamentStateBytes = stub.getState(medId);
        if (medikamentStateBytes == null || medikamentStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Medikament mit ID '%s' nicht gefunden.", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }
        final Medikament existingMedikament = JsonUtil.fromJson(new String(medikamentStateBytes, StandardCharsets.UTF_8), Medikament.class);

        final String lowerCaseNewStatus = newStatus.toLowerCase();
        if (!("freigegeben".equals(lowerCaseNewStatus) || "abgelehnt".equals(lowerCaseNewStatus))) {
            throw new ChaincodeException("Ungültiger Status: Der Status muss 'freigegeben' oder 'abgelehnt' sein.", PharmaSupplyChainErrors.INVALID_MEDIKAMENT_STATUS_CHANGE.toString());
        }

        existingMedikament.setStatus(lowerCaseNewStatus);
        existingMedikament.setApprovedById(approverActorId);
        final String updatedMedikamentJson = JsonUtil.toJson(existingMedikament);
        stub.putStringState(medId, updatedMedikamentJson);

        PharmaEvent event = new PharmaEvent("Medikament", medId, "UPDATED");
        ctx.getStub().setEvent(EVENT_NAME, JsonUtil.toJson(event).getBytes(StandardCharsets.UTF_8));

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
        Actor callingActor = getCallingActorFromContext(ctx);
        final String invokerActorId = callingActor.getActorId();

        byte[] medikamentStateBytes = stub.getState(medId);
        if (medikamentStateBytes == null || medikamentStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Medikament mit ID '%s' nicht gefunden.", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }
        final Medikament existingMedikament = JsonUtil.fromJson(new String(medikamentStateBytes, StandardCharsets.UTF_8), Medikament.class);

        if (!existingMedikament.getHerstellerId().equals(invokerActorId)) {
            throw new ChaincodeException("Nicht autorisiert: Nur der anlegende Hersteller darf dieses Medikament bearbeiten.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        if (newBezeichnung != null && !newBezeichnung.isEmpty()) {
            existingMedikament.setBezeichnung(newBezeichnung);
        }
        if (newInfoblattHash != null && !newInfoblattHash.isEmpty()) {
            existingMedikament.setInfoblattHash(newInfoblattHash);
        }
        if (newIpfsLink != null && !newIpfsLink.isEmpty()) {
            existingMedikament.setIpfsLink(newIpfsLink);
        }

        final String updatedMedikamentJson = JsonUtil.toJson(existingMedikament);
        stub.putStringState(medId, updatedMedikamentJson);

        PharmaEvent event = new PharmaEvent("Medikament", medId, "UPDATED");
        ctx.getStub().setEvent(EVENT_NAME, JsonUtil.toJson(event).getBytes(StandardCharsets.UTF_8));

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
        Actor callingActor = getCallingActorFromContext(ctx);
        final String invokerActorId = callingActor.getActorId();
        final String actualRoleFromCert = callingActor.getRole();

        byte[] medikamentStateBytes = stub.getState(medId);
        if (medikamentStateBytes == null || medikamentStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Medikament mit ID '%s' nicht gefunden.", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }
        final Medikament existingMedikament = JsonUtil.fromJson(new String(medikamentStateBytes, StandardCharsets.UTF_8), Medikament.class);

        final Map<String, String> currentTags = existingMedikament.getTags();
        if ("hersteller".equalsIgnoreCase(actualRoleFromCert)) {
            if (!existingMedikament.getHerstellerId().equals(invokerActorId)) {
                throw new ChaincodeException("Nicht autorisiert: Nur der anlegende Hersteller darf Tags für dieses Medikament setzen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
            }
            currentTags.put("hersteller", tagValue);
        } else if ("behoerde".equalsIgnoreCase(actualRoleFromCert)) {
            currentTags.put("behoerde", tagValue);
        } else {
            throw new ChaincodeException("Nicht autorisiert: Nur Hersteller oder Behörden dürfen Tags setzen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        existingMedikament.setTags(currentTags);
        final String updatedMedikamentJson = JsonUtil.toJson(existingMedikament);
        stub.putStringState(medId, updatedMedikamentJson);

        PharmaEvent event = new PharmaEvent("Medikament", medId, "UPDATED");
        ctx.getStub().setEvent(EVENT_NAME, JsonUtil.toJson(event).getBytes(StandardCharsets.UTF_8));

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
        byte[] medikamentStateBytes = stub.getState(medId);

        if (medikamentStateBytes == null || medikamentStateBytes.length == 0) {
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
        byte[] medikamentStateBytes = ctx.getStub().getState(medId);

        if (medikamentStateBytes == null || medikamentStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Medikament %s nicht gefunden", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }

        Medikament existingMedikament = JsonUtil.fromJson(new String(medikamentStateBytes, StandardCharsets.UTF_8), Medikament.class);
        Actor callingActor = getCallingActorFromContext(ctx);

        if (!(callingActor.getRole().equalsIgnoreCase("behoerde")
                ||
                (callingActor.getRole().equalsIgnoreCase("hersteller") && Objects.equals(callingActor.getActorId(), existingMedikament.getHerstellerId()) && existingMedikament.getStatus().equalsIgnoreCase("angelegt")))) {
            throw new ChaincodeException("Nicht autorisiert, dieses Medikament zu löschen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        ctx.getStub().delState(medId);
        ctx.getStub().delState(UNIT_COUNTER_PREFIX + medId);

        PharmaEvent event = new PharmaEvent("Medikament", medId, "DELETED");
        ctx.getStub().setEvent(EVENT_NAME, JsonUtil.toJson(event).getBytes(StandardCharsets.UTF_8));
    }


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

        byte[] medikamentStateBytes = ctx.getStub().getState(medId);
        if (medikamentStateBytes == null || medikamentStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Medikament %s nicht gefunden", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }

        Medikament existingMedikament = JsonUtil.fromJson(new String(medikamentStateBytes, StandardCharsets.UTF_8), Medikament.class);

        Actor callingActor = getCallingActorFromContext(ctx);

        if (!Objects.equals(callingActor.getActorId(), existingMedikament.getHerstellerId())) {
            throw new ChaincodeException("Nur der Hersteller des Medikaments darf Einheiten erstellen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        if (!existingMedikament.getStatus().equalsIgnoreCase("freigegeben")) {
            throw new ChaincodeException(String.format("Medikament %s ist nicht freigegeben und kann daher nicht in Einheiten unterteilt werden.", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_APPROVED.toString());
        }

        byte[] counterBytes = ctx.getStub().getState(UNIT_COUNTER_PREFIX + medId);
        int currentUnitCounter = 0;
        if (counterBytes != null && counterBytes.length > 0) {
            currentUnitCounter = Integer.parseInt(new String(counterBytes, StandardCharsets.UTF_8));
        }

        List<Unit> createdUnits = new ArrayList<>();

        for (int i = 0; i < anzahl; i++) {
            currentUnitCounter++;
            String unitId = medId + "-" + chargeBezeichnung + "-" + String.format("%04d", currentUnitCounter);

            if (unitExists(ctx, unitId)) {
                throw new ChaincodeException(String.format("Einheit %s existiert bereits. Inkonsistenter Zählerstand.", unitId), PharmaSupplyChainErrors.UNIT_ALREADY_EXISTS.toString());
            }

            Unit newUnit = new Unit(unitId, medId, chargeBezeichnung, ipfsLink, callingActor.getActorId());
            ctx.getStub().putState(unitId, JsonUtil.toJson(newUnit).getBytes(StandardCharsets.UTF_8));
            createdUnits.add(newUnit);

            PharmaEvent event = new PharmaEvent("Unit", unitId, "CREATED");
            ctx.getStub().setEvent(EVENT_NAME, JsonUtil.toJson(event).getBytes(StandardCharsets.UTF_8));
        }

        ctx.getStub().putState(UNIT_COUNTER_PREFIX + medId, String.valueOf(currentUnitCounter).getBytes(StandardCharsets.UTF_8));

        return JsonUtil.toJson(createdUnits);
    }

    /**
     * Fügt einer spezifischen Einheit Temperaturmesswerte hinzu.
     * Nur der aktuelle Eigentümer der Einheit darf Temperaturdaten hinzufügen.
     *
     * @param ctx         Der Transaktionskontext.
     * @param unitId      Die ID der Einheit.
     * @param temperature Der Temperaturwert als String.
     * @param timestamp   Der Zeitstempel der Messung als ISO 8601 String.
     * @return Die aktualisierte Einheit als JSON-String.
     * Example: {"function":"addTemperatureReading","Args":["MED-HASH123-Charge-XYZ-0001","25.5","2025-06-19T10:00:00Z"]}
     */
    @Transaction()
    public String addTemperatureReading(final Context ctx, final String unitId, final String temperature, final String timestamp) {
        byte[] unitStateBytes = ctx.getStub().getState(unitId);
        if (unitStateBytes == null || unitStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Einheit %s nicht gefunden", unitId), PharmaSupplyChainErrors.UNIT_NOT_FOUND.toString());
        }

        Unit existingUnit = JsonUtil.fromJson(new String(unitStateBytes, StandardCharsets.UTF_8), Unit.class);

        Actor callingActor = getCallingActorFromContext(ctx);
        if (!Objects.equals(callingActor.getActorId(), existingUnit.getCurrentOwnerActorId())) {
            throw new ChaincodeException("Nur der aktuelle Eigentümer der Einheit darf Temperaturdaten hinzufügen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        existingUnit.addTemperatureReading(timestamp, temperature);

        ctx.getStub().putState(unitId, JsonUtil.toJson(existingUnit).getBytes(StandardCharsets.UTF_8));

        PharmaEvent event = new PharmaEvent("Unit", unitId, "UPDATED");
        ctx.getStub().setEvent(EVENT_NAME, JsonUtil.toJson(event).getBytes(StandardCharsets.UTF_8));

        return JsonUtil.toJson(existingUnit);
    }

    /**
     * Transferiert den Besitz einer Einheit vom aktuellen Eigentümer zu einem neuen Eigentümer.
     * Nur der aktuelle Eigentümer darf eine Einheit transferieren.
     *
     * @param ctx            Der Transaktionskontext.
     * @param unitId         Die ID der zu transferierenden Einheit.
     * @param newOwnerActorId Die ActorId des neuen Eigentümers.
     * @param transferTimestamp Der Zeitstempel des Transfers als ISO 8601 String.
     * @return Die aktualisierte Einheit als JSON-String.
     * Example: {"function":"transferUnit","Args":["MED-HASH123-Charge-XYZ-0001","grosshaendler2","2025-06-19T10:05:00Z"]}
     */
    @Transaction()
    public String transferUnit(final Context ctx, final String unitId, final String newOwnerActorId, final String transferTimestamp) {
        ChaincodeStub stub = ctx.getStub();
        byte[] unitStateBytes = stub.getState(unitId);
        if (unitStateBytes == null || unitStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Einheit %s nicht gefunden", unitId), PharmaSupplyChainErrors.UNIT_NOT_FOUND.toString());
        }

        Unit existingUnit = JsonUtil.fromJson(new String(unitStateBytes, StandardCharsets.UTF_8), Unit.class);

        Actor callingActor = getCallingActorFromContext(ctx);
        if (!Objects.equals(callingActor.getActorId(), existingUnit.getCurrentOwnerActorId())) {
            throw new ChaincodeException("Nur der aktuelle Eigentümer der Einheit darf den Besitz übertragen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        // NEU: Rolle des neuen Eigentümers prüfen
        byte[] newOwnerActorBytes = stub.getState(newOwnerActorId);
        if (newOwnerActorBytes == null || newOwnerActorBytes.length == 0) {
            throw new ChaincodeException(String.format("Neuer Eigentümer Akteur %s nicht gefunden.", newOwnerActorId), PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }
        Actor newOwnerActor = JsonUtil.fromJson(new String(newOwnerActorBytes, StandardCharsets.UTF_8), Actor.class);

        // Setzen von isConsumed und consumedRefId basierend auf der Rolle des neuen Eigentümers
        if ("apotheke".equalsIgnoreCase(newOwnerActor.getRole()) || "konsument".equalsIgnoreCase(newOwnerActor.getRole())) {
            existingUnit.setIsConsumed(true);
            existingUnit.setConsumedRefId(newOwnerActorId);
        } else {
            existingUnit.setIsConsumed(false); // Sicherstellen, dass es auf false gesetzt wird, wenn Rolle nicht passt
            existingUnit.setConsumedRefId(""); // Sicherstellen, dass es geleert wird
        }

        String previousOwnerId = existingUnit.getCurrentOwnerActorId();
        existingUnit.addTransferEntry(previousOwnerId, newOwnerActorId, transferTimestamp);


        existingUnit.setCurrentOwnerActorId(newOwnerActorId);
        stub.putState(unitId, JsonUtil.toJson(existingUnit).getBytes(StandardCharsets.UTF_8));

        PharmaEvent event = new PharmaEvent("Unit", unitId, "UPDATED");
        ctx.getStub().setEvent(EVENT_NAME, JsonUtil.toJson(event).getBytes(StandardCharsets.UTF_8));

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
        byte[] unitStateBytes = ctx.getStub().getState(unitId);

        if (unitStateBytes == null || unitStateBytes.length == 0) {
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

    /**
     * Sucht nach Medikamenten, deren Bezeichnung einen bestimmten Text enthält (case-insensitive).
     * Erfordert einen Index auf 'docType' und 'bezeichnung' in CouchDB für gute Performance.
     *
     * @param ctx Der Smart Contract Kontext.
     * @param bezeichnungQuery Der Suchtext.
     * @return Eine Liste von passenden Medikamenten als JSON-Array.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryMedikamenteByBezeichnung(final Context ctx, final String bezeichnungQuery) {
        final ChaincodeStub stub = ctx.getStub();
        final List<Medikament> medikamentList = new ArrayList<>();

        final String queryString = String.format(
                "{\"selector\":{\"docType\":\"medikament\",\"bezeichnung\":{\"$regex\":\"(?i)%s\"}}}", bezeichnungQuery);

        final QueryResultsIterator<org.hyperledger.fabric.shim.ledger.KeyValue> resultsIterator = stub.getQueryResult(queryString);

        for (final org.hyperledger.fabric.shim.ledger.KeyValue kv : resultsIterator) {
            final Medikament medikament = JsonUtil.fromJson(kv.getStringValue(), Medikament.class);
            medikamentList.add(medikament);
        }

        return JsonUtil.toJson(medikamentList);
    }

    /**
     * Sucht nach Akteuren, deren Bezeichnung einen bestimmten Text enthält (case-insensitive).
     * Erfordert einen Index auf 'docType' und 'bezeichnung' in CouchDB für gute Performance.
     *
     * @param ctx Der Smart Contract Kontext.
     * @param bezeichnungQuery Der Suchtext, nach dem in der Bezeichnung gesucht wird.
     * @return Eine Liste von passenden Akteuren als JSON-Array.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryActorsByBezeichnung(final Context ctx, final String bezeichnungQuery) {
        final ChaincodeStub stub = ctx.getStub();
        final List<Actor> actorList = new ArrayList<>();

        final String queryString = String.format(
                "{\"selector\":{\"docType\":\"actor\",\"bezeichnung\":{\"$regex\":\"(?i)%s\"}}}", bezeichnungQuery);

        final QueryResultsIterator<org.hyperledger.fabric.shim.ledger.KeyValue> resultsIterator = stub.getQueryResult(queryString);

        for (final org.hyperledger.fabric.shim.ledger.KeyValue kv : resultsIterator) {
            final Actor actor = JsonUtil.fromJson(kv.getStringValue(), Actor.class);
            actorList.add(actor);
        }

        return JsonUtil.toJson(actorList);
    }

    /**
     * Transferiert den Besitz einer Gruppe von Einheiten innerhalb einer Charge vom aktuellen Eigentümer zu einem neuen Eigentümer.
     * Nur der aktuelle Eigentümer aller Einheiten im Batch darf diese transferieren.
     * Alle Einheiten im angegebenen Nummernkreis müssen existieren und denselben aktuellen Besitzer haben wie der aufrufende Akteur.
     *
     * @param ctx Der Transaktionskontext.
     * @param medId Die ID des Medikaments, zu dem die Einheiten gehören.
     * @param chargeBezeichnung Die Bezeichnung der Charge.
     * @param startUnitNumber Die Startnummer der Einheit im Batch (inklusiv, z.B. 1).
     * @param endUnitNumber Die Endnummer der Einheit im Batch (inklusiv, z.B. 10).
     * @param newOwnerActorId Die ActorId des neuen Eigentümers.
     * @param transferTimestamp Der Zeitstempel des Transfers als ISO 8601 String.
     * @return Eine JSON-Zeichenkette der aktualisierten Einheiten.
     * @throws ChaincodeException Wenn ein Medikament oder Akteur nicht gefunden wird, der Aufrufer nicht autorisiert ist
     * oder eine Einheit im Batch nicht existiert/nicht dem Aufrufer gehört.
     *
     * Beispiel für Aufruf:
     * {"function":"transferUnitsInBatch","Args":["MED-HASH123","Charge-XYZ","1","10","grosshaendler2","2025-06-19T10:05:00Z"]}
     */
    @Transaction()
    public String transferUnitsInBatch(final Context ctx,
                                       final String medId,
                                       final String chargeBezeichnung,
                                       final int startUnitNumber,
                                       final int endUnitNumber,
                                       final String newOwnerActorId,
                                       final String transferTimestamp) {

        final ChaincodeStub stub = ctx.getStub();
        Actor callingActor = getCallingActorFromContext(ctx);
        final String invokerActorId = callingActor.getActorId();

        // Rolle des neuen Eigentümers für die isConsumed-Logik prüfen (einmal außerhalb der Schleife)
        byte[] newOwnerActorBytes = stub.getState(newOwnerActorId);
        if (newOwnerActorBytes == null || newOwnerActorBytes.length == 0) {
            throw new ChaincodeException(String.format("Neuer Eigentümer Akteur %s nicht gefunden.", newOwnerActorId), PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }
        Actor newOwnerActor = JsonUtil.fromJson(new String(newOwnerActorBytes, StandardCharsets.UTF_8), Actor.class);
        boolean isNewOwnerConsumerType = "apotheke".equalsIgnoreCase(newOwnerActor.getRole()) || "konsument".equalsIgnoreCase(newOwnerActor.getRole());


        byte[] medikamentStateBytes = stub.getState(medId);
        if (medikamentStateBytes == null || medikamentStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Medikament %s nicht gefunden, Einheiten können nicht transferiert werden.", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }

        if (startUnitNumber <= 0 || endUnitNumber < startUnitNumber) {
            throw new ChaincodeException("Ungültiger Nummernkreis für Einheiten. Startnummer muss positiv sein und Endnummer >= Startnummer.", PharmaSupplyChainErrors.INVALID_ARGUMENT.toString());
        }

        List<Unit> transferredUnits = new ArrayList<>();

        for (int i = startUnitNumber; i <= endUnitNumber; i++) {
            String unitId = medId + "-" + chargeBezeichnung + "-" + String.format("%04d", i);

            byte[] unitStateBytes = stub.getState(unitId);
            if (unitStateBytes == null || unitStateBytes.length == 0) {
                throw new ChaincodeException(String.format("Einheit %s im Batch nicht gefunden. Transaktion abgebrochen.", unitId), PharmaSupplyChainErrors.UNIT_NOT_FOUND.toString());
            }

            Unit existingUnit = JsonUtil.fromJson(new String(unitStateBytes, StandardCharsets.UTF_8), Unit.class);

            if (!Objects.equals(invokerActorId, existingUnit.getCurrentOwnerActorId())) {
                throw new ChaincodeException(String.format("Akteur %s ist nicht der aktuelle Eigentümer von Einheit %s. Batch-Transfer nicht autorisiert.", invokerActorId, unitId), PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
            }

            // Setzen von isConsumed und consumedRefId basierend auf der Rolle des neuen Eigentümers
            if (isNewOwnerConsumerType) {
                existingUnit.setIsConsumed(true);
                existingUnit.setConsumedRefId(newOwnerActorId);
            } else {
                existingUnit.setIsConsumed(false);
                existingUnit.setConsumedRefId("");
            }

            String previousOwnerId = existingUnit.getCurrentOwnerActorId();
            existingUnit.addTransferEntry(previousOwnerId, newOwnerActorId, transferTimestamp);
            existingUnit.setCurrentOwnerActorId(newOwnerActorId);

            stub.putState(unitId, JsonUtil.toJson(existingUnit).getBytes(StandardCharsets.UTF_8));
            transferredUnits.add(existingUnit);

            PharmaEvent event = new PharmaEvent("Unit", unitId, "UPDATED");
            ctx.getStub().setEvent(EVENT_NAME, JsonUtil.toJson(event).getBytes(StandardCharsets.UTF_8));
        }

        System.out.println(String.format("Batch-Transfer von %d Einheiten für Medikament %s, Charge %s von %s zu %s abgeschlossen.",
                transferredUnits.size(), medId, chargeBezeichnung, invokerActorId, newOwnerActorId));

        return JsonUtil.toJson(transferredUnits);
    }

    /**
     * Markiert eine Einheit als an einen Konsumenten übergeben und somit "verbraucht".
     * Nur Akteure mit der Rolle "apotheke" dürfen diese Transaktion ausführen.
     * Die übergebene Einheit muss dem aufrufenden Apotheker gehören.
     *
     * @param ctx Der Smart Contract Kontext.
     * @param unitId Die ID der Einheit, die an den Konsumenten übergeben wird.
     * @param consumentRefId Die Referenz-ID des Konsumenten (kann auch eine externe ID sein).
     * @param timestamp Der Zeitstempel der Übergabe als ISO 8601 String.
     * @return Die aktualisierte Einheit als JSON-String.
     * @throws ChaincodeException Wenn der Aufrufer keine Apotheke ist, die Einheit nicht gefunden wird
     * oder die Einheit nicht dem aufrufenden Apotheker gehört oder bereits verbraucht ist.
     *
     * Example: {"function":"transferUnitToConsumer","Args":["MED-HASH-Charge-0001","KONS-12345","2025-07-05T10:00:00Z"]}
     */
    @Transaction()
    public String transferUnitToConsumer(final Context ctx, final String unitId, final String consumentRefId, final String timestamp) {
        final ChaincodeStub stub = ctx.getStub();
        Actor callingActor = getCallingActorFromContext(ctx);
        final String invokerActorId = callingActor.getActorId();
        verifyCallingActorRole(ctx, "apotheke");
        byte[] unitStateBytes = stub.getState(unitId);
        if (unitStateBytes == null || unitStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Einheit %s nicht gefunden.", unitId), PharmaSupplyChainErrors.UNIT_NOT_FOUND.toString());
        }
        Unit existingUnit = JsonUtil.fromJson(new String(unitStateBytes, StandardCharsets.UTF_8), Unit.class);

        if (!Objects.equals(invokerActorId, existingUnit.getCurrentOwnerActorId())) {
            throw new ChaincodeException(String.format("Akteur %s ist nicht der aktuelle Eigentümer von Einheit %s. Übergabe nicht autorisiert.", invokerActorId, unitId), PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }
        if (existingUnit.getIsConsumed()) {
            throw new ChaincodeException(String.format("Einheit %s wurde bereits als verbraucht markiert und kann nicht erneut übergeben werden.", unitId), PharmaSupplyChainErrors.INVALID_ARGUMENT.toString());
        }
        existingUnit.setIsConsumed(true);
        existingUnit.setConsumedRefId(consumentRefId);
        String previousOwnerId = existingUnit.getCurrentOwnerActorId();
        existingUnit.addTransferEntry(previousOwnerId, consumentRefId, timestamp);
        existingUnit.setCurrentOwnerActorId(consumentRefId);
        stub.putState(unitId, JsonUtil.toJson(existingUnit).getBytes(StandardCharsets.UTF_8));
        PharmaEvent event = new PharmaEvent("Unit", unitId, "UPDATED");
        ctx.getStub().setEvent(EVENT_NAME, JsonUtil.toJson(event).getBytes(StandardCharsets.UTF_8));
        System.out.println(String.format("Einheit %s erfolgreich an Konsument %s übergeben und als verbraucht markiert.", unitId, consumentRefId));
        return JsonUtil.toJson(existingUnit);
    }
}

package de.jklein.fabric;

import de.jklein.fabric.models.Actor;
import de.jklein.fabric.models.Medikament;
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
// import java.time.Instant; // Nicht mehr für Obj. Zeitstempel benötigt
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        INVALID_MEDIKAMENT_STATUS_CHANGE
    }

    // ====================================================================================================
    // AKTEUR-FUNKTIONEN
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
        final String clientId = ctx.getClientIdentity().getId(); // Unique identifier from the certificate

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

        // Rich Query für CouchDB: Selektiert Dokumente mit dem angegebenen 'email' und docType "actor".
        // Benötigt den Index 'indexEmail' in META-INF/statedb/couchdb/indexes/indexEmail.json
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
        // Da E-Mails eindeutig sein sollten, geben wir den ersten Treffer zurück.
        return JsonUtil.toJson(actorList.get(0));
    }


    /**
     * Fragt alle registrierten Akteure ab.
     * OPTIMIERUNG FÜR COUCHDB: Nutzt einen 'docType'-Selektor und erfordert einen entsprechenden Index.
     *
     * @param ctx Der Smart Contract Kontext.
     * @return Eine Liste aller Akteure als JSON-Array von Actor-Objekten.
     *
     * Beispiel für Aufruf:
     * {"function":"queryAllActors","Args":[]}
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryAllActors(final Context ctx) {
        final ChaincodeStub stub = ctx.getStub();
        final List<Actor> actorList = new ArrayList<>();

        // Rich Query für CouchDB: Selektiert alle Dokumente mit docType "actor".
        // Dies ist die robusteste Methode, um alle Instanzen eines bestimmten Typs zu finden.
        // Benötigt den Index 'indexDocType' in META-INF/statedb/couchdb/indexes/indexDocType.json
        final String queryString = "{\"selector\":{\"docType\":\"actor\"}}";
        final QueryResultsIterator<org.hyperledger.fabric.shim.ledger.KeyValue> resultsIterator = stub.getQueryResult(queryString);

        for (final org.hyperledger.fabric.shim.ledger.KeyValue kv : resultsIterator) {
            final Actor actor = JsonUtil.fromJson(kv.getStringValue(), Actor.class);
            actorList.add(actor);
        }

        return JsonUtil.toJson(actorList);
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

        // Rich Query für CouchDB basierend auf der Rolle und docType "actor".
        // Benötigt den Index 'indexRole' in META-INF/statedb/couchdb/indexes/indexRole.json
        final String queryString = String.format("{\"selector\":{\"role\":\"%s\", \"docType\":\"actor\"}}", role.toLowerCase()); // Rolle und docType filtern
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

        final String actorState = stub.getStringState(actorId);

        if (actorState.isEmpty()) {
            final String errorMessage = String.format("Akteur mit ID %s nicht gefunden", actorId);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }

        final Actor existingActor = JsonUtil.fromJson(actorState, Actor.class);

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
    // MEDIKAMENT-FUNKTIONEN
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
     * {"function":"createMedikament","Args":["Paracetamol 500mg","a1b2c3d4e5f6...","QmHashdesInfoblatts","2025-06-19T10:30:00Z"]}
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
        final String herstellerActorState = stub.getStringState(herstellerId);
        if (herstellerActorState.isEmpty()) {
            throw new ChaincodeException("Die Hersteller-ID des Aufrufers ist nicht im System registriert.", PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }

        // 2. Medikamenten-ID generieren: MED-SHA256(HerstellerID-Bezeichnung)
        final String combinedMedIdHashInput = herstellerId + "-" + bezeichnung;
        final String medSha = generateSha256(combinedMedIdHashInput);
        final String medId = "MED-" + medSha;

        final String medikamentState = stub.getStringState(medId);
        if (!medikamentState.isEmpty()) {
            throw new ChaincodeException(String.format("Medikament mit ID '%s' existiert bereits.", medId), PharmaSupplyChainErrors.MEDIKAMENT_ALREADY_EXISTS.toString());
        }

        // 3. Medikament-Objekt erstellen und im Ledger speichern
        final Medikament newMedikament = new Medikament(medId, herstellerId, bezeichnung, ipfsLink);
        newMedikament.setInfoblattHash(infoblattHash);

        final String newMedikamentJson = JsonUtil.toJson(newMedikament);
        stub.putStringState(medId, newMedikamentJson);

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
     * {"function":"approveMedikament","Args":["MED-a1b2c3d4e5f6...","freigegeben","2025-06-19T10:35:00Z"]}
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
        final String medikamentState = stub.getStringState(medId);
        if (medikamentState.isEmpty()) {
            throw new ChaincodeException(String.format("Medikament mit ID '%s' nicht gefunden.", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }
        final Medikament existingMedikament = JsonUtil.fromJson(medikamentState, Medikament.class);

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
     * {"function":"updateMedikament","Args":["MED-a1b2c3d4e5f6...","Paracetamol Forte","neuerhash","neueripfslink","2025-06-19T10:40:00Z"]}
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
        final String medikamentState = stub.getStringState(medId);
        if (medikamentState.isEmpty()) {
            throw new ChaincodeException(String.format("Medikament mit ID '%s' nicht gefunden.", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }
        final Medikament existingMedikament = JsonUtil.fromJson(medikamentState, Medikament.class);

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
     * {"function":"addMedikamentTag","Args":["MED-a1b2c3d4e5f6...","Produktion Charge X erfolgreich abgeschlossen","2025-06-19T10:45:00Z"]}
     * Beispiel für Aufruf (von Behörde):
     * {"function":"addMedikamentTag","Args":["MED-a1b2c3d4e5f6...","Zulassung 2024-06-19 erteilt","2025-06-19T10:46:00Z"]}
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
        final String medikamentState = stub.getStringState(medId);
        if (medikamentState.isEmpty()) {
            throw new ChaincodeException(String.format("Medikament mit ID '%s' nicht gefunden.", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }
        final Medikament existingMedikament = JsonUtil.fromJson(medikamentState, Medikament.class);

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
        final String medikamentState = stub.getStringState(medId);

        if (medikamentState.isEmpty()) {
            final String errorMessage = String.format("Medikament mit ID '%s' nicht gefunden.", medId);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }
        return medikamentState;
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

        // Rich Query für CouchDB basierend auf herstellerId und docType "medikament"
        // Benötigt den Index 'indexHerstellerId' in META-INF/statedb/couchdb/indexes/indexHerstellerId.json
        final String queryString = String.format("{\"selector\":{\"herstellerId\":\"%s\", \"docType\":\"medikament\"}}", herstellerId);
        final QueryResultsIterator<org.hyperledger.fabric.shim.ledger.KeyValue> resultsIterator = stub.getQueryResult(queryString);

        for (final org.hyperledger.fabric.shim.ledger.KeyValue kv : resultsIterator) {
            final Medikament medikament = JsonUtil.fromJson(kv.getStringValue(), Medikament.class);
            medikamentList.add(medikament);
        }

        return JsonUtil.toJson(medikamentList);
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

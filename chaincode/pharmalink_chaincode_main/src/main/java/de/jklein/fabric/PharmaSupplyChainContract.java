package de.jklein.fabric;

import de.jklein.fabric.models.Actor;
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
import java.util.ArrayList;
import java.util.List;

@Contract(
        name = "PharmaSupplyChainContract",
        info = @Info(
                title = "Pharma Supply Chain Chaincode",
                description = "Chaincode für die Verwaltung von Akteuren in einer pharmazeutischen Lieferkette",
                version = "1.0.0-SNAPSHOT"))
@Default
public final class PharmaSupplyChainContract implements ContractInterface {

    private enum PharmaSupplyChainErrors {
        ACTOR_NOT_FOUND,
        ACTOR_ALREADY_EXISTS,
        INVALID_ARGUMENT,
        UNAUTHORIZED_ACCESS
    }

    /**
     * Registriert einen neuen Akteur im Ledger oder gibt dessen Informationen zurück, wenn er bereits existiert.
     * Die Actor ID wird auf Basis des SHA-256 Hashs der Client-Identität (resultierend aus MSPID und der eindeutigen
     * ID des Zertifikats des aufrufenden Akteurs, wie von Fabric zurückgegeben) und der zugewiesenen Rolle gebildet:
     * Rolle-SHA256(MSPID-ClientIdentityID). Dies gewährleistet eine konsistente ID-Generierung über verschiedene Peers hinweg,
     * solange die zugrunde liegende Client-Identität (Zertifikat und MSPID) gleich bleibt.
     *
     * @param ctx Der Smart Contract Kontext.
     * @param email E-Mail des Akteurs.
     * @param role Rolle des Akteurs (z.B. "hersteller", "grosshaendler", "apotheke", "behoerde").
     * @param ipfsLink Optionaler IPFS Link für weitere Attribute des Akteurs. Kann leer sein.
     * @return Die Informationen des registrierten oder bereits existierenden Akteurs als JSON-String.
     * @throws ChaincodeException Wenn die Rolle ungültig ist oder ein Hash-Fehler auftritt.
     *
     * Beispiel für Aufruf:
     * {"function":"initCall","Args":["max.mustermann@example.com","hersteller","QmWgX..."]}
     * {"function":"initCall","Args":["erika.musterfrau@example.com","apotheke",""]}
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT) // KORRIGIERT: TYPE in Großbuchstaben
    public String initCall(final Context ctx, final String email, final String role, final String ipfsLink) {
        ChaincodeStub stub = ctx.getStub();
        String mspId = ctx.getClientIdentity().getMSPID();
        // Verwendung von getId() für Kompatibilität, wie in Fabric-Samples gezeigt
        String clientId = ctx.getClientIdentity().getId();

        // Generierung der Actor ID: Rolle-SHA256(MSPID-ClientIdentityID)
        String combinedIdForHash = mspId + "-" + clientId;
        String actorSha = generateSha256(combinedIdForHash);
        String actorId = role.toLowerCase() + "-" + actorSha; // Rolle in Kleinbuchstaben für Konsistenz

        String actorState = stub.getStringState(actorId);

        if (!actorState.isEmpty()) {
            // Akteur existiert bereits, gib Informationen zurück
            System.out.println("Akteur mit ID " + actorId + " ist bereits registriert. Rückgabe der Informationen.");
            return actorState;
        }

        // Überprüfen, ob die angegebene Rolle eine gültige Affiliation ist
        // (laut fabric_setup_test_consortium.sh)
        boolean isValidRole = false;
        String[] allowedRoles = {"hersteller", "grosshaendler", "apotheke", "behoerde"};
        for (String allowedRole : allowedRoles) {
            if (allowedRole.equalsIgnoreCase(role)) {
                isValidRole = true;
                break;
            }
        }

        if (!isValidRole) {
            String errorMessage = String.format("Die Rolle '%s' ist nicht gültig. Erlaubte Rollen sind: %s", role, String.join(", ", allowedRoles));
            throw new ChaincodeException(errorMessage, PharmaSupplyChainErrors.INVALID_ARGUMENT.toString());
        }

        // Akteur registrieren (ohne 'name' und 'organization')
        Actor newActor = new Actor(actorId, role.toLowerCase(), email, ipfsLink);
        String newActorJson = JsonUtil.toJson(newActor);
        stub.putStringState(actorId, newActorJson);

        System.out.println("Neuer Akteur registriert: " + newActorJson);
        return newActorJson;
    }

    /**
     * Fragt die Informationen eines spezifischen Akteurs anhand seiner Actor ID ab.
     * Die Actor ID hat das Format: Rolle-SHA256(MSPID-Zertifikat-ID).
     *
     * @param ctx Der Smart Contract Kontext.
     * @param actorId Die eindeutige ID des Akteurs (z.B. "hersteller-a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2").
     * @return Die Akteur-Informationen als JSON-String.
     * @throws ChaincodeException Wenn der Akteur nicht gefunden wird.
     *
     * Beispiel für Aufruf:
     * {"function":"queryActorById","Args":["hersteller-a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2"]}
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE) // KORRIGIERT: TYPE in Großbuchstaben
    public String queryActorById(final Context ctx, final String actorId) {
        ChaincodeStub stub = ctx.getStub();
        String actorState = stub.getStringState(actorId);

        if (actorState.isEmpty()) {
            String errorMessage = String.format("Akteur mit ID %s nicht gefunden", actorId);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }
        return actorState;
    }

    /**
     * Fragt alle registrierten Akteure ab.
     *
     * @param ctx Der Smart Contract Kontext.
     * @return Eine Liste aller Akteure als JSON-Array von Actor-Objekten.
     *
     * Beispiel für Aufruf:
     * {"function":"queryAllActors","Args":[]}
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE) // KORRIGIERT: TYPE in Großbuchstaben
    public String queryAllActors(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<Actor> actorList = new ArrayList<>();

        // OPTIMIERUNG FÜR COUCHDB: Rich Query für alle Dokumente, die unserem ActorId-Schema entsprechen.
        // Diese Abfrage ist effizienter, wenn ein Index auf 'actorId' existiert.
        // Benötigt den Index 'indexActorId' in META-INF/statedb/couchdb/indexes/indexActorId.json
        String queryString = "{\"selector\":{\"actorId\":{\"$regex\":\"^.*-\\\\[a-f0-9\\\\]{64}$\"}}}";
        QueryResultsIterator<org.hyperledger.fabric.shim.ledger.KeyValue> resultsIterator = stub.getQueryResult(queryString);

        for (org.hyperledger.fabric.shim.ledger.KeyValue kv : resultsIterator) {
            Actor actor = JsonUtil.fromJson(kv.getStringValue(), Actor.class);
            actorList.add(actor);
        }

        return JsonUtil.toJson(actorList);
    }

    /**
     * Fragt alle Akteure mit einer spezifischen Rolle ab.
     *
     * @param ctx Der Smart Contract Kontext.
     * @param role Die Rolle, nach der gefiltert werden soll (z.B. "hersteller").
     * @return Eine Liste von Akteuren mit der angegebenen Rolle als JSON-Array von Actor-Objekten.
     *
     * Beispiel für Aufruf:
     * {"function":"queryActorsByRole","Args":["hersteller"]}
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE) // KORRIGIERT: TYPE in Großbuchstaben
    public String queryActorsByRole(final Context ctx, final String role) {
        ChaincodeStub stub = ctx.getStub();
        List<Actor> actorList = new ArrayList<>();

        // OPTIMIERUNG FÜR COUCHDB: Rich Query für Akteure basierend auf der Rolle.
        // Benötigt den Index 'indexRole' in META-INF/statedb/couchdb/indexes/indexRole.json
        String queryString = String.format("{\"selector\":{\"role\":\"%s\"}}", role.toLowerCase()); // Rolle in Kleinbuchstaben für Abfrage
        QueryResultsIterator<org.hyperledger.fabric.shim.ledger.KeyValue> resultsIterator = stub.getQueryResult(queryString);

        for (org.hyperledger.fabric.shim.ledger.KeyValue kv : resultsIterator) {
            Actor actor = JsonUtil.fromJson(kv.getStringValue(), Actor.class);
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
    @Transaction(intent = Transaction.TYPE.SUBMIT) // KORRIGIERT: TYPE in Großbuchstaben
    public String updateActorIpfsLink(final Context ctx, final String actorId, final String newIpfsLink) {
        ChaincodeStub stub = ctx.getStub();
        String mspId = ctx.getClientIdentity().getMSPID();
        // Verwendung von getId() für Kompatibilität
        String clientId = ctx.getClientIdentity().getId();

        String actorState = stub.getStringState(actorId);

        if (actorState.isEmpty()) {
            String errorMessage = String.format("Akteur mit ID %s nicht gefunden", actorId);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }

        Actor existingActor = JsonUtil.fromJson(actorState, Actor.class);

        // Überprüfen der Berechtigung: Nur der Akteur selbst darf sein Profil aktualisieren
        // Wir rekonstruieren die erwartete Actor ID basierend auf der Identität des Aufrufers
        // und der Rolle des gefundenen Akteurs.
        String expectedActorShaSuffix = generateSha256(mspId + "-" + clientId);
        String expectedActorId = existingActor.getRole().toLowerCase() + "-" + expectedActorShaSuffix;

        if (!actorId.equals(expectedActorId)) {
            String errorMessage = "Nicht autorisiert: Nur der Eigentümer des Profils darf es aktualisieren.";
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        existingActor.setIpfsLink(newIpfsLink);

        String updatedActorJson = JsonUtil.toJson(existingActor);
        stub.putStringState(actorId, updatedActorJson);

        System.out.println("Akteur IPFS Link aktualisiert: " + updatedActorJson);
        return updatedActorJson;
    }

    /**
     * Generiert einen SHA-256 Hash aus einem String.
     * @param input Der zu hashende String.
     * @return Der SHA-256 Hash als Hex-String.
     * @throws ChaincodeException Wenn ein Fehler bei der Hash-Generierung auftritt.
     */
    private String generateSha256(final String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new ChaincodeException("Fehler beim Generieren des SHA-256 Hashs: " + e.getMessage(), e.getClass().getSimpleName());
        }
    }
}

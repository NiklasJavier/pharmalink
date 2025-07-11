package de.jklein.fabric;

import de.jklein.fabric.models.Actor;
import de.jklein.fabric.models.Medikament;
import de.jklein.fabric.models.Unit;
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
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

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
        INVALID_UNIT_OWNER,
        MEDIKAMENT_HAS_UNITS
    }

    private static final String UNIT_COUNTER_PREFIX = "unitCounter_";

    private void emitEvent(final Context ctx, final String eventName, final Object payloadObject) {
        try {
            String payloadJson = JsonUtil.toJson(payloadObject);
            ctx.getStub().setEvent(eventName, payloadJson.getBytes(StandardCharsets.UTF_8));
            System.out.println("Ereignis ausgelöst: " + eventName + " mit Inhalt: " + payloadJson);
        } catch (Exception e) {
            System.err.println("Fehler beim Auslösen von Ereignis " + eventName + ": " + e.getMessage());
        }
    }

    private boolean actorExists(final Context ctx, final String actorId) {
        byte[] actorState = ctx.getStub().getState(actorId);
        return actorState != null && actorState.length > 0;
    }

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

    private void verifyCallingActorRole(final Context ctx, final String requiredRole) {
        Actor callingActor = getCallingActorFromContext(ctx);

        if (!callingActor.getRole().equalsIgnoreCase(requiredRole)) {
            String errorMessage = String.format("Akteur %s ist nicht berechtigt. Erforderliche Rolle: %s. Aktuelle Rolle: %s",
                    callingActor.getActorId(), requiredRole, callingActor.getRole());
            throw new ChaincodeException(errorMessage, PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }
    }

    // Bsp.: {"function":"createActor","Args":["apotheke-123","Sonnen-Apotheke","apotheke","info@sonnen-apotheke.de","Qm..."]}
    @Transaction()
    public String createActor(final Context ctx, final String actorId, final String bezeichnung, final String role, final String email, final String ipfsLink) {
        if (actorExists(ctx, actorId)) {
            throw new ChaincodeException(String.format("Akteur %s existiert bereits", actorId), PharmaSupplyChainErrors.ACTOR_ALREADY_EXISTS.toString());
        }

        verifyCallingActorRole(ctx, "behoerde");

        Actor actor = new Actor(actorId, bezeichnung, role, email, ipfsLink);
        ctx.getStub().putState(actorId, JsonUtil.toJson(actor).getBytes(StandardCharsets.UTF_8));
        emitEvent(ctx, "ActorCreated", actor);
        return JsonUtil.toJson(actor);
    }

    // Bsp.: {"function":"queryActor","Args":["apotheke-123"]}
    @Transaction()
    public String queryActor(final Context ctx, final String actorId) {
        byte[] actorState = ctx.getStub().getState(actorId);

        if (actorState == null || actorState.length == 0) {
            throw new ChaincodeException(String.format("Akteur %s nicht gefunden", actorId), PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }

        return new String(actorState, StandardCharsets.UTF_8);
    }

    // Bsp.: {"function":"updateActor","Args":["apotheke-123","Sonnen-Apotheke Neu","new@sonnen-apotheke.de","QmNew..."]}
    @Transaction()
    public String updateActor(final Context ctx, final String actorId, final String newBezeichnung, final String newEmail, final String newIpfsLink) {
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
        existingActor.setEmail(newEmail);
        existingActor.setIpfsLink(newIpfsLink);

        final String updatedActorJson = JsonUtil.toJson(existingActor);
        ctx.getStub().putState(actorId, updatedActorJson.getBytes(StandardCharsets.UTF_8));
        emitEvent(ctx, "ActorUpdated", existingActor);
        return updatedActorJson;
    }

    // Bsp.: {"function":"deleteActor","Args":["apotheke-123"]}
    @Transaction()
    public void deleteActor(final Context ctx, final String actorId) {
        if (!actorExists(ctx, actorId)) {
            throw new ChaincodeException(String.format("Akteur %s nicht gefunden", actorId), PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }

        verifyCallingActorRole(ctx, "behoerde");

        ctx.getStub().delState(actorId);
        Map<String, String> deletePayload = new TreeMap<>();
        deletePayload.put("actorId", actorId);
        deletePayload.put("docType", "actor");
        emitEvent(ctx, "ActorDeleted", deletePayload);
    }

    // Bsp.: {"function":"queryAllActors","Args":[]}
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

    // Bsp.: {"function":"initCall","Args":[]}
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String initCall(final Context ctx) {
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

        if (!actorState.isEmpty()) {
            System.out.println("Akteur mit ID " + actorId + " ist bereits registriert. Rückgabe der Informationen.");
            return actorState;
        }

        final Actor newActor = new Actor(actorId, actualRoleFromCert.toLowerCase());

        final String newActorJson = JsonUtil.toJson(newActor);
        stub.putStringState(actorId, newActorJson);
        emitEvent(ctx, "ActorInitialized", newActor);

        System.out.println("Neuer Akteur mit leeren Stammdaten registriert: " + newActorJson);
        return newActorJson;
    }

    // Bsp.: {"function":"queryActorById","Args":["apotheke-123..."]}
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

    // Bsp.: {"function":"queryActorByEmail","Args":["info@sonnen-apotheke.de"]}
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

    // Bsp.: {"function":"queryActorsByRole","Args":["apotheke"]}
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

    // Bsp.: {"function":"updateActorIpfsLink","Args":["apotheke-123...","QmUpdated..."]}
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
        emitEvent(ctx, "ActorIpfsLinkUpdated", existingActor);
        System.out.println("Akteur IPFS Link aktualisiert: " + updatedActorJson);
        return updatedActorJson;
    }

    // Bsp.: {"function":"createMedikament","Args":["Aspirin 500mg","hash123","Qm..."]}
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String createMedikament(final Context ctx, final String bezeichnung, final String infoblattHash, final String ipfsLink) {
        final ChaincodeStub stub = ctx.getStub();
        Actor callingActor = getCallingActorFromContext(ctx);

        if (!"hersteller".equalsIgnoreCase(callingActor.getRole())) {
            throw new ChaincodeException("Nicht autorisiert: Nur Hersteller dürfen Medikamente anlegen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        final String herstellerId = callingActor.getActorId();

        final String combinedMedIdHashInput = herstellerId + "-" + bezeichnung;
        final String medSha = generateSha256(combinedMedIdHashInput);
        final String medId = "MED-" + medSha;

        byte[] medikamentStateBytes = stub.getState(medId);
        if (medikamentStateBytes != null && medikamentStateBytes.length > 0) {
            throw new ChaincodeException(String.format("Medikament mit ID '%s' existiert bereits.", medId), PharmaSupplyChainErrors.MEDIKAMENT_ALREADY_EXISTS.toString());
        }

        final Medikament newMedikament = new Medikament(medId, herstellerId, bezeichnung, ipfsLink);
        newMedikament.setInfoblattHash(infoblattHash);

        final String newMedikamentJson = JsonUtil.toJson(newMedikament);
        stub.putStringState(medId, newMedikamentJson);

        stub.putStringState(UNIT_COUNTER_PREFIX + medId, "0");
        emitEvent(ctx, "MedikamentCreated", newMedikament);
        System.out.println("Neues Medikament angelegt: " + newMedikamentJson);
        return newMedikamentJson;
    }

    // Bsp.: {"function":"approveMedikament","Args":["MED-abc...","freigegeben"]}
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
        emitEvent(ctx, "MedikamentStatusUpdated", existingMedikament);
        System.out.println("Medikamentstatus aktualisiert: " + updatedMedikamentJson);
        return updatedMedikamentJson;
    }

    // Bsp.: {"function":"updateMedikament","Args":["MED-abc...","Aspirin Forte","newhash","newQm..."]}
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
        emitEvent(ctx, "MedikamentUpdated", existingMedikament);
        System.out.println("Medikament aktualisiert: " + updatedMedikamentJson);
        return updatedMedikamentJson;
    }

    // Bsp.: {"function":"addMedikamentTag","Args":["MED-abc...","Charge 2 geprüft"]}
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
        emitEvent(ctx, "MedikamentTagAdded", existingMedikament);
        System.out.println("Medikament-Tag aktualisiert: " + updatedMedikamentJson);
        return updatedMedikamentJson;
    }

    // Bsp.: {"function":"queryMedikamentById","Args":["MED-abc..."]}
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

    // Bsp.: {"function":"queryMedikamenteByHerstellerId","Args":["hersteller-xyz..."]}
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

    // Bsp.: {"function":"deleteMedikament","Args":["MED-abc..."]}
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
        Map<String, String> deletePayload = new TreeMap<>();
        deletePayload.put("medId", medId);
        deletePayload.put("docType", "medikament");
        emitEvent(ctx, "MedikamentDeleted", deletePayload);
    }

    // Bsp.: {"function":"createUnits","Args":["MED-abc...","CH-2025-07","100","QmUnits..."]}
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
            emitEvent(ctx, "UnitCreated", newUnit);
        }

        ctx.getStub().putState(UNIT_COUNTER_PREFIX + medId, String.valueOf(currentUnitCounter).getBytes(StandardCharsets.UTF_8));

        return JsonUtil.toJson(createdUnits);
    }

    // Bsp.: {"function":"addTemperatureReading","Args":["UNIT-xyz...","5.5","2025-07-12T10:00:00Z"]}
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
        emitEvent(ctx, "UnitTemperatureAdded", existingUnit);
        return JsonUtil.toJson(existingUnit);
    }

    // Bsp.: {"function":"transferUnit","Args":["UNIT-xyz...","apotheke-123","2025-07-12T11:00:00Z"]}
    @Transaction()
    public String transferUnit(final Context ctx, final String unitId, final String newOwnerActorId, final String transferTimestamp) {
        byte[] unitStateBytes = ctx.getStub().getState(unitId);
        if (unitStateBytes == null || unitStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Einheit %s nicht gefunden", unitId), PharmaSupplyChainErrors.UNIT_NOT_FOUND.toString());
        }

        Unit existingUnit = JsonUtil.fromJson(new String(unitStateBytes, StandardCharsets.UTF_8), Unit.class);

        Actor callingActor = getCallingActorFromContext(ctx);
        if (!Objects.equals(callingActor.getActorId(), existingUnit.getCurrentOwnerActorId())) {
            throw new ChaincodeException("Nur der aktuelle Eigentümer der Einheit darf den Besitz übertragen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        if (!actorExists(ctx, newOwnerActorId)) {
            throw new ChaincodeException(String.format("Neuer Eigentümer Akteur %s nicht gefunden.", newOwnerActorId), PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }

        String previousOwnerId = existingUnit.getCurrentOwnerActorId();
        existingUnit.addTransferEntry(previousOwnerId, newOwnerActorId, transferTimestamp);

        existingUnit.setCurrentOwnerActorId(newOwnerActorId);
        ctx.getStub().putState(unitId, JsonUtil.toJson(existingUnit).getBytes(StandardCharsets.UTF_8));
        emitEvent(ctx, "UnitTransferred", existingUnit);
        return JsonUtil.toJson(existingUnit);
    }

    // Bsp.: {"function":"queryUnitById","Args":["UNIT-xyz..."]}
    @Transaction()
    public String queryUnitById(final Context ctx, final String unitId) {
        byte[] unitStateBytes = ctx.getStub().getState(unitId);

        if (unitStateBytes == null || unitStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Einheit %s nicht gefunden", unitId), PharmaSupplyChainErrors.UNIT_NOT_FOUND.toString());
        }

        return new String(unitStateBytes, StandardCharsets.UTF_8);
    }

    // Bsp.: {"function":"queryUnitsByMedId","Args":["MED-abc..."]}
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

    // Bsp.: {"function":"queryUnitsByOwner","Args":["hersteller-xyz..."]}
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

    private boolean unitExists(final Context ctx, final String unitId) {
        byte[] unitState = ctx.getStub().getState(unitId);
        return unitState != null && unitState.length > 0;
    }

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

    // Bsp.: {"function":"queryMedikamenteByBezeichnung","Args":["Aspirin"]}
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

    // Bsp.: {"function":"queryActorsByBezeichnung","Args":["Sonnen-Apotheke"]}
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

    // Bsp.: {"function":"transferUnitRange","Args":["MED-abc...","CH-2025-07","1","50","grosshaendler-456","2025-07-12T12:00:00Z"]}
    @Transaction()
    public String transferUnitRange(final Context ctx, final String medId, final String chargeBezeichnung,
                                    final int startCounter, final int endCounter,
                                    final String newOwnerActorId, final String transferTimestamp) {

        if (startCounter <= 0 || endCounter < startCounter) {
            throw new ChaincodeException("Ungültiger Zählerbereich.", PharmaSupplyChainErrors.INVALID_ARGUMENT.toString());
        }

        final Actor callingActor = getCallingActorFromContext(ctx);
        final String previousOwnerId = callingActor.getActorId();

        if (!actorExists(ctx, newOwnerActorId)) {
            throw new ChaincodeException(String.format("Neuer Eigentümer Akteur %s nicht gefunden.", newOwnerActorId), PharmaSupplyChainErrors.ACTOR_NOT_FOUND.toString());
        }

        List<Unit> unitsToTransfer = new ArrayList<>();

        for (int i = startCounter; i <= endCounter; i++) {
            String unitId = medId + "-" + chargeBezeichnung + "-" + String.format("%04d", i);

            byte[] unitStateBytes = ctx.getStub().getState(unitId);
            if (unitStateBytes == null || unitStateBytes.length == 0) {
                throw new ChaincodeException(String.format("Einheit %s im Bereich nicht gefunden. Transaktion abgebrochen.", unitId), PharmaSupplyChainErrors.UNIT_NOT_FOUND.toString());
            }

            Unit unit = JsonUtil.fromJson(new String(unitStateBytes, StandardCharsets.UTF_8), Unit.class);

            if (!Objects.equals(previousOwnerId, unit.getCurrentOwnerActorId())) {
                throw new ChaincodeException(String.format("Sie sind nicht der Besitzer der Einheit %s. Transaktion abgebrochen.", unitId), PharmaSupplyChainErrors.INVALID_UNIT_OWNER.toString());
            }
            unitsToTransfer.add(unit);
        }

        for (Unit unit : unitsToTransfer) {
            unit.addTransferEntry(previousOwnerId, newOwnerActorId, transferTimestamp);
            unit.setCurrentOwnerActorId(newOwnerActorId);
            ctx.getStub().putState(unit.getUnitId(), JsonUtil.toJson(unit).getBytes(StandardCharsets.UTF_8));
            emitEvent(ctx, "UnitTransferred", unit);
        }

        String successMessage = String.format("%d Einheiten (Bereich %d-%d) erfolgreich an %s übertragen.",
                (endCounter - startCounter + 1), startCounter, endCounter, newOwnerActorId);
        System.out.println(successMessage);
        return successMessage;
    }

    // Bsp.: {"function":"deleteUnits","Args":["[\"UNIT-001\",\"UNIT-002\"]"]}
    @Transaction()
    public void deleteUnits(final Context ctx, final String unitIdsJson) {
        final ChaincodeStub stub = ctx.getStub();
        final Actor callingActor = getCallingActorFromContext(ctx);
        final String callerId = callingActor.getActorId();

        final String[] unitIds = JsonUtil.fromJson(unitIdsJson, String[].class);
        if (unitIds == null || unitIds.length == 0) {
            throw new ChaincodeException("Keine Chargen-IDs zum Löschen angegeben.", PharmaSupplyChainErrors.INVALID_ARGUMENT.toString());
        }

        for (final String unitId : unitIds) {
            final byte[] unitStateBytes = stub.getState(unitId);
            if (unitStateBytes == null || unitStateBytes.length == 0) {
                throw new ChaincodeException(String.format("Charge %s nicht gefunden. Transaktion wird abgebrochen.", unitId), PharmaSupplyChainErrors.UNIT_NOT_FOUND.toString());
            }
            final Unit unit = JsonUtil.fromJson(new String(unitStateBytes, StandardCharsets.UTF_8), Unit.class);
            if (!Objects.equals(callerId, unit.getCurrentOwnerActorId())) {
                throw new ChaincodeException(String.format("Sie sind nicht der Besitzer der Charge %s. Transaktion wird abgebrochen.", unitId), PharmaSupplyChainErrors.INVALID_UNIT_OWNER.toString());
            }
        }

        for (final String unitId : unitIds) {
            stub.delState(unitId);
            Map<String, String> deletePayload = new TreeMap<>();
            deletePayload.put("unitId", unitId);
            deletePayload.put("docType", "unit");
            emitEvent(ctx, "UnitDeleted", deletePayload);
        }

        System.out.printf("%d Chargen erfolgreich gelöscht.%n", unitIds.length);
    }

    // Bsp.: {"function":"queryAllMedikamente","Args":[]}
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryAllMedikamente(final Context ctx) {
        List<Medikament> medikamentList = new ArrayList<>();
        String queryString = "{\"selector\":{\"docType\":\"medikament\"}}";
        final QueryResultsIterator<org.hyperledger.fabric.shim.ledger.KeyValue> resultsIterator = ctx.getStub().getQueryResult(queryString);
        for (final org.hyperledger.fabric.shim.ledger.KeyValue kv : resultsIterator) {
            final Medikament medikament = JsonUtil.fromJson(kv.getStringValue(), Medikament.class);
            medikamentList.add(medikament);
        }
        return JsonUtil.toJson(medikamentList);
    }

    // Bsp.: {"function":"deleteMedikamentIfNoUnits","Args":["MED-abc..."]}
    @Transaction()
    public void deleteMedikamentIfNoUnits(final Context ctx, final String medId) {
        final ChaincodeStub stub = ctx.getStub();

        final byte[] medikamentStateBytes = stub.getState(medId);
        if (medikamentStateBytes == null || medikamentStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Medikament %s nicht gefunden", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }
        final Medikament existingMedikament = JsonUtil.fromJson(new String(medikamentStateBytes, StandardCharsets.UTF_8), Medikament.class);
        final Actor callingActor = getCallingActorFromContext(ctx);

        if (!(callingActor.getRole().equalsIgnoreCase("behoerde")
                || (callingActor.getRole().equalsIgnoreCase("hersteller")
                && Objects.equals(callingActor.getActorId(), existingMedikament.getHerstellerId())
                && existingMedikament.getStatus().equalsIgnoreCase("angelegt")))) {
            throw new ChaincodeException("Nicht autorisiert, dieses Medikament zu löschen.", PharmaSupplyChainErrors.UNAUTHORIZED_ACCESS.toString());
        }

        final String unitsJson = queryUnitsByMedId(ctx, medId);
        final Unit[] units = JsonUtil.fromJson(unitsJson, Unit[].class);
        if (units.length > 0) {
            throw new ChaincodeException(String.format("Medikament %s kann nicht gelöscht werden, da bereits %d Charge(n) existieren.", medId, units.length), PharmaSupplyChainErrors.MEDIKAMENT_HAS_UNITS.toString());
        }

        stub.delState(medId);
        stub.delState(UNIT_COUNTER_PREFIX + medId);

        Map<String, String> deletePayload = new TreeMap<>();
        deletePayload.put("medId", medId);
        deletePayload.put("docType", "medikament");
        emitEvent(ctx, "MedikamentDeleted", deletePayload);
    }

    // Bsp.: {"function":"queryChargeCountsByMedId","Args":["MED-abc..."]}
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryChargeCountsByMedId(final Context ctx, final String medId) {
        final ChaincodeStub stub = ctx.getStub();
        final Map<String, Integer> chargeCounts = new TreeMap<>();

        byte[] medikamentStateBytes = stub.getState(medId);
        if (medikamentStateBytes == null || medikamentStateBytes.length == 0) {
            throw new ChaincodeException(String.format("Medikament mit ID '%s' nicht gefunden.", medId), PharmaSupplyChainErrors.MEDIKAMENT_NOT_FOUND.toString());
        }

        String queryString = String.format("{\"selector\":{\"docType\":\"unit\",\"medId\":\"%s\"}}", medId);
        final QueryResultsIterator<org.hyperledger.fabric.shim.ledger.KeyValue> resultsIterator = stub.getQueryResult(queryString);

        for (final org.hyperledger.fabric.shim.ledger.KeyValue kv : resultsIterator) {
            final Unit unit = JsonUtil.fromJson(kv.getStringValue(), Unit.class);
            String chargeBezeichnung = unit.getChargeBezeichnung();
            chargeCounts.put(chargeBezeichnung, chargeCounts.getOrDefault(chargeBezeichnung, 0) + 1);
        }

        return JsonUtil.toJson(chargeCounts);
    }
}

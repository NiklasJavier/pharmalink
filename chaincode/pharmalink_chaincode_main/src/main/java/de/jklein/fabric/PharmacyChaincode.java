package de.jklein.fabric;

import com.owlike.genson.Genson;
import de.jklein.fabric.assets.Actor;
import de.jklein.fabric.assets.Batch;
import de.jklein.fabric.assets.DrugInfo;
import de.jklein.fabric.assets.DrugUnit;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Contract(
        name = "PharmacyChaincode",
        info = @Info(title = "Pharmacy Supply Chain Contract", version = "4.2.0",
                license = @License(name = "Apache-2.0"),
                contact = @Contact(email = "info@jklein.de", name = "Chaincode-Support")))
@Default
public final class PharmacyChaincode implements ContractInterface {
    private final Genson GENSON = new Genson();
    private static final String ACTOR_KEY_PREFIX = "actor~";
    private static final String DRUGINFO_KEY_PREFIX = "druginfo~";
    private static final String BATCH_KEY_PREFIX = "batch~";
    private static final String UNIT_KEY_PREFIX = "unit~";
    private static final String ACTOR_ID_COUNTER_KEY = "actorIdCounter";

    private enum PharmacyErrors {
        ACTOR_NOT_FOUND, ACTOR_ALREADY_EXISTS, ACTOR_NOT_APPROVED,
        DRUGINFO_NOT_FOUND, DRUGINFO_ALREADY_EXISTS, DRUGINFO_NOT_APPROVED,
        GTIN_ALREADY_EXISTS,
        DRUGUNIT_NOT_FOUND, BATCH_NOT_FOUND,
        UNAUTHORIZED, INVALID_ROLE, UNIT_LOCKED, UNIT_ALREADY_DISPENSED,
        DUPLICATE_PENDING_ACTOR
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void initLedger(final Context ctx) {
        final ChaincodeStub stub = ctx.getStub();
        final String callerId = ctx.getClientIdentity().getId();
        final String callerMspId = ctx.getClientIdentity().getMSPID();
        final Actor behoerde = new Actor(callerId, callerMspId, "BEHOERDE-001", "Regulierungsbehörde", "behoerde", "Approved");
        final String actorKey = ACTOR_KEY_PREFIX + callerId;
        if (stub.getStringState(actorKey) == null || stub.getStringState(actorKey).isEmpty()) {
            stub.putStringState(actorKey, behoerde.toJSONString());
        }
        if (stub.getStringState(ACTOR_ID_COUNTER_KEY) == null || stub.getStringState(ACTOR_ID_COUNTER_KEY).isEmpty()) {
            stub.putStringState(ACTOR_ID_COUNTER_KEY, "0");
        }
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void requestActorRegistration(final Context ctx, final String name) {
        final ChaincodeStub stub = ctx.getStub();
        final String callerId = ctx.getClientIdentity().getId();
        final String callerMspId = ctx.getClientIdentity().getMSPID();
        final String callerRole = getClientRole(ctx);
        if (callerRole == null || callerRole.equals("behoerde")) {
            throw new ChaincodeException("Behörden können sich nicht registrieren.", PharmacyErrors.INVALID_ROLE.toString());
        }
        final String key = ACTOR_KEY_PREFIX + callerId;
        if (stub.getStringState(key) != null && !stub.getStringState(key).isEmpty()) {
            throw new ChaincodeException("Ein Akteur mit diesem Zertifikat existiert bereits.", PharmacyErrors.ACTOR_ALREADY_EXISTS.toString());
        }
        final Actor newActor = new Actor(callerId, callerMspId, null, name, callerRole, "Pending");
        stub.putStringState(key, newActor.toJSONString());
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Actor approveActor(final Context ctx, final String actorName) {
        assertBehoerde(ctx);
        final ChaincodeStub stub = ctx.getStub();
        final String actorQuery = String.format("{\"selector\":{\"name\":\"%s\", \"status\":\"Pending\", \"_id\":{\"$regex\":\"^%s\"}}}", actorName, ACTOR_KEY_PREFIX);
        final String queryResult = richQuery(ctx, actorQuery);
        if (queryResult.equals("[]")) {
            throw new ChaincodeException("Kein wartender Akteur mit dem Namen '" + actorName + "' gefunden.", PharmacyErrors.ACTOR_NOT_FOUND.toString());
        }
        final Actor[] actors = GENSON.deserialize(queryResult, Actor[].class);
        if (actors.length > 1) {
            throw new ChaincodeException("Mehrere wartende Akteure mit dem Namen '" + actorName + "' gefunden. Bitte bereinigen.", PharmacyErrors.DUPLICATE_PENDING_ACTOR.toString());
        }
        final Actor pendingActor = actors[0];
        final String key = ACTOR_KEY_PREFIX + pendingActor.getCertId();
        int currentIdNum = Integer.parseInt(stub.getStringState(ACTOR_ID_COUNTER_KEY));
        currentIdNum++;
        final String newActorId = pendingActor.getRole().toUpperCase() + "-" + String.format("%03d", currentIdNum);

        final Actor approvedActor = new Actor(pendingActor.getCertId(), pendingActor.getMspId(), newActorId, pendingActor.getName(), pendingActor.getRole(), "Approved");
        stub.putStringState(key, approvedActor.toJSONString());
        stub.putStringState(ACTOR_ID_COUNTER_KEY, String.valueOf(currentIdNum));
        return approvedActor;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryPendingActors(final Context ctx) {
        assertBehoerde(ctx);
        return richQuery(ctx, String.format("{\"selector\":{\"status\":\"Pending\", \"_id\":{\"$regex\":\"^%s\"}}}", ACTOR_KEY_PREFIX));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryAllActors(final Context ctx) {
        assertBehoerde(ctx);
        final ChaincodeStub stub = ctx.getStub();
        final List<Actor> allActors = new ArrayList<>();
        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(ACTOR_KEY_PREFIX, ACTOR_KEY_PREFIX + Character.MAX_VALUE)) {
            for (final KeyValue result : results) {
                final Actor actor = Actor.fromJSONString(result.getStringValue());
                allActors.add(actor);
            }
        } catch (final Exception e) {
            throw new ChaincodeException("Abfrage aller Akteure fehlgeschlagen: " + e.getMessage());
        }
        return GENSON.serialize(allActors);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryActorsByRole(final Context ctx, final String role) {
        assertBehoerde(ctx);
        return richQuery(ctx, String.format("{\"selector\":{\"role\":\"%s\", \"status\":\"Approved\", \"_id\":{\"$regex\":\"^%s\"}}}", role, ACTOR_KEY_PREFIX));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugInfo createDrugInfo(final Context ctx, final String name, final String description, final String gtin) {
        final Actor manufacturer = assertApprovedActor(ctx, "hersteller");
        final ChaincodeStub stub = ctx.getStub();
        final String gtinQuery = String.format("{\"selector\":{\"gtin\":\"%s\", \"_id\":{\"$regex\":\"^%s\"}}}", gtin, DRUGINFO_KEY_PREFIX);
        if (!richQuery(ctx, gtinQuery).equals("[]")) {
            throw new ChaincodeException("Ein Medikament mit dieser GTIN existiert bereits.", PharmacyErrors.GTIN_ALREADY_EXISTS.toString());
        }
        final String drugId = "DRUG-" + ctx.getStub().getTxId();
        final DrugInfo drugInfo = new DrugInfo(drugId, gtin, name, manufacturer.getActorId(), description, "NotApproved");
        stub.putStringState(DRUGINFO_KEY_PREFIX + drugId, drugInfo.toJSONString());
        return drugInfo;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugInfo approveDrugInfo(final Context ctx, final String drugId) {
        assertBehoerde(ctx);
        final ChaincodeStub stub = ctx.getStub();
        final String key = DRUGINFO_KEY_PREFIX + drugId;
        final DrugInfo drugInfo = getAsset(stub, key, DrugInfo.class, PharmacyErrors.DRUGINFO_NOT_FOUND);
        final DrugInfo approvedDrugInfo = new DrugInfo(drugInfo.getId(), drugInfo.getGtin(), drugInfo.getName(), drugInfo.getManufacturerId(), drugInfo.getDescription(), "Approved");
        stub.putStringState(key, approvedDrugInfo.toJSONString());
        return approvedDrugInfo;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryDrugByGTIN(final Context ctx, final String gtin) {
        final String drugInfoQuery = String.format("{\"selector\":{\"gtin\":\"%s\", \"_id\":{\"$regex\":\"^%s\"}}}", gtin, DRUGINFO_KEY_PREFIX);
        final String drugInfoResult = richQuery(ctx, drugInfoQuery);
        if (drugInfoResult.equals("[]")) {
            throw new ChaincodeException("Kein Medikament mit GTIN " + gtin + " gefunden.", PharmacyErrors.DRUGINFO_NOT_FOUND.toString());
        }

        final DrugInfo[] drugInfosArray = GENSON.deserialize(drugInfoResult, DrugInfo[].class);
        final List<DrugInfo> drugInfos = Arrays.asList(drugInfosArray);
        final DrugInfo drugInfo = drugInfos.get(0);

        final String batchQuery = String.format("{\"selector\":{\"drugId\":\"%s\", \"_id\":{\"$regex\":\"^%s\"}}}", drugInfo.getId(), BATCH_KEY_PREFIX);
        final String batchesResult = richQuery(ctx, batchQuery);
        final Map<String, Object> response = new HashMap<>();
        response.put("medikament", drugInfo);
        response.put("chargenSummary", GENSON.deserialize(batchesResult, List.class));
        return GENSON.serialize(response);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public long balanceOf(final Context ctx, final String ownerActorId) {
        final String query = String.format("{\"selector\":{\"owner\":\"%s\", \"_id\":{\"$regex\":\"^%s\"}}}", ownerActorId, UNIT_KEY_PREFIX);
        long count = 0;
        try (QueryResultsIterator<KeyValue> results = ctx.getStub().getQueryResult(query)) {
            for (final KeyValue ignored : results) {
                count++;
            }
        } catch (final Exception e) {
            throw new ChaincodeException("Fehler beim Zählen der Einheiten: " + e.getMessage());
        }
        return count;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String ownerOf(final Context ctx, final String unitId) {
        final DrugUnit unit = getAsset(ctx.getStub(), UNIT_KEY_PREFIX + unitId, DrugUnit.class, PharmacyErrors.DRUGUNIT_NOT_FOUND);
        return unit.getOwner();
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Batch mintBatch(final Context ctx, final String drugId, final long quantity, final String beschreibung) {
        final Actor manufacturer = assertApprovedActor(ctx, "hersteller");
        final ChaincodeStub stub = ctx.getStub();
        final DrugInfo drugInfo = getAsset(stub, DRUGINFO_KEY_PREFIX + drugId, DrugInfo.class, PharmacyErrors.DRUGINFO_NOT_FOUND);
        if (!"Approved".equals(drugInfo.getStatus())) {
            throw new ChaincodeException("Medikamenten-Stammdaten sind nicht genehmigt.", PharmacyErrors.DRUGINFO_NOT_APPROVED.toString());
        }
        if (!drugInfo.getManufacturerId().equals(manufacturer.getActorId())) {
            throw new ChaincodeException("Nur der ursprüngliche Hersteller darf Chargen für dieses Medikament anlegen.", PharmacyErrors.UNAUTHORIZED.toString());
        }
        final String batchId = "BATCH-" + ctx.getStub().getTxId();
        final Batch batch = new Batch(batchId, drugId, quantity, manufacturer.getActorId(), beschreibung, new ArrayList<>());
        stub.putStringState(BATCH_KEY_PREFIX + batchId, batch.toJSONString());
        for (int i = 1; i <= quantity; i++) {
            final String unitId = batchId + "-" + String.format("%05d", i);
            final DrugUnit unit = new DrugUnit.Builder()
                    .id(unitId)
                    .batchId(batchId)
                    .drugId(drugId)
                    .owner(manufacturer.getActorId())
                    .manufacturerId(manufacturer.getActorId())
                    .description(beschreibung)
                    .tags(new ArrayList<>())
                    .currentState("Created")
                    .build();
            stub.putStringState(UNIT_KEY_PREFIX + unitId, unit.toJSONString());
        }
        stub.setEvent("BatchMinted", batch.toJSONString().getBytes(StandardCharsets.UTF_8));
        return batch;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugUnit transferFrom(final Context ctx, final String fromActorId, final String toActorId, final String unitId, final String newState) {
        final Actor caller = assertApprovedActor(ctx, null);
        final ChaincodeStub stub = ctx.getStub();
        if (!caller.getActorId().equals(fromActorId)) {
            throw new ChaincodeException("Der Aufrufer ist nicht der angegebene 'from' Akteur.", PharmacyErrors.UNAUTHORIZED.toString());
        }
        final DrugUnit unit = getAsset(stub, UNIT_KEY_PREFIX + unitId, DrugUnit.class, PharmacyErrors.DRUGUNIT_NOT_FOUND);
        if (!unit.getOwner().equals(fromActorId)) {
            throw new ChaincodeException("Der 'from' Akteur ist nicht der aktuelle Besitzer der Einheit.", PharmacyErrors.UNAUTHORIZED.toString());
        }
        assertActorIsApprovedById(ctx, toActorId);

        final DrugUnit newUnit = new DrugUnit.Builder()
                .id(unit.getId())
                .batchId(unit.getBatchId())
                .drugId(unit.getDrugId())
                .owner(toActorId)
                .manufacturerId(unit.getManufacturerId())
                .description(unit.getDescription())
                .tags(unit.getTags())
                .currentState(newState)
                .dispensedBy(unit.getDispensedBy())
                .dispensedTo(unit.getDispensedTo())
                .dispensingTimestamp(unit.getDispensingTimestamp())
                .build();
        stub.putStringState(UNIT_KEY_PREFIX + unitId, newUnit.toJSONString());
        stub.setEvent("Transfer", GENSON.serialize(new String[]{fromActorId, toActorId, unitId}).getBytes(StandardCharsets.UTF_8));
        return newUnit;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryBatchUnits(final Context ctx, final String batchId) {
        return richQuery(ctx, String.format("{\"selector\":{\"batchId\":\"%s\", \"_id\":{\"$regex\":\"^%s\"}}}", batchId, UNIT_KEY_PREFIX));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void tagBatch(final Context ctx, final String batchId, final String tag) {
        assertBehoerde(ctx);
        final ChaincodeStub stub = ctx.getStub();
        final Batch batch = getAsset(stub, BATCH_KEY_PREFIX + batchId, Batch.class, PharmacyErrors.BATCH_NOT_FOUND);

        List<String> newTags = new ArrayList<>(batch.getTags());
        if (!newTags.contains(tag)) {
            newTags.add(tag);
        }
        final Batch newBatch = new Batch(batch.getId(), batch.getDrugId(), batch.getQuantity(), batch.getManufacturerId(), batch.getDescription(), newTags);
        stub.putStringState(BATCH_KEY_PREFIX + batchId, newBatch.toJSONString());

        final String unitQuery = String.format("{\"selector\":{\"batchId\":\"%s\", \"_id\":{\"$regex\":\"^%s\"}}}", batchId, UNIT_KEY_PREFIX);
        try (QueryResultsIterator<KeyValue> results = stub.getQueryResult(unitQuery)) {
            for (final KeyValue result : results) {
                final DrugUnit oldUnit = DrugUnit.fromJSONString(result.getStringValue());
                List<String> unitTags = new ArrayList<>(oldUnit.getTags());
                if (!unitTags.contains(tag)) {
                    unitTags.add(tag);
                }
                final DrugUnit newUnit = new DrugUnit.Builder()
                        .id(oldUnit.getId()).batchId(oldUnit.getBatchId()).drugId(oldUnit.getDrugId())
                        .owner(oldUnit.getOwner()).manufacturerId(oldUnit.getManufacturerId())
                        .description(oldUnit.getDescription()).tags(unitTags)
                        .currentState(oldUnit.getCurrentState()).dispensedBy(oldUnit.getDispensedBy())
                        .dispensedTo(oldUnit.getDispensedTo()).dispensingTimestamp(oldUnit.getDispensingTimestamp())
                        .build();
                stub.putStringState(UNIT_KEY_PREFIX + oldUnit.getId(), newUnit.toJSONString());
            }
        } catch (final Exception e) {
            throw new ChaincodeException("Fehler bei der Übertragung des Tags auf die Units: " + e.getMessage());
        }
        stub.setEvent("BatchTagged", newBatch.toJSONString().getBytes(StandardCharsets.UTF_8));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryUnitsByTag(final Context ctx, final String tag) {
        return richQuery(ctx, String.format("{\"selector\":{\"tags\":{\"$elemMatch\":{\"$eq\":\"%s\"}}, \"_id\":{\"$regex\":\"^%s\"}}}", tag, UNIT_KEY_PREFIX));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugUnit dispenseDrugUnit(final Context ctx, final String unitId, final String recipientId) {
        final Actor pharmacy = assertApprovedActor(ctx, "apotheke");
        final ChaincodeStub stub = ctx.getStub();
        final DrugUnit unit = getAsset(stub, UNIT_KEY_PREFIX + unitId, DrugUnit.class, PharmacyErrors.DRUGUNIT_NOT_FOUND);
        if (unit.getTags().contains("sperre")) {
            throw new ChaincodeException("Diese Einheit ist gesperrt und kann nicht ausgegeben werden.", PharmacyErrors.UNIT_LOCKED.toString());
        }
        if (!unit.getOwner().equals(pharmacy.getActorId())) {
            throw new ChaincodeException("Nur der aktuelle Besitzer darf die Einheit austragen.", PharmacyErrors.UNAUTHORIZED.toString());
        }
        if ("Dispensed".equals(unit.getCurrentState())) {
            throw new ChaincodeException("Diese Einheit wurde bereits ausgegeben.", PharmacyErrors.UNIT_ALREADY_DISPENSED.toString());
        }

        final String timestamp = ctx.getStub().getTxTimestamp().toString();
        final DrugUnit dispensedUnit = new DrugUnit.Builder()
                .id(unit.getId()).batchId(unit.getBatchId()).drugId(unit.getDrugId())
                .owner("Dispensed").manufacturerId(unit.getManufacturerId())
                .description(unit.getDescription()).tags(unit.getTags())
                .currentState("Dispensed").dispensedBy(pharmacy.getActorId())
                .dispensedTo(recipientId).dispensingTimestamp(timestamp)
                .build();

        stub.putStringState(UNIT_KEY_PREFIX + unitId, dispensedUnit.toJSONString());
        stub.setEvent("DrugDispensed", dispensedUnit.toJSONString().getBytes(StandardCharsets.UTF_8));
        return dispensedUnit;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryDispensedByPharmacy(final Context ctx, final String apothekenId) {
        final Actor caller = assertApprovedActor(ctx, null);
        if (!"behoerde".equals(caller.getRole()) && !caller.getActorId().equals(apothekenId)) {
            throw new ChaincodeException("Sie sind nicht berechtigt, diese Abfrage durchzuführen.", PharmacyErrors.UNAUTHORIZED.toString());
        }
        return richQuery(ctx, String.format("{\"selector\":{\"dispensedBy\":\"%s\", \"_id\":{\"$regex\":\"^%s\"}}}", apothekenId, UNIT_KEY_PREFIX));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryUnitDetails(final Context ctx, final String unitId) {
        final ChaincodeStub stub = ctx.getStub();
        final DrugUnit unit = getAsset(stub, UNIT_KEY_PREFIX + unitId, DrugUnit.class, PharmacyErrors.DRUGUNIT_NOT_FOUND);
        final Batch batch = getAsset(stub, BATCH_KEY_PREFIX + unit.getBatchId(), Batch.class, PharmacyErrors.BATCH_NOT_FOUND);
        final DrugInfo drugInfo = getAsset(stub, DRUGINFO_KEY_PREFIX + unit.getDrugId(), DrugInfo.class, PharmacyErrors.DRUGINFO_NOT_FOUND);
        final List<Object> history = new ArrayList<>();
        try (QueryResultsIterator<KeyModification> results = stub.getHistoryForKey(UNIT_KEY_PREFIX + unitId)) {
            for (final KeyModification result : results) {
                history.add(GENSON.deserialize(result.getStringValue(), Map.class));
            }
        } catch (final Exception e) {
            throw new ChaincodeException("Fehler beim Abrufen der Historie: " + e.getMessage());
        }
        final Map<String, Object> response = new LinkedHashMap<>();
        response.put("medikamentInfo", drugInfo);
        response.put("chargeInfo", batch);
        response.put("einheitInfo", unit);
        response.put("transferLog", history);
        return GENSON.serialize(response);
    }

    private String getClientRole(final Context ctx) {
        return ctx.getClientIdentity().getAttributeValue("role");
    }

    private void assertBehoerde(final Context ctx) {
        if (!"behoerde".equals(getClientRole(ctx))) {
            throw new ChaincodeException("Nur 'behoerde' ist berechtigt.", PharmacyErrors.UNAUTHORIZED.toString());
        }
    }

    private Actor assertApprovedActor(final Context ctx, final String expectedRole) {
        final ChaincodeStub stub = ctx.getStub();
        final String callerId = ctx.getClientIdentity().getId();
        final String callerRole = getClientRole(ctx);
        if (expectedRole != null && !expectedRole.equals(callerRole)) {
            throw new ChaincodeException(String.format("Erwartete Rolle '%s', aber Aufrufer hat Rolle '%s'.", expectedRole, callerRole), PharmacyErrors.INVALID_ROLE.toString());
        }
        final Actor actor = getAsset(stub, ACTOR_KEY_PREFIX + callerId, Actor.class, PharmacyErrors.ACTOR_NOT_FOUND);
        if (!"Approved".equals(actor.getStatus())) {
            throw new ChaincodeException("Der aufrufende Akteur ist nicht genehmigt.", PharmacyErrors.ACTOR_NOT_APPROVED.toString());
        }
        return actor;
    }

    private void assertActorIsApprovedById(final Context ctx, final String actorId) {
        final String query = String.format("{\"selector\":{\"actorId\":\"%s\", \"status\":\"Approved\", \"_id\":{\"$regex\":\"^%s\"}}}", actorId, ACTOR_KEY_PREFIX);
        if ("[]".equals(richQuery(ctx, query))) {
            throw new ChaincodeException("Akteur " + actorId + " ist nicht gefunden oder nicht genehmigt.", PharmacyErrors.ACTOR_NOT_APPROVED.toString());
        }
    }

    private <T> T getAsset(final ChaincodeStub stub, final String key, final Class<T> clazz, final PharmacyErrors error) {
        final String json = stub.getStringState(key);
        if (json == null || json.isEmpty()) {
            throw new ChaincodeException(String.format("Asset mit Schlüssel %s nicht gefunden", key), error.toString());
        }
        return GENSON.deserialize(json, clazz);
    }

    private String richQuery(final Context ctx, final String query) {
        final ChaincodeStub stub = ctx.getStub();
        final StringBuilder sb = new StringBuilder("[");
        try (QueryResultsIterator<KeyValue> results = stub.getQueryResult(query)) {
            boolean first = true;
            for (final KeyValue result : results) {
                if (!first) {
                    sb.append(",");
                }
                sb.append(result.getStringValue());
                first = false;
            }
        } catch (final Exception e) {
            throw new ChaincodeException("Rich Query fehlgeschlagen: " + e.getMessage());
        }
        sb.append("]");
        return sb.toString();
    }
}

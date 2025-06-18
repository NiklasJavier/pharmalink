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
import org.hyperledger.fabric.shim.ledger.KeyModification; // KORREKTUR: Nötiger Import
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;

@Contract(
        name = "PharmacyChaincode",
        info = @Info(
                title = "Pharmacy Supply Chain Contract",
                description = "Ein Prototyp für eine pharmazeutische Lieferkette",
                version = "1.0.0",
                license = @License(
                        name = "Apache-2.0"
                ),
                contact = @Contact(
                        email = "info@jklein.de",
                        name = "JKlein"
                )
        )
)
@Default
public final class PharmacyChaincode implements ContractInterface {

    private static final Genson GENSON = new Genson();

    private enum PharmacyErrors {
        ACTOR_NOT_FOUND,
        ACTOR_ALREADY_EXISTS,
        ACTOR_ALREADY_APPROVED,
        ACTOR_NOT_APPROVED,
        DRUGINFO_NOT_FOUND,
        DRUGINFO_ALREADY_EXISTS,
        BATCH_NOT_FOUND,
        BATCH_ALREADY_EXISTS,
        DRUGUNIT_NOT_FOUND,
        DRUGUNIT_ALREADY_EXISTS,
        INSUFFICIENT_PERMISSIONS,
        WRONG_OWNER
    }

    private final String ACTOR_KEY_PREFIX = "actor~";
    private final String DRUGINFO_KEY_PREFIX = "druginfo~";
    private final String BATCH_KEY_PREFIX = "batch~";
    private final String DRUGUNIT_KEY_PREFIX = "drugunit~";


    @Transaction()
    public void initLedger(final Context ctx) {
    }

    @Transaction()
    public Actor registerActor(final Context ctx, final String name) {
        ChaincodeStub stub = ctx.getStub();
        String mspId = ctx.getClientIdentity().getMSPID();
        String certId = ctx.getClientIdentity().getId();
        String role = ctx.getClientIdentity().getAttributeValue("role");
        String actorId = getActorIdFromCertificate(ctx);

        if (role == null || role.isEmpty()) {
            throw new ChaincodeException("Die Rolle des Benutzers ist nicht im Zertifikat gesetzt.", PharmacyErrors.INSUFFICIENT_PERMISSIONS.toString());
        }

        String key = ACTOR_KEY_PREFIX + actorId;
        String existingActor = stub.getStringState(key);
        if (existingActor != null && !existingActor.isEmpty()) {
            throw new ChaincodeException("Akteur mit der ID " + actorId + " existiert bereits.", PharmacyErrors.ACTOR_ALREADY_EXISTS.toString());
        }

        Actor actor = new Actor(certId, mspId, actorId, name, role, "Pending");
        stub.putStringState(key, actor.toJSONString());

        return actor;
    }

    @Transaction()
    public Actor approveActor(final Context ctx, final String actorIdToApprove) {
        requireRole(ctx, "behoerde");

        ChaincodeStub stub = ctx.getStub();
        String key = ACTOR_KEY_PREFIX + actorIdToApprove;
        Actor actor = getAsset(stub, key, Actor.class, PharmacyErrors.ACTOR_NOT_FOUND);

        if ("Approved".equals(actor.getStatus())) {
            throw new ChaincodeException("Akteur " + actorIdToApprove + " ist bereits genehmigt.", PharmacyErrors.ACTOR_ALREADY_APPROVED.toString());
        }

        Actor approvedActor = new Actor(actor.getCertId(), actor.getMspId(), actor.getActorId(), actor.getName(), actor.getRole(), "Approved");
        stub.putStringState(key, approvedActor.toJSONString());

        return approvedActor;
    }

    @Transaction()
    public String getActor(final Context ctx, final String actorId) {
        return ctx.getStub().getStringState(ACTOR_KEY_PREFIX + actorId);
    }

    @Transaction()
    public DrugInfo createDrugInfo(final Context ctx, final String gtin, final String name, final String description) {
        requireRole(ctx, "hersteller");
        String manufacturerId = getActorIdFromCertificate(ctx);

        ChaincodeStub stub = ctx.getStub();
        String drugId = "DRUG-" + gtin;
        String key = DRUGINFO_KEY_PREFIX + drugId;

        String existingDrugInfo = stub.getStringState(key);
        if (existingDrugInfo != null && !existingDrugInfo.isEmpty()) {
            throw new ChaincodeException("DrugInfo mit der ID " + drugId + " existiert bereits.", PharmacyErrors.DRUGINFO_ALREADY_EXISTS.toString());
        }

        DrugInfo drugInfo = new DrugInfo(drugId, gtin, name, manufacturerId, description, "Active");
        stub.putStringState(key, drugInfo.toJSONString());
        return drugInfo;
    }

    @Transaction()
    public Batch createBatch(final Context ctx, final String batchId, final String drugId, final long quantity, final String description) {
        requireRole(ctx, "hersteller");
        String manufacturerId = getActorIdFromCertificate(ctx);

        ChaincodeStub stub = ctx.getStub();

        DrugInfo drugInfo = getAsset(stub, DRUGINFO_KEY_PREFIX + drugId, DrugInfo.class, PharmacyErrors.DRUGINFO_NOT_FOUND);
        if (!drugInfo.getManufacturerId().equals(manufacturerId)) {
            throw new ChaincodeException("Sie sind nicht der Hersteller dieses Medikaments.", PharmacyErrors.INSUFFICIENT_PERMISSIONS.toString());
        }

        String key = BATCH_KEY_PREFIX + batchId;
        String existingBatch = stub.getStringState(key);
        if (existingBatch != null && !existingBatch.isEmpty()) {
            throw new ChaincodeException("Batch mit der ID " + batchId + " existiert bereits.", PharmacyErrors.BATCH_ALREADY_EXISTS.toString());
        }

        Batch batch = new Batch(batchId, drugId, quantity, manufacturerId, description, new ArrayList<>());
        stub.putStringState(key, batch.toJSONString());

        for (int i = 1; i <= quantity; i++) {
            String unitId = batchId + "-" + i;
            String unitKey = DRUGUNIT_KEY_PREFIX + unitId;

            DrugUnit drugUnit = new DrugUnit.Builder(unitId, batchId, drugId)
                    .owner(manufacturerId)
                    .manufacturerId(manufacturerId)
                    .currentState("Created")
                    .build();
            stub.putStringState(unitKey, drugUnit.toJSONString());
        }

        return batch;
    }

    @Transaction()
    public DrugUnit transferDrugUnit(final Context ctx, final String drugUnitId, final String newOwnerId, final String temperature) {
        ChaincodeStub stub = ctx.getStub();
        String currentOwnerId = getActorIdFromCertificate(ctx);
        String key = DRUGUNIT_KEY_PREFIX + drugUnitId;

        DrugUnit drugUnit = getAsset(stub, key, DrugUnit.class, PharmacyErrors.DRUGUNIT_NOT_FOUND);

        if (!drugUnit.getOwner().equals(currentOwnerId)) {
            throw new ChaincodeException("Nur der aktuelle Besitzer kann die Einheit transferieren.", PharmacyErrors.WRONG_OWNER.toString());
        }

        Actor newOwner = getAsset(stub, ACTOR_KEY_PREFIX + newOwnerId, Actor.class, PharmacyErrors.ACTOR_NOT_FOUND);
        requireApprovedActor(newOwner);

        DrugUnit.Builder builder = new DrugUnit.Builder(drugUnit.getId(), drugUnit.getBatchId(), drugUnit.getDrugId())
                .owner(newOwnerId)
                .manufacturerId(drugUnit.getManufacturerId())
                .description(drugUnit.getDescription())
                .tags(drugUnit.getTags())
                .dispensedBy(drugUnit.getDispensedBy())
                .dispensedTo(drugUnit.getDispensedTo())
                .dispensingTimestamp(drugUnit.getDispensingTimestamp())
                .temperatureReadings(new ArrayList<>(drugUnit.getTemperatureReadings()));

        String currentState = drugUnit.getCurrentState();
        String newOwnerRole = newOwner.getRole();
        if (currentState.equals("Created") && newOwnerRole.equals("grosshaendler")) {
            builder.currentState("InTransit_ToWholesaler");
        } else if (currentState.equals("InTransit_ToWholesaler") && newOwnerRole.equals("apotheke")) {
            builder.currentState("InTransit_ToPharmacy");
        } else if (currentState.equals("InStock_Wholesaler") && newOwnerRole.equals("apotheke")) {
            builder.currentState("InTransit_ToPharmacy");
        } else {
            throw new ChaincodeException("Ungültiger Transfer von " + drugUnit.getOwner() + " (" + currentState + ") an " + newOwnerId + " (" + newOwnerRole + ")", PharmacyErrors.INSUFFICIENT_PERMISSIONS.toString());
        }

        if (temperature != null && !temperature.isEmpty()) {
            String reading = String.format("{\"timestamp\": \"%s\", \"temp\": \"%s\", \"by\": \"%s\"}",
                    stub.getTxTimestamp().toString(), temperature, currentOwnerId);
            builder.addTemperatureReading(reading);
        }

        DrugUnit updatedDrugUnit = builder.build();
        stub.putStringState(key, updatedDrugUnit.toJSONString());
        return updatedDrugUnit;
    }

    @Transaction()
    public DrugUnit dispenseDrugUnit(final Context ctx, final String drugUnitId, final String dispensedTo, final String description) {
        requireRole(ctx, "apotheke");

        ChaincodeStub stub = ctx.getStub();
        String dispenserId = getActorIdFromCertificate(ctx);
        String key = DRUGUNIT_KEY_PREFIX + drugUnitId;

        DrugUnit drugUnit = getAsset(stub, key, DrugUnit.class, PharmacyErrors.DRUGUNIT_NOT_FOUND);

        if (!drugUnit.getOwner().equals(dispenserId)) {
            throw new ChaincodeException("Nur der aktuelle Besitzer (Apotheke) kann die Einheit ausgeben.", PharmacyErrors.WRONG_OWNER.toString());
        }

        DrugUnit dispensedDrugUnit = new DrugUnit.Builder(drugUnit.getId(), drugUnit.getBatchId(), drugUnit.getDrugId())
                .owner(drugUnit.getOwner())
                .manufacturerId(drugUnit.getManufacturerId())
                .description(description)
                .tags(drugUnit.getTags())
                .currentState("Dispensed")
                .dispensedBy(dispenserId)
                .dispensedTo(dispensedTo)
                .dispensingTimestamp(stub.getTxTimestamp().toString())
                .temperatureReadings(drugUnit.getTemperatureReadings())
                .build();

        stub.putStringState(key, dispensedDrugUnit.toJSONString());
        return dispensedDrugUnit;
    }

    /**
     * Ruft die Historie einer Medikamenteneinheit ab (wer war wann Besitzer).
     *
     * '{"function":"getDrugUnitHistory","Args":["BATCH-001-1"]}'
     *
     * @param ctx der Transaktionskontext
     * @param drugUnitId Die ID der Medikamenteneinheit.
     * @return Ein JSON-Array mit der Historie.
     */
    @Transaction()
    public String getDrugUnitHistory(final Context ctx, final String drugUnitId) {
        ChaincodeStub stub = ctx.getStub();
        String key = DRUGUNIT_KEY_PREFIX + drugUnitId;

        final List<String> history = new ArrayList<>();
        try (QueryResultsIterator<KeyModification> results = stub.getHistoryForKey(key)) {
            // KORREKTUR: Der Typ in der for-Schleife ist jetzt 'KeyModification'.
            for (final KeyModification result : results) {
                history.add(result.getStringValue());
            }
        } catch (Exception e) {
            throw new ChaincodeException("Fehler beim Abrufen der Historie: " + e.getMessage());
        }
        return GENSON.serialize(history);
    }

    @Transaction()
    public String getTemperatureHistoryForDrugUnit(final Context ctx, final String drugUnitId) {
        DrugUnit drugUnit = getAsset(ctx.getStub(), DRUGUNIT_KEY_PREFIX + drugUnitId, DrugUnit.class, PharmacyErrors.DRUGUNIT_NOT_FOUND);
        return GENSON.serialize(drugUnit.getTemperatureReadings());
    }

    @Transaction()
    public String queryDrugUnitsByOwner(final Context ctx, final String ownerId) {
        String query = String.format("{\"selector\":{\"owner\":\"%s\", \"_id\":{\"$regex\": \"^%s\"}}}", ownerId, DRUGUNIT_KEY_PREFIX);
        return richQuery(ctx, query);
    }

    // --- Hilfsfunktionen ---

    private void requireRole(final Context ctx, final String requiredRole) {
        String role = ctx.getClientIdentity().getAttributeValue("role");
        if (role == null || !role.equals(requiredRole)) {
            throw new ChaincodeException("Benutzer hat nicht die erforderliche Rolle '" + requiredRole + "'.", PharmacyErrors.INSUFFICIENT_PERMISSIONS.toString());
        }
        requireApprovedActor(ctx);
    }

    private void requireApprovedActor(final Context ctx) {
        String actorId = getActorIdFromCertificate(ctx);
        Actor actor = getAsset(ctx.getStub(), ACTOR_KEY_PREFIX + actorId, Actor.class, PharmacyErrors.ACTOR_NOT_FOUND);
        requireApprovedActor(actor);
    }

    private void requireApprovedActor(final Actor actor) {
        if (!"Approved".equals(actor.getStatus())) {
            throw new ChaincodeException("Akteur " + actor.getActorId() + " ist nicht genehmigt.", PharmacyErrors.ACTOR_NOT_APPROVED.toString());
        }
    }

    private String getActorIdFromCertificate(final Context ctx) {
        String enrollmentId = ctx.getClientIdentity().getAttributeValue("hf.EnrollmentID");
        if (enrollmentId == null || enrollmentId.isEmpty()) {
            throw new ChaincodeException("Attribut 'hf.EnrollmentID' konnte im Zertifikat nicht gefunden werden.");
        }
        return enrollmentId;
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

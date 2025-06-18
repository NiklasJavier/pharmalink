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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Contract(
        name = "PharmacyChaincode",
        info = @Info(
                title = "Pharmacy Supply Chain Contract",
                description = "Ein Prototyp für eine pharmazeutische Lieferkette mit UUIDs",
                version = "2.5.0",
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
        WRONG_OWNER,
        HISTORY_QUERY_FAILED,
        RICH_QUERY_FAILED,
        ASSET_DESERIALIZATION_FAILED,
        UUID_GENERATION_FAILED
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
        String role = getRoleFromCertificate(ctx);
        String enrollmentId = getEnrollmentIdFromCertificate(ctx);
        String txId = stub.getTxId(); // Holen der Transaktions-ID

        String query = String.format("{\"selector\":{\"enrollmentId\":\"%s\"}}", enrollmentId);
        try (QueryResultsIterator<KeyValue> results = stub.getQueryResult(query)) {
            Iterator<KeyValue> iterator = results.iterator();
            if (iterator.hasNext()) {
                throw new ChaincodeException("Ein Akteur mit der Enrollment-ID " + enrollmentId + " existiert bereits.", PharmacyErrors.ACTOR_ALREADY_EXISTS.toString());
            }
        } catch (Exception e) {
            throw new ChaincodeException("Datenbankabfrage zur Existenzprüfung fehlgeschlagen: " + e.getMessage(), PharmacyErrors.RICH_QUERY_FAILED.toString());
        }

        // ANPASSUNG: UUID wird jetzt deterministisch erzeugt statt zufällig.
        String actorId = generateDeterministicUUID(enrollmentId, txId);
        String key = ACTOR_KEY_PREFIX + actorId;
        String status;
        String approvedBy;

        if ("behoerde".equals(role)) {
            status = "Approved";
            approvedBy = actorId;
        } else {
            status = "Pending";
            approvedBy = "";
        }

        Actor actor = new Actor.Builder(actorId, enrollmentId)
                .name(name)
                .mspId(mspId)
                .role(role)
                .status(status)
                .approvedBy(approvedBy)
                .certId(certId)
                .build();

        stub.putStringState(key, actor.toJSONString());

        return actor;
    }

    @Transaction()
    public Actor approveActor(final Context ctx, final String actorIdToApprove) {
        ChaincodeStub stub = ctx.getStub();

        Actor approver = requireApprovedBehoerde(ctx);

        String keyToApprove = ACTOR_KEY_PREFIX + actorIdToApprove;
        Actor actorToApprove = getAsset(stub, keyToApprove, Actor.class, PharmacyErrors.ACTOR_NOT_FOUND);

        if ("Approved".equals(actorToApprove.getStatus())) {
            throw new ChaincodeException("Akteur mit UUID " + actorIdToApprove + " ist bereits genehmigt.", PharmacyErrors.ACTOR_ALREADY_APPROVED.toString());
        }

        Actor approvedActor = new Actor.Builder(actorToApprove.getActorId(), actorToApprove.getEnrollmentId())
                .name(actorToApprove.getName())
                .mspId(actorToApprove.getMspId())
                .role(actorToApprove.getRole())
                .certId(actorToApprove.getCertId())
                .status("Approved")
                .approvedBy(approver.getActorId())
                .build();

        stub.putStringState(keyToApprove, approvedActor.toJSONString());
        return approvedActor;
    }

    @Transaction()
    public String getActorByUUID(final Context ctx, final String actorId) {
        String assetJSON = ctx.getStub().getStringState(ACTOR_KEY_PREFIX + actorId);
        if (assetJSON == null || assetJSON.isEmpty()) {
            throw new ChaincodeException("Akteur mit UUID " + actorId + " nicht gefunden.", PharmacyErrors.ACTOR_NOT_FOUND.toString());
        }
        return assetJSON;
    }

    @Transaction()
    public String queryPendingActors(final Context ctx) {
        requireApprovedBehoerde(ctx);
        String query = String.format("{\"selector\":{\"status\":\"Pending\", \"_id\":{\"$regex\": \"^%s\"}}}", ACTOR_KEY_PREFIX);
        return richQuery(ctx, query);
    }

    @Transaction()
    public String queryAllActors(final Context ctx) {
        requireApprovedBehoerde(ctx);
        String query = String.format("{\"selector\":{\"_id\":{\"$regex\": \"^%s\"}}}", ACTOR_KEY_PREFIX);
        return richQuery(ctx, query);
    }

    @Transaction()
    public DrugInfo createDrugInfo(final Context ctx, final String gtin, final String name, final String description) {
        Actor manufacturer = requireApprovedActorByRole(ctx, "hersteller");

        ChaincodeStub stub = ctx.getStub();
        String drugId = "DRUG-" + gtin;
        String key = DRUGINFO_KEY_PREFIX + drugId;

        String existingDrugInfo = stub.getStringState(key);
        if (existingDrugInfo != null && !existingDrugInfo.isEmpty()) {
            throw new ChaincodeException("DrugInfo mit der ID " + drugId + " existiert bereits.", PharmacyErrors.DRUGINFO_ALREADY_EXISTS.toString());
        }

        DrugInfo drugInfo = new DrugInfo(drugId, gtin, name, manufacturer.getActorId(), description, "Active");
        stub.putStringState(key, drugInfo.toJSONString());
        return drugInfo;
    }

    @Transaction()
    public Batch createBatch(final Context ctx, final String batchId, final String drugId, final long quantity, final String description) {
        Actor manufacturer = requireApprovedActorByRole(ctx, "hersteller");
        String manufacturerId = manufacturer.getActorId();

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
        Actor currentOwner = findActorByEnrollmentId(ctx, getEnrollmentIdFromCertificate(ctx));

        String key = DRUGUNIT_KEY_PREFIX + drugUnitId;
        DrugUnit drugUnit = getAsset(stub, key, DrugUnit.class, PharmacyErrors.DRUGUNIT_NOT_FOUND);

        if (!drugUnit.getOwner().equals(currentOwner.getActorId())) {
            throw new ChaincodeException("Nur der aktuelle Besitzer kann die Einheit transferieren.", PharmacyErrors.WRONG_OWNER.toString());
        }

        Actor newOwner = getAsset(stub, ACTOR_KEY_PREFIX + newOwnerId, Actor.class, PharmacyErrors.ACTOR_NOT_FOUND);
        if (!"Approved".equals(newOwner.getStatus())) {
            throw new ChaincodeException("Der neue Besitzer mit UUID " + newOwnerId + " ist nicht genehmigt.", PharmacyErrors.ACTOR_NOT_APPROVED.toString());
        }

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
            throw new ChaincodeException("Ungültiger Transfer von " + currentOwner.getEnrollmentId() + " an " + newOwner.getEnrollmentId(), PharmacyErrors.INSUFFICIENT_PERMISSIONS.toString());
        }

        if (temperature != null && !temperature.isEmpty()) {
            String reading = String.format("{\"timestamp\": \"%s\", \"temp\": \"%s\", \"by\": \"%s\"}",
                    stub.getTxTimestamp().toString(), temperature, currentOwner.getActorId());
            builder.addTemperatureReading(reading);
        }

        DrugUnit updatedDrugUnit = builder.build();
        stub.putStringState(key, updatedDrugUnit.toJSONString());
        return updatedDrugUnit;
    }

    @Transaction()
    public String getDrugUnitHistory(final Context ctx, final String drugUnitId) {
        ChaincodeStub stub = ctx.getStub();
        String key = DRUGUNIT_KEY_PREFIX + drugUnitId;

        final List<String> history = new ArrayList<>();
        try (QueryResultsIterator<KeyModification> results = stub.getHistoryForKey(key)) {
            for (final KeyModification result : results) {
                history.add(result.getStringValue());
            }
        } catch (Exception e) {
            throw new ChaincodeException("Fehler beim Abrufen der Historie: " + e.getMessage(), PharmacyErrors.HISTORY_QUERY_FAILED.toString());
        }
        return GENSON.serialize(history);
    }

    private Actor requireApprovedBehoerde(final Context ctx) {
        String role = getRoleFromCertificate(ctx);
        if (!"behoerde".equals(role)) {
            throw new ChaincodeException("Benutzer hat nicht die erforderliche Rolle 'behoerde'.", PharmacyErrors.INSUFFICIENT_PERMISSIONS.toString());
        }
        return findAndCheckApprovedActor(ctx);
    }

    private Actor requireApprovedActorByRole(final Context ctx, final String requiredRole) {
        String role = getRoleFromCertificate(ctx);
        if (!requiredRole.equals(role)) {
            throw new ChaincodeException("Benutzer hat nicht die erforderliche Rolle '" + requiredRole + "'.", PharmacyErrors.INSUFFICIENT_PERMISSIONS.toString());
        }
        return findAndCheckApprovedActor(ctx);
    }

    private Actor findAndCheckApprovedActor(final Context ctx) {
        String enrollmentId = getEnrollmentIdFromCertificate(ctx);
        Actor actor = findActorByEnrollmentId(ctx, enrollmentId);
        if (!"Approved".equals(actor.getStatus())) {
            throw new ChaincodeException("Der aufrufende Akteur '" + enrollmentId + "' ist nicht genehmigt.", PharmacyErrors.ACTOR_NOT_APPROVED.toString());
        }
        return actor;
    }

    private Actor findActorByEnrollmentId(final Context ctx, final String enrollmentId) {
        String query = String.format("{\"selector\":{\"enrollmentId\":\"%s\"}}", enrollmentId);
        try (QueryResultsIterator<KeyValue> results = ctx.getStub().getQueryResult(query)) {
            Iterator<KeyValue> iterator = results.iterator();
            if (!iterator.hasNext()) {
                throw new ChaincodeException("Akteur mit Enrollment-ID '" + enrollmentId + "' ist nicht im System registriert.", PharmacyErrors.ACTOR_NOT_FOUND.toString());
            }
            KeyValue result = iterator.next();
            try {
                return GENSON.deserialize(result.getStringValue(), Actor.class);
            } catch (Exception e) {
                throw new ChaincodeException("Fehler beim Deserialisieren von Akteur mit Enrollment-ID " + enrollmentId, PharmacyErrors.ASSET_DESERIALIZATION_FAILED.toString());
            }
        } catch (Exception e) {
            throw new ChaincodeException("Datenbankabfrage für Enrollment-ID '" + enrollmentId + "' fehlgeschlagen: " + e.getMessage(), PharmacyErrors.RICH_QUERY_FAILED.toString());
        }
    }

    private String getEnrollmentIdFromCertificate(final Context ctx) {
        String enrollmentId = ctx.getClientIdentity().getAttributeValue("hf.EnrollmentID");
        if (enrollmentId == null || enrollmentId.isEmpty()) {
            throw new ChaincodeException("Attribut 'hf.EnrollmentID' konnte im Zertifikat nicht gefunden werden.");
        }
        return enrollmentId;
    }

    private String getRoleFromCertificate(final Context ctx) {
        String role = ctx.getClientIdentity().getAttributeValue("role");
        if (role == null || role.isEmpty()) {
            throw new ChaincodeException("Die Rolle des Benutzers ist nicht im Zertifikat gesetzt.", PharmacyErrors.INSUFFICIENT_PERMISSIONS.toString());
        }
        return role;
    }

    private <T> T getAsset(final ChaincodeStub stub, final String key, final Class<T> clazz, final PharmacyErrors error) {
        final String json = stub.getStringState(key);
        if (json == null || json.isEmpty()) {
            throw new ChaincodeException(String.format("Asset mit Schlüssel %s nicht gefunden", key), error.toString());
        }
        try {
            return GENSON.deserialize(json, clazz);
        } catch (Exception e) {
            throw new ChaincodeException("Fehler beim Deserialisieren von Asset " + key, PharmacyErrors.ASSET_DESERIALIZATION_FAILED.toString());
        }
    }

    /**
     * Erzeugt eine deterministische, zeitbasierte UUID (Version 5)
     * aus der Enrollment-ID und der Transaktions-ID, um sicherzustellen,
     * dass alle Peers zum selben Ergebnis kommen.
     */
    private String generateDeterministicUUID(final String enrollmentId, final String txId) {
        try {
            String name = enrollmentId + txId;
            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(nameBytes);

            hash[6] &= 0x0f;
            hash[6] |= 0x50;
            hash[8] &= 0x3f;
            hash[8] |= 0x80;

            return UUID.nameUUIDFromBytes(hash).toString();
        } catch (NoSuchAlgorithmException e) {
            // Dieser Fehler sollte in einer Standard-Java-Umgebung nie auftreten.
            throw new ChaincodeException("Fehler bei der UUID-Generierung: SHA-1 nicht verfügbar.", PharmacyErrors.UUID_GENERATION_FAILED.toString());
        }
    }

    private String richQuery(final Context ctx, final String query) {
        final ChaincodeStub stub = ctx.getStub();
        final StringBuilder sb = new StringBuilder("[");
        try (QueryResultsIterator<KeyValue> results = stub.getQueryResult(query)) {
            for (final KeyValue result : results) {
                if (sb.length() > 1) {
                    sb.append(",");
                }
                sb.append(result.getStringValue());
            }
        } catch (final Exception e) {
            throw new ChaincodeException("Rich Query fehlgeschlagen: " + e.getMessage(), PharmacyErrors.RICH_QUERY_FAILED.toString());
        }
        sb.append("]");
        return sb.toString();
    }
}

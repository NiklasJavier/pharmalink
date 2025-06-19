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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Contract(
        name = "PharmacyChaincode",
        info = @Info(
                title = "Pharmacy Supply-Chain Contract",
                description = "Chaincode zur Nachverfolgung von Medikamenten in einer Lieferkette",
                version = "1.0.5",
                license = @License(
                        name = "Apache-2.0",
                        url = "http://www.apache.org/licenses/LICENSE-2.0"),
                contact = @Contact(
                        email = "j.klein@example.com",
                        name = "JKlein",
                        url = "https://hyperledger.example.com")))
@Default
public final class PharmacyChaincode implements ContractInterface {

    private static final Genson GENSON = new Genson();
    // KORREKTUR: Präfix für den Lookup-Schlüssel, um Rich Queries zu vermeiden
    private static final String ENROLLMENT_ID_LOOKUP_PREFIX = "enrollmentId~";

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void InitLedger(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        String txId = stub.getTxId();
        String mspId = ctx.getClientIdentity().getMSPID();

        System.out.println("========= Initialisiere Ledger mit Test-Akteuren =========");

        Actor behoerde = new Actor.Builder()
                .actorId(generateDeterministicUUID("Actor", txId + "Behoerde"))
                .enrollmentId("behoerde-admin")
                .description("Zulassungsbehörde für Medikamente")
                .mspId(mspId).role("behoerde").status("Approved").approvedBy("self").certId("InitialLedgerSetup")
                .build();
        stub.putStringState(behoerde.getActorId(), behoerde.toJSONString());
        // Erstelle auch den Lookup-Schlüssel für den Init-Akteur
        stub.putStringState(ENROLLMENT_ID_LOOKUP_PREFIX + "behoerde-admin", behoerde.getActorId());
        System.out.println("Asset erstellt: " + behoerde);
    }

    /**
     * KORRIGIERTE VERSION: Verwendet einen direkten Key-Lookup statt einer Rich Query,
     * um Duplikate zuverlässig zu verhindern.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Actor requestActorRegistration(final Context ctx, final String description, final String role) {
        final ChaincodeStub stub = ctx.getStub();
        final String enrollmentId = ctx.getClientIdentity().getId();

        final String certRole = getRoleFromCertificate(ctx);
        if (!certRole.equals(role)) {
            throw new ChaincodeException("Die angeforderte Rolle '" + role + "' stimmt nicht mit der Rolle im Zertifikat ('" + certRole + "') überein.", PharmacyErrors.PERMISSION_DENIED.toString());
        }

        // KORREKTUR: Verwende direkten Lookup-Schlüssel statt Rich Query
        final String lookupKey = ENROLLMENT_ID_LOOKUP_PREFIX + enrollmentId;
        byte[] existingActorId = stub.getState(lookupKey);
        if (existingActorId != null && existingActorId.length > 0) {
            throw new ChaincodeException("Ein Akteur mit der Enrollment ID '" + enrollmentId + "' existiert bereits.", PharmacyErrors.ACTOR_ALREADY_EXISTS.toString());
        }

        final String actorId = generateDeterministicUUID(enrollmentId, ctx.getStub().getTxId());
        final Actor.Builder builder = new Actor.Builder()
                .actorId(actorId)
                .enrollmentId(enrollmentId)
                .description(description)
                .mspId(ctx.getClientIdentity().getMSPID())
                .role(role)
                .certId(ctx.getClientIdentity().getId());

        if (role.equals("behoerde")) {
            builder.status("Approved");
            builder.approvedBy("self");
        } else {
            builder.status("Pending");
        }

        final Actor actor = builder.build();
        stub.putStringState(actor.getActorId(), actor.toJSONString());
        // Speichere den neuen Lookup-Schlüssel
        stub.putStringState(lookupKey, actor.getActorId());

        return actor;
    }

    /**
     * KORRIGIERTE VERSION: Mit zusätzlicher Null-Prüfung zur Vermeidung von Abstürzen.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Actor approveActor(final Context ctx, final String actorIdToApprove) {
        requireRole(ctx, "behoerde");
        final ChaincodeStub stub = ctx.getStub();
        final Actor actorToApprove = getActorById(ctx, actorIdToApprove);
        final Actor approver = queryOwnActor(ctx);

        if (approver == null) {
            throw new ChaincodeException("Der genehmigende Akteur (Behörde) konnte nicht gefunden werden. Ist die Behörde selbst im System registriert?", PharmacyErrors.ACTOR_NOT_FOUND.toString());
        }

        if (!actorToApprove.getStatus().equals("Pending")) {
            throw new ChaincodeException("Akteur hat nicht den Status 'Pending'.", PharmacyErrors.INVALID_ACTOR_STATUS.toString());
        }

        final Actor.Builder builder = new Actor.Builder()
                .actorId(actorToApprove.getActorId()).enrollmentId(actorToApprove.getEnrollmentId())
                .description(actorToApprove.getDescription()).mspId(actorToApprove.getMspId())
                .role(actorToApprove.getRole()).certId(actorToApprove.getCertId()).status("Approved")
                .approvedBy(approver.getActorId());
        final Actor updatedActor = builder.build();
        stub.putStringState(updatedActor.getActorId(), updatedActor.toJSONString());
        return updatedActor;
    }

    /**
     * KORRIGIERTE VERSION: Verwendet einen direkten Key-Lookup statt einer Rich Query.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Actor queryOwnActor(final Context ctx) {
        final String enrollmentId = ctx.getClientIdentity().getId();
        final ChaincodeStub stub = ctx.getStub();

        // KORREKTUR: Verwende direkten Lookup-Schlüssel
        final String lookupKey = ENROLLMENT_ID_LOOKUP_PREFIX + enrollmentId;
        byte[] actorIdBytes = stub.getState(lookupKey);

        if (actorIdBytes == null || actorIdBytes.length == 0) {
            // Versuche Fallback für InitLedger-Akteur mit einfacher ID
            actorIdBytes = stub.getState(ENROLLMENT_ID_LOOKUP_PREFIX + getCnFromIdentity(enrollmentId));
            if (actorIdBytes == null || actorIdBytes.length == 0) {
                throw new ChaincodeException("Kein Akteur für die Enrollment ID '" + enrollmentId + "' gefunden.", PharmacyErrors.ACTOR_NOT_FOUND.toString());
            }
        }

        final String actorId = new String(actorIdBytes, StandardCharsets.UTF_8);
        return getActorById(ctx, actorId);
    }

    // Unveränderte Funktionen folgen...
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugInfo registerDrugInfo(final Context ctx, final String gtin, final String name, final String description) {
        requireRole(ctx, "hersteller");
        final Actor manufacturer = queryOwnActor(ctx);
        requireApproved(manufacturer);
        final DrugInfo drugInfo = new DrugInfo(UUID.randomUUID().toString(), gtin, name, manufacturer.getActorId(), description, "Active");
        ctx.getStub().putStringState(drugInfo.getId(), drugInfo.toJSONString());
        return drugInfo;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Batch createBatchAndDrugUnits(final Context ctx, final String drugId, final int quantity, final String description) {
        requireRole(ctx, "hersteller");
        final ChaincodeStub stub = ctx.getStub();
        final Actor manufacturer = queryOwnActor(ctx);
        requireApproved(manufacturer);
        getDrugInfoById(ctx, drugId);
        final Batch batch = new Batch(UUID.randomUUID().toString(), drugId, quantity, manufacturer.getActorId(), description, List.of("created"));
        stub.putStringState(batch.getId(), batch.toJSONString());
        for (int i = 0; i < quantity; i++) {
            DrugUnit unit = new DrugUnit.Builder().id(UUID.randomUUID().toString()).batchId(batch.getId())
                    .drugId(drugId).owner(manufacturer.getActorId()).manufacturerId(manufacturer.getActorId())
                    .description(description + " - Einheit " + (i + 1)).currentState("MANUFACTURED")
                    .tags(new ArrayList<>()).temperatureReadings(new ArrayList<>()).build();
            stub.putStringState(unit.getId(), unit.toJSONString());
        }
        return batch;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugUnit transferDrugUnit(final Context ctx, final String drugUnitId, final String newOwnerActorId) {
        final ChaincodeStub stub = ctx.getStub();
        final Actor caller = queryOwnActor(ctx);
        requireApproved(caller);
        final DrugUnit drugUnit = getDrugUnitById(ctx, drugUnitId);
        if (!drugUnit.getOwner().equals(caller.getActorId())) {
            throw new ChaincodeException("Der Aufrufer ist nicht der Besitzer der Medikamenteneinheit.", PharmacyErrors.DRUG_UNIT_NOT_OWNED.toString());
        }
        final Actor newOwner = getActorById(ctx, newOwnerActorId);
        requireApproved(newOwner);
        DrugUnit updatedUnit = new DrugUnit.Builder().fromExistingDrugUnit(drugUnit).owner(newOwnerActorId)
                .currentState("IN_TRANSIT").build();
        stub.putStringState(updatedUnit.getId(), updatedUnit.toJSONString());
        return updatedUnit;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugUnit dispenseDrugUnit(final Context ctx, final String drugUnitId, final String patientId) {
        requireRole(ctx, "apotheke");
        final ChaincodeStub stub = ctx.getStub();
        final Actor pharmacy = queryOwnActor(ctx);
        requireApproved(pharmacy);
        final DrugUnit drugUnit = getDrugUnitById(ctx, drugUnitId);
        if (!drugUnit.getOwner().equals(pharmacy.getActorId())) {
            throw new ChaincodeException("Die Apotheke ist nicht der Besitzer der Medikamenteneinheit.", PharmacyErrors.DRUG_UNIT_NOT_OWNED.toString());
        }
        DrugUnit updatedUnit = new DrugUnit.Builder().fromExistingDrugUnit(drugUnit).currentState("DISPENSED")
                .dispensedBy(pharmacy.getActorId()).dispensedTo(patientId)
                .dispensingTimestamp(Instant.now().toString()).build();
        stub.putStringState(updatedUnit.getId(), updatedUnit.toJSONString());
        return updatedUnit;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugUnit addTemperatureReading(final Context ctx, final String drugUnitId, final String temperature) {
        final ChaincodeStub stub = ctx.getStub();
        final Actor caller = queryOwnActor(ctx);
        requireApproved(caller);
        final DrugUnit drugUnit = getDrugUnitById(ctx, drugUnitId);
        if (!drugUnit.getOwner().equals(caller.getActorId())) {
            throw new ChaincodeException("Nur der Besitzer kann Temperaturdaten hinzufügen.", PharmacyErrors.DRUG_UNIT_NOT_OWNED.toString());
        }
        List<String> newReadings = new ArrayList<>(drugUnit.getTemperatureReadings());
        newReadings.add(temperature + " @ " + Instant.now().toString());
        DrugUnit updatedUnit = new DrugUnit.Builder().fromExistingDrugUnit(drugUnit)
                .temperatureReadings(newReadings).build();
        stub.putStringState(updatedUnit.getId(), updatedUnit.toJSONString());
        return updatedUnit;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Actor queryActor(final Context ctx, final String actorId) {
        return getActorById(ctx, actorId);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryDrugUnitHistory(final Context ctx, final String drugUnitId) {
        final ChaincodeStub stub = ctx.getStub();
        final StringBuilder sb = new StringBuilder("[");
        try (QueryResultsIterator<KeyModification> results = stub.getHistoryForKey(drugUnitId)) {
            boolean first = true;
            for (KeyModification result : results) {
                if (!first) {
                    sb.append(",");
                }
                sb.append("{\"txId\":\"").append(result.getTxId()).append("\",");
                sb.append("\"timestamp\":\"").append(result.getTimestamp().toString()).append("\",");
                sb.append("\"isDelete\":").append(result.isDeleted()).append(",");
                sb.append("\"value\":").append(result.getStringValue()).append("}");
                first = false;
            }
        } catch (Exception e) {
            throw new ChaincodeException("Fehler beim Lesen der Historie: " + e.getMessage());
        }
        sb.append("]");
        return sb.toString();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryActorsByRole(final Context ctx, final String role) {
        final String query = String.format("{\"selector\":{\"role\":\"%s\"}}", role);
        return richQuery(ctx, query);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryDrugUnitsByOwner(final Context ctx, final String ownerActorId) {
        final String query = String.format("{\"selector\":{\"owner\":\"%s\"}}", ownerActorId);
        return richQuery(ctx, query);
    }

    private Actor getActorById(final Context ctx, final String actorId) {
        final byte[] state = ctx.getStub().getState(actorId);
        if (state == null || state.length == 0) {
            throw new ChaincodeException("Akteur mit ID '" + actorId + "' nicht gefunden.", PharmacyErrors.ACTOR_NOT_FOUND.toString());
        }
        return Actor.fromJSONString(new String(state, StandardCharsets.UTF_8));
    }

    private DrugInfo getDrugInfoById(final Context ctx, final String drugInfoId) {
        final byte[] state = ctx.getStub().getState(drugInfoId);
        if (state == null || state.length == 0) {
            throw new ChaincodeException("DrugInfo mit ID '" + drugInfoId + "' nicht gefunden.", PharmacyErrors.DRUG_INFO_NOT_FOUND.toString());
        }
        return DrugInfo.fromJSONString(new String(state, StandardCharsets.UTF_8));
    }

    private DrugUnit getDrugUnitById(final Context ctx, final String drugUnitId) {
        final byte[] state = ctx.getStub().getState(drugUnitId);
        if (state == null || state.length == 0) {
            throw new ChaincodeException("DrugUnit mit ID '" + drugUnitId + "' nicht gefunden.", PharmacyErrors.DRUG_UNIT_NOT_FOUND.toString());
        }
        return DrugUnit.fromJSONString(new String(state, StandardCharsets.UTF_8));
    }

    private void requireRole(final Context ctx, final String requiredRole) {
        final String actualRole = getRoleFromCertificate(ctx);
        if (!actualRole.equals(requiredRole)) {
            throw new ChaincodeException("Aufrufer hat nicht die erforderliche Rolle. Benötigt: '" + requiredRole + "', aber Rolle ist: '" + actualRole + "'.", PharmacyErrors.PERMISSION_DENIED.toString());
        }
    }

    private void requireApproved(final Actor actor) {
        if (!actor.getStatus().equals("Approved")) {
            throw new ChaincodeException("Der Akteur '" + actor.getDescription() + "' ist nicht für Aktionen genehmigt.", PharmacyErrors.ACTOR_NOT_APPROVED.toString());
        }
    }

    private String getRoleFromCertificate(final Context ctx) {
        String role = ctx.getClientIdentity().getAttributeValue("role");
        if (role == null || role.isEmpty()) {
            throw new ChaincodeException("Die Rolle des Benutzers ist nicht im Zertifikat gesetzt.", PharmacyErrors.PERMISSION_DENIED.toString());
        }
        return role;
    }

            private String getCnFromIdentity(final String identity) {
        // Example identity: "x509::CN=hersteller-user2, OU=...::CN=ca.org1..."
        // We want to extract "hersteller-user2"
        if (identity.contains("CN=")) {
            String[] parts = identity.split("::");
            if (parts.length > 1 && parts[1].contains("CN=")) {
                String cnPart = parts[1].split(",")[0]; // CN=hersteller-user2
                return cnPart.substring(3); // hersteller-user2
            }
        }
        return identity; // Fallback
    }

    private String generateDeterministicUUID(final String namespace, final String name) {
        try {
            String source = namespace + name;
            byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes);
            return UUID.nameUUIDFromBytes(hash).toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Fehler bei der UUID-Generierung", e);
        }
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
            throw new ChaincodeException("Rich Query fehlgeschlagen: " + e.getMessage(), PharmacyErrors.RICH_QUERY_FAILED.toString());
        }
        sb.append("]");
        return sb.toString();
    }
}

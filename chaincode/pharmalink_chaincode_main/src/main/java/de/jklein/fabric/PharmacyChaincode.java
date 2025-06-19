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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Contract(
        name = "PharmacyChaincode",
        info = @Info(
                title = "Pharmacy Supply-Chain Contract",
                description = "Chaincode zur Nachverfolgung von Medikamenten in einer Lieferkette",
                version = "FINAL",
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

    /**
     * Initialisiert ein leeres Ledger. Akteure müssen manuell registriert werden.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void InitLedger(final Context ctx) {
        System.out.println("========= Leeres Ledger wurde initialisiert =========");
    }


    /**
     * Registriert einen Akteur. Die 'description' wurde entfernt, um das 7-Parameter-Limit einzuhalten.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Actor requestActorRegistration(final Context ctx, final String role) {
        final ChaincodeStub stub = ctx.getStub();
        final String enrollmentId = ctx.getClientIdentity().getId();

        final String certRole = getRoleFromCertificate(ctx);
        if (!certRole.equals(role)) {
            throw new ChaincodeException("Die angeforderte Rolle '" + role + "' stimmt nicht mit der Rolle im Zertifikat ('" + certRole + "') überein.", PharmacyErrors.PERMISSION_DENIED.toString());
        }

        final String query = String.format("{\"selector\":{\"enrollmentId\":\"%s\"}}", enrollmentId);
        final String queryResult = richQuery(ctx, query);
        if (!queryResult.equals("[]")) {
            throw new ChaincodeException("Ein Akteur mit dieser Identität existiert bereits.", PharmacyErrors.ACTOR_ALREADY_EXISTS.toString());
        }

        final String actorId = UUID.randomUUID().toString();
        String status;
        String approvedBy;

        if (role.equals("behoerde")) {
            status = "Approved";
            approvedBy = "self";
        } else {
            status = "Pending";
            approvedBy = "";
        }

        // Ruft den neuen Konstruktor mit 7 Parametern auf
        final Actor actor = new Actor(
                actorId,
                enrollmentId,
                ctx.getClientIdentity().getMSPID(),
                role,
                status,
                approvedBy,
                ctx.getClientIdentity().getId()
        );

        stub.putStringState(actorId, actor.toJSONString());
        return actor;
    }


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Actor approveActor(final Context ctx, final String actorIdToApprove) {
        requireRole(ctx, "behoerde");
        final ChaincodeStub stub = ctx.getStub();
        final Actor actorToApprove = getActorById(ctx, actorIdToApprove);
        final Actor approver = queryOwnActor(ctx);

        if (!actorToApprove.getStatus().equals("Pending")) {
            throw new ChaincodeException("Akteur hat nicht den Status 'Pending'.", PharmacyErrors.INVALID_ACTOR_STATUS.toString());
        }

        // Erstellt das aktualisierte Objekt mit dem neuen 7-Parameter-Konstruktor
        Actor updatedActor = new Actor(
                actorToApprove.getActorId(),
                actorToApprove.getEnrollmentId(),
                actorToApprove.getMspId(),
                actorToApprove.getRole(),
                "Approved", // neuer Status
                approver.getActorId(), // ID des Genehmigers
                actorToApprove.getCertId()
        );

        stub.putStringState(updatedActor.getActorId(), updatedActor.toJSONString());
        return updatedActor;
    }


    /**
     * Findet den eigenen Akteur über eine Rich Query (mit Index).
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Actor queryOwnActor(final Context ctx) {
        final String enrollmentId = ctx.getClientIdentity().getId();
        final String query = String.format("{\"selector\":{\"enrollmentId\":\"%s\"}}", enrollmentId);
        final String queryResultString = richQuery(ctx, query);

        if (queryResultString.equals("[]")) {
            throw new ChaincodeException("Kein Akteur für die Enrollment ID '" + enrollmentId + "' gefunden.", PharmacyErrors.ACTOR_NOT_FOUND.toString());
        }
        try {
            // Die Deserialisierungslogik, die auch mit verschiedenen Genson-Versionen funktioniert
            List<Object> rawList = GENSON.deserialize(queryResultString, List.class);
            if (rawList.isEmpty()) {
                throw new ChaincodeException("Kein Akteur für die Enrollment ID " + enrollmentId + " gefunden.", PharmacyErrors.ACTOR_NOT_FOUND.toString());
            }
            String actorJson = GENSON.serialize(rawList.get(0));
            return Actor.fromJSONString(actorJson);
        } catch (Exception e) {
            throw new ChaincodeException("Fehler beim Parsen des Abfrageergebnisses für queryOwnActor: " + e.getMessage(), PharmacyErrors.RICH_QUERY_FAILED.toString());
        }
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Actor queryActor(final Context ctx, final String actorId) {
        return getActorById(ctx, actorId);
    }

    // ===================================================================================
    //  ANDERE TRANSAKTIONEN & HILFSMETHODEN (unverändert)
    // ===================================================================================

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
            throw new ChaincodeException("Der Akteur ist nicht für Aktionen genehmigt.", PharmacyErrors.ACTOR_NOT_APPROVED.toString());
        }
    }

    private String getRoleFromCertificate(final Context ctx) {
        String role = ctx.getClientIdentity().getAttributeValue("role");
        if (role == null || role.isEmpty()) {
            throw new ChaincodeException("Die Rolle des Benutzers ist nicht im Zertifikat gesetzt.", PharmacyErrors.PERMISSION_DENIED.toString());
        }
        return role;
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

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
                version = "1.0.3",
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
     * Initialisiert das Ledger mit einem Satz von Test-Akteuren.
     * Erstellt eine genehmigte Behörde und drei weitere Akteure mit dem Status "Pending",
     * um den Genehmigungsworkflow testen zu können.
     *
     * @param ctx Der Transaktionskontext.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void InitLedger(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        String txId = stub.getTxId();
        String mspId = ctx.getClientIdentity().getMSPID();

        System.out.println("========= Initialisiere Ledger mit Test-Akteuren =========");

        // 1. Behörde (ist automatisch "Approved")
        Actor behoerde = new Actor.Builder()
                .actorId(generateDeterministicUUID("Actor", txId + "Behoerde"))
                .enrollmentId("behoerde-admin")
                .description("Zulassungsbehörde für Medikamente")
                .mspId(mspId)
                .role("behoerde")
                .status("Approved")
                .approvedBy("self")
                .certId("InitialLedgerSetup")
                .build();
        stub.putStringState(behoerde.getActorId(), behoerde.toJSONString());
        System.out.println("Asset erstellt: " + behoerde);

        // 2. Hersteller (Status "Pending", muss von Behörde genehmigt werden)
        Actor hersteller1 = new Actor.Builder()
                .actorId(generateDeterministicUUID("Actor", txId + "Hersteller1"))
                .enrollmentId("hersteller-user1")
                .description("Bayer AG")
                .mspId(mspId)
                .role("hersteller")
                .status("Pending")
                .approvedBy("")
                .certId("InitialLedgerSetup")
                .build();
        stub.putStringState(hersteller1.getActorId(), hersteller1.toJSONString());
        System.out.println("Asset erstellt: " + hersteller1);


        // 3. Großhändler (Status "Pending")
        Actor grosshaendler1 = new Actor.Builder()
                .actorId(generateDeterministicUUID("Actor", txId + "Grosshaendler1"))
                .enrollmentId("grosshaendler-user1")
                .description("PHOENIX Pharmahandel")
                .mspId(mspId)
                .role("grosshaendler")
                .status("Pending")
                .approvedBy("")
                .certId("InitialLedgerSetup")
                .build();
        stub.putStringState(grosshaendler1.getActorId(), grosshaendler1.toJSONString());
        System.out.println("Asset erstellt: " + grosshaendler1);

        // 4. Apotheke (Status "Pending")
        Actor apotheke1 = new Actor.Builder()
                .actorId(generateDeterministicUUID("Actor", txId + "Apotheke1"))
                .enrollmentId("apotheke-user1")
                .description("Apotheke am Marktplatz")
                .mspId(mspId)
                .role("apotheke")
                .status("Pending")
                .approvedBy("")
                .certId("InitialLedgerSetup")
                .build();
        stub.putStringState(apotheke1.getActorId(), apotheke1.toJSONString());
        System.out.println("Asset erstellt: " + apotheke1);

        System.out.println("========= Initialisierung des Ledgers abgeschlossen =========");
    }

    // ========= Akteur-Management =========

    /**
     * Ein neuer Benutzer registriert sich selbst im Netzwerk mit einer bestimmten Rolle.
     * Beispiel:
     * '{"function":"requestActorRegistration","Args":["Apotheke am Dom","apotheke"]}'
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Actor requestActorRegistration(final Context ctx, final String description, final String role) {
        final ChaincodeStub stub = ctx.getStub();
        final String enrollmentId = ctx.getClientIdentity().getId();

        final String query = String.format("{\"selector\":{\"enrollmentId\":\"%s\"}}", enrollmentId);
        final String existingActors = richQuery(ctx, query);
        if (!existingActors.equals("[]")) {
            throw new ChaincodeException("Ein Akteur mit der Enrollment ID '" + enrollmentId + "' existiert bereits.", PharmacyErrors.ACTOR_ALREADY_EXISTS.toString());
        }

        final Actor actor = new Actor.Builder()
                .actorId(UUID.randomUUID().toString())
                .enrollmentId(enrollmentId)
                .description(description)
                .mspId(ctx.getClientIdentity().getMSPID())
                .role(role)
                .status("Pending")
                .certId(ctx.getClientIdentity().getId())
                .build();

        stub.putStringState(actor.getActorId(), actor.toJSONString());
        return actor;
    }

    /**
     * Genehmigt die Registrierung eines neuen Akteurs. Nur für 'behoerde' erlaubt.
     * Beispiel:
     * '{"function":"approveActor","Args":["a1b2c3d4-e5f6-7890-1234-567890abcdef"]}'
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Actor approveActor(final Context ctx, final String actorIdToApprove) {
        requireRole(ctx, "behoerde");

        final ChaincodeStub stub = ctx.getStub();
        final Actor actorToApprove = getActorById(ctx, actorIdToApprove);
        final Actor approver = queryOwnActor(ctx);

        if (!actorToApprove.getStatus().equals("Pending")) {
            throw new ChaincodeException("Akteur hat nicht den Status 'Pending'.", PharmacyErrors.INVALID_ACTOR_STATUS.toString());
        }

        final Actor.Builder builder = new Actor.Builder()
                .actorId(actorToApprove.getActorId())
                .enrollmentId(actorToApprove.getEnrollmentId())
                .description(actorToApprove.getDescription())
                .mspId(actorToApprove.getMspId())
                .role(actorToApprove.getRole())
                .certId(actorToApprove.getCertId())
                .status("Approved")
                .approvedBy(approver.getActorId());

        final Actor updatedActor = builder.build();
        stub.putStringState(updatedActor.getActorId(), updatedActor.toJSONString());

        return updatedActor;
    }

    // ========= Stammdaten-Management (DrugInfo) =========

    /**
     * Registriert die Stammdaten eines neuen Medikaments. Nur für 'hersteller' erlaubt.
     * Beispiel:
     * '{"function":"registerDrugInfo","Args":["GTIN-12345","Aspirin 500mg","Kopfschmerztabletten"]}'
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugInfo registerDrugInfo(final Context ctx, final String gtin, final String name, final String description) {
        requireRole(ctx, "hersteller");
        final Actor manufacturer = queryOwnActor(ctx);
        requireApproved(manufacturer);

        final DrugInfo drugInfo = new DrugInfo(UUID.randomUUID().toString(), gtin, name, manufacturer.getActorId(), description, "Active");

        ctx.getStub().putStringState(drugInfo.getId(), drugInfo.toJSONString());
        return drugInfo;
    }

    // ========= Chargen- und Einheiten-Management =========

    /**
     * Erstellt eine neue Charge und die zugehörigen, einzelnen Medikamenten-Einheiten. Nur für 'hersteller'.
     * Beispiel:
     * '{"function":"createBatchAndDrugUnits","Args":["drug-info-uuid-123",100,"Sommerproduktion 2025"]}'
     */
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
            DrugUnit unit = new DrugUnit.Builder()
                    .id(UUID.randomUUID().toString())
                    .batchId(batch.getId())
                    .drugId(drugId)
                    .owner(manufacturer.getActorId())
                    .manufacturerId(manufacturer.getActorId())
                    .description(description + " - Einheit " + (i + 1))
                    .currentState("MANUFACTURED")
                    .tags(new ArrayList<>())
                    .temperatureReadings(new ArrayList<>())
                    .build();
            stub.putStringState(unit.getId(), unit.toJSONString());
        }
        return batch;
    }

    // ========= Lieferketten-Transaktionen =========

    /**
     * Überträgt eine Medikamenteneinheit an einen neuen Besitzer.
     * Beispiel:
     * '{"function":"transferDrugUnit","Args":["drug-unit-uuid-abc","new-owner-actor-id-xyz"]}'
     */
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

        DrugUnit updatedUnit = new DrugUnit.Builder()
                .fromExistingDrugUnit(drugUnit)
                .owner(newOwnerActorId)
                .currentState("IN_TRANSIT")
                .build();

        stub.putStringState(updatedUnit.getId(), updatedUnit.toJSONString());
        return updatedUnit;
    }

    /**
     * Gibt eine Medikamenteneinheit an einen Patienten aus. Nur für 'apotheke' erlaubt.
     * Beispiel:
     * '{"function":"dispenseDrugUnit","Args":["drug-unit-uuid-abc","Patient-XYZ-Hash"]}'
     */
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

        DrugUnit updatedUnit = new DrugUnit.Builder()
                .fromExistingDrugUnit(drugUnit)
                .currentState("DISPENSED")
                .dispensedBy(pharmacy.getActorId())
                .dispensedTo(patientId)
                .dispensingTimestamp(Instant.now().toString())
                .build();

        stub.putStringState(updatedUnit.getId(), updatedUnit.toJSONString());
        return updatedUnit;
    }

    /**
     * Fügt einen Temperaturmesswert zu einer Medikamenteneinheit hinzu (Besonderheit).
     * Beispiel:
     * '{"function":"addTemperatureReading","Args":["drug-unit-uuid-abc","5.2C"]}'
     */
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

        DrugUnit updatedUnit = new DrugUnit.Builder()
                .fromExistingDrugUnit(drugUnit)
                .temperatureReadings(newReadings)
                .build();

        stub.putStringState(updatedUnit.getId(), updatedUnit.toJSONString());
        return updatedUnit;
    }

    // ========= Abfrage-Funktionen (Queries) =========

    /**
     * Liest die Daten eines beliebigen Akteurs.
     * Beispiel:
     * '{"function":"queryActor","Args":["actor-uuid-123"]}'
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Actor queryActor(final Context ctx, final String actorId) {
        return getActorById(ctx, actorId);
    }

    /**
     * Liest die eigenen Akteursdaten des Aufrufers.
     * Beispiel:
     * '{"function":"queryOwnActor","Args":[]}'
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Actor queryOwnActor(final Context ctx) {
        final String enrollmentId = ctx.getClientIdentity().getId();
        final String query = String.format("{\"selector\":{\"enrollmentId\":\"%s\"}}", enrollmentId);
        final String queryResultString = richQuery(ctx, query);

        if (queryResultString.equals("[]")) {
            throw new ChaincodeException("Kein Akteur für die Enrollment ID " + enrollmentId + " gefunden.", PharmacyErrors.ACTOR_NOT_FOUND.toString());
        }

        try {
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

    /**
     * Liest die Daten einer Medikamenteneinheit.
     * Beispiel:
     * '{"function":"queryDrugUnit","Args":["drug-unit-uuid-abc"]}'
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public DrugUnit queryDrugUnit(final Context ctx, final String drugUnitId) {
        return getDrugUnitById(ctx, drugUnitId);
    }

    /**
     * Liest die Transaktionshistorie einer Medikamenteneinheit.
     * Beispiel:
     * '{"function":"queryDrugUnitHistory","Args":["drug-unit-uuid-abc"]}'
     */
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

    /**
     * Findet alle Akteure mit einer bestimmten Rolle.
     * Beispiel:
     * '{"function":"queryActorsByRole","Args":["apotheke"]}'
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryActorsByRole(final Context ctx, final String role) {
        final String query = String.format("{\"selector\":{\"role\":\"%s\"}}", role);
        return richQuery(ctx, query);
    }

    /**
     * Findet alle Medikamenteneinheiten eines bestimmten Besitzers.
     * Beispiel:
     * '{"function":"queryDrugUnitsByOwner","Args":["owner-actor-id-xyz"]}'
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryDrugUnitsByOwner(final Context ctx, final String ownerActorId) {
        final String query = String.format("{\"selector\":{\"owner\":\"%s\"}}", ownerActorId);
        return richQuery(ctx, query);
    }


    // ========= Private Hilfsmethoden =========

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
            throw new ChaincodeException("Die Rolle des Benutzers ist nicht im Zertifikat gesetzt. Dies ist für die Autorisierung erforderlich.", PharmacyErrors.PERMISSION_DENIED.toString());
        }
        return role;
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

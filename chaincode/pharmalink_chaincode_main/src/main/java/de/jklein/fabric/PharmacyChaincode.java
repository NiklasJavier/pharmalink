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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Contract(
        name = "PharmacyChaincode",
        info = @Info(
                title = "Pharmacy Supply Chain Contract",
                description = "Ein Prototyp für eine pharmazeutische Lieferkette auf Hyperledger Fabric.",
                version = "1.0.0",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "j.klein@example.com",
                        name = "Jonas Klein",
                        url = "https://example.com")))
@Default
public final class PharmacyChaincode implements ContractInterface {

    private final Genson genson = new Genson();

    private enum PharmacyErrors {
        ACTOR_NOT_FOUND,
        ACTOR_ALREADY_EXISTS,
        DRUG_INFO_NOT_FOUND,
        DRUG_INFO_ALREADY_EXISTS,
        BATCH_NOT_FOUND,
        BATCH_ALREADY_EXISTS,
        DRUG_UNIT_NOT_FOUND,
        DRUG_UNIT_ALREADY_EXISTS,
        PERMISSION_DENIED,
        INVALID_ARGUMENT,
        UUID_GENERATION_FAILED,
        RICH_QUERY_FAILED,
        INVALID_JSON
    }

    /**
     * Initialisiert das Ledger mit Beispiel-Akteuren, Medikamenteninformationen, Chargen und Medikamenteneinheiten.
     * Dies ist nützlich für Entwicklungs- und Testzwecke.
     *
     * @param ctx Der Transaktionskontext.
     */
    // Beispielausführung: {"function":"initLedger","Args":[]}
    @Transaction()
    public void initLedger(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        String txId = stub.getTxId();

        // Beispiel-Akteure
        // Behörde
        // Der behördeAdmin wird weiterhin mit einer festen ID erstellt,
        // da er der "System"-Akteur ist, der andere Akteure genehmigt.
        // Seine certId sollte später mit der tatsächlichen Admin-Zertifikat-ID übereinstimmen.
        String behordeCertId = "CN=behoerdeAdmin::" + ctx.getClientIdentity().getMSPID(); // Beispiel CertId für Admin
        Actor behorde = new Actor.Builder()
                .actorId("behoerdeAdmin") // Feste ID für den System-Admin
                .enrollmentId("behoerdeAdmin") // Feste Enrollment ID für den System-Admin
                .name("Zentrale Arzneimittelbehörde")
                .mspId("BehoerdeMSP") // Annahme: MSPID für Behörde
                .role("behoerde")
                .status("approved")
                .approvedBy("system")
                .certId(behordeCertId)
                .build();
        stub.putState(behorde.getActorId(), behorde.toJSONString().getBytes(StandardCharsets.UTF_8));

        // Hersteller
        // Für reale Akteure wird die actorId nun als UUID generiert.
        // Die CertId sollte dem Common Name des registrierten Benutzers entsprechen (z.B. "CN=hersteller-user1::Org1MSP")
        String hersteller1EnrollmentId = "hersteller-user1"; // Dies ist die EnrollmentID
        String hersteller1CertId = "CN=" + hersteller1EnrollmentId + "::HerstellerMSP"; // Beispiel CertId
        String hersteller1ActorId = generateDeterministicUUID(hersteller1EnrollmentId, txId + "hersteller1Seed");
        Actor hersteller1 = new Actor.Builder()
                .actorId(hersteller1ActorId)
                .enrollmentId(hersteller1EnrollmentId)
                .name("PharmaCorp GmbH")
                .mspId("HerstellerMSP") // Annahme: MSPID für Hersteller
                .role("hersteller")
                .status("approved")
                .approvedBy(behorde.getActorId())
                .certId(hersteller1CertId)
                .build();
        stub.putState(hersteller1.getActorId(), hersteller1.toJSONString().getBytes(StandardCharsets.UTF_8));

        // Großhändler
        String grosshaendler1EnrollmentId = "grosshaendler-user1";
        String grosshaendler1CertId = "CN=" + grosshaendler1EnrollmentId + "::GrosshaendlerMSP"; // Beispiel CertId
        String grosshaendler1ActorId = generateDeterministicUUID(grosshaendler1EnrollmentId, txId + "grosshaendler1Seed");
        Actor grosshaendler1 = new Actor.Builder()
                .actorId(grosshaendler1ActorId)
                .enrollmentId(grosshaendler1EnrollmentId)
                .name("MediDistributor AG")
                .mspId("GrosshaendlerMSP") // Annahme: MSPID für Großhändler
                .role("grosshaendler")
                .status("approved")
                .approvedBy(behorde.getActorId())
                .certId(grosshaendler1CertId)
                .build();
        stub.putState(grosshaendler1.getActorId(), grosshaendler1.toJSONString().getBytes(StandardCharsets.UTF_8));

        // Apotheke
        String apotheke1EnrollmentId = "apotheke-user1";
        String apotheke1CertId = "CN=" + apotheke1EnrollmentId + "::ApothekeMSP"; // Beispiel CertId
        String apotheke1ActorId = generateDeterministicUUID(apotheke1EnrollmentId, txId + "apotheke1Seed");
        Actor apotheke1 = new Actor.Builder()
                .actorId(apotheke1ActorId)
                .enrollmentId(apotheke1EnrollmentId)
                .name("Stadt Apotheke")
                .mspId("ApothekeMSP") // Annahme: MSPID für Apotheke
                .role("apotheke")
                .status("approved")
                .approvedBy(behorde.getActorId())
                .certId(apotheke1CertId)
                .build();
        stub.putState(apotheke1.getActorId(), apotheke1.toJSONString().getBytes(StandardCharsets.UTF_8));


        // Beispiel-Medikamenteninformationen
        // ID wird vom Chaincode generiert
        String drug1Id = generateDeterministicUUID(hersteller1.getActorId(), txId + "Paracetamol500mg");
        DrugInfo drug1 = new DrugInfo(drug1Id, "01234567890128", "Paracetamol 500mg", hersteller1.getActorId(), "Ein klassisches Schmerzmittel", "active");
        stub.putState(drug1.getId(), drug1.toJSONString().getBytes(StandardCharsets.UTF_8));

        String drug2Id = generateDeterministicUUID(hersteller1.getActorId(), txId + "Ibuprofen400mg");
        DrugInfo drug2 = new DrugInfo(drug2Id, "01234567890135", "Ibuprofen 400mg", hersteller1.getActorId(), "Entzündungshemmendes Medikament", "active");
        stub.putState(drug2.getId(), drug2.toJSONString().getBytes(StandardCharsets.UTF_8));

        // Beispiel-Chargen
        // ID wird vom Chaincode generiert
        String batch1Id = generateDeterministicUUID(hersteller1.getActorId(), txId + drug1.getId() + "Q12023");
        Batch batch1 = new Batch(batch1Id, drug1.getId(), 1000, hersteller1.getActorId(), "Produktionscharge Q1 2023 Paracetamol", List.of("analgetisch", "rezeptfrei"));
        stub.putState(batch1.getId(), batch1.toJSONString().getBytes(StandardCharsets.UTF_8));

        String batch2Id = generateDeterministicUUID(hersteller1.getActorId(), txId + drug2.getId() + "Q22023");
        Batch batch2 = new Batch(batch2Id, drug2.getId(), 500, hersteller1.getActorId(), "Produktionscharge Q2 2023 Ibuprofen", List.of("entzündungshemmend"));
        stub.putState(batch2.getId(), batch2.toJSONString().getBytes(StandardCharsets.UTF_8));

        // Beispiel-Medikamenteneinheiten
        // ID wird vom Chaincode generiert
        String drugUnit1Id = generateDeterministicUUID(hersteller1.getActorId(), txId + batch1.getId() + "Unit_A");
        DrugUnit drugUnit1 = new DrugUnit.Builder()
                .id(drugUnit1Id)
                .batchId(batch1.getId())
                .drugId(drug1.getId())
                .owner(hersteller1.getActorId())
                .manufacturerId(hersteller1.getActorId())
                .description("Einzelpackung Paracetamol - Box A")
                .tags(List.of("klein", "originalverpackt"))
                .currentState("manufactured")
                .build();
        stub.putState(drugUnit1.getId(), drugUnit1.toJSONString().getBytes(StandardCharsets.UTF_8));

        String drugUnit2Id = generateDeterministicUUID(hersteller1.getActorId(), txId + batch1.getId() + "Unit_B");
        DrugUnit drugUnit2 = new DrugUnit.Builder()
                .id(drugUnit2Id)
                .batchId(batch1.getId())
                .drugId(drug1.getId())
                .owner(hersteller1.getActorId())
                .manufacturerId(hersteller1.getActorId())
                .description("Einzelpackung Paracetamol - Box B")
                .tags(List.of("klein", "originalverpackt"))
                .currentState("manufactured")
                .build();
        stub.putState(drugUnit2.getId(), drugUnit2.toJSONString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Registriert einen neuen Akteur im Ledger.
     * Die actorId wird automatisch generiert. Der Status ist initial 'pending', es sei denn, die Rolle ist 'behoerde', dann 'approved'.
     * Die weiteren Informationen wie mspId, certId und approvedBy werden automatisch gesetzt.
     *
     * @param ctx Der Transaktionskontext.
     * @param name Der Alias/Name des Akteurs.
     * @param role Die Rolle des Akteurs (z.B. "hersteller", "grosshaendler", "apotheke", "behoerde").
     * @return Der neu erstellte Akteur als JSON-String.
     */
    // Beispielausführung (für einen neuen Hersteller):
    // {"function":"registerActor","Args":["Neue Pharma GmbH", "hersteller"]}
    // Beispielausführung (für eine neue Behörde):
    // {"function":"registerActor","Args":["Bundesamt für Arzneimittelsicherheit", "behoerde"]}
    @Transaction()
    public String registerActor(final Context ctx, final String name, final String role) { // Parameter angepasst
        ChaincodeStub stub = ctx.getStub();
        String currentMspId = ctx.getClientIdentity().getMSPID(); // MSPID des aufrufenden Clients
        String certId = ctx.getClientIdentity().getId(); // Zertifikat-ID des aufrufenden Clients
        String enrollmentId = getEnrollmentIdFromCertId(certId); // EnrollmentID aus der CertId ableiten
        String txId = stub.getTxId();

        // Prüfen, ob Akteur mit dieser Enrollment-ID bereits existiert
        String query = String.format("{\"selector\":{\"enrollmentId\":\"%s\"}}", enrollmentId);
        try (QueryResultsIterator<KeyValue> results = stub.getQueryResult(query)) {
            Iterator<KeyValue> iterator = results.iterator();
            if (iterator.hasNext()) {
                // Ein Akteur mit dieser Enrollment-ID ist bereits registriert.
                // Wir können hier den existierenden Akteur zurückgeben oder einen Fehler werfen.
                // Da "registrieren" einen neuen Vorgang impliziert, werfen wir einen Fehler.
                throw new ChaincodeException(String.format("Ein Akteur mit der Identität '%s' (Enrollment-ID) ist bereits registriert.", enrollmentId), PharmacyErrors.ACTOR_ALREADY_EXISTS.toString());
            }
        } catch (Exception e) {
            throw new ChaincodeException("Fehler bei der Abfrage der bestehenden Akteure: " + e.getMessage(), PharmacyErrors.RICH_QUERY_FAILED.toString());
        }

        String actorId = generateDeterministicUUID(enrollmentId, txId); // Generiere die definitive ActorId

        String status;
        String approvedBy = ""; // Standardmäßig leer, muss von Behörde gesetzt werden

        // Setze Status und approvedBy basierend auf der Rolle
        if ("behoerde".equals(role)) {
            status = "approved";
            // Eine Behörde genehmigt sich selbst bei der Registrierung
            approvedBy = actorId; // Selbstgenehmigung
        } else {
            status = "pending";
            // Ein normaler Akteur muss von einer Behörde genehmigt werden, initial leer
        }

        Actor newActor = new Actor.Builder()
                .actorId(actorId)
                .enrollmentId(enrollmentId) // Speichere die abgeleitete Enrollment ID
                .name(name)
                .mspId(currentMspId)
                .role(role)
                .status(status)
                .approvedBy(approvedBy)
                .certId(certId) // Die CertId des aufrufenden Clients wird gespeichert
                .build();

        stub.putState(actorId, newActor.toJSONString().getBytes(StandardCharsets.UTF_8));
        return newActor.toJSONString();
    }

    /**
     * Genehmigt einen ausstehenden Akteur.
     * Nur ein Akteur mit der Rolle 'behoerde' kann einen Akteur genehmigen.
     *
     * @param ctx Der Transaktionskontext.
     * @param actorId Die ID des zu genehmigenden Akteurs.
     * @return Der genehmigte Akteur als JSON-String.
     */
    // Beispielausführung (als Behörde): {"function":"approveActor","Args":["UUID-des-neuen-Akteurs"]}
    @Transaction()
    public String approveActor(final Context ctx, final String actorId) {
        ChaincodeStub stub = ctx.getStub();
        Actor invoker = getCurrentActor(ctx); // Der Aufrufer muss eine genehmigte Behörde sein

        if (!"behoerde".equals(invoker.getRole())) {
            throw new ChaincodeException("Nur die Behörde kann Akteure genehmigen.", PharmacyErrors.PERMISSION_DENIED.toString());
        }

        byte[] actorState = stub.getState(actorId);
        if (actorState == null || actorState.length == 0) {
            throw new ChaincodeException(String.format("Akteur mit ID %s nicht gefunden.", actorId), PharmacyErrors.ACTOR_NOT_FOUND.toString());
        }

        Actor actorToApprove = genson.deserialize(new String(actorState, StandardCharsets.UTF_8), Actor.class);

        if (!"pending".equals(actorToApprove.getStatus())) {
            throw new ChaincodeException(String.format("Akteur mit ID %s hat nicht den Status 'pending'. Genehmigung nicht möglich.", actorId), PharmacyErrors.INVALID_ARGUMENT.toString());
        }

        Actor approvedActor = new Actor.Builder()
                .actorId(actorToApprove.getActorId())
                .enrollmentId(actorToApprove.getEnrollmentId()) // enrollmentId beibehalten
                .name(actorToApprove.getName())
                .mspId(actorToApprove.getMspId())
                .role(actorToApprove.getRole())
                .status("approved")
                .approvedBy(invoker.getActorId()) // Genehmigt durch den aufrufenden Behörden-Akteur
                .certId(actorToApprove.getCertId())
                .build();

        stub.putState(actorId, approvedActor.toJSONString().getBytes(StandardCharsets.UTF_8));
        return approvedActor.toJSONString();
    }


    /**
     * Liest die Details eines Akteurs aus dem Ledger.
     * Jeder genehmigte Akteur kann die Details anderer Akteure lesen.
     *
     * @param ctx Der Transaktionskontext.
     * @param actorId Die ID des zu lesenden Akteurs.
     * @return Der Akteur als JSON-String.
     */
    // Beispielausführung: {"function":"readActor","Args":["UUID-des-Herstellers"]}
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String readActor(final Context ctx, final String actorId) {
        ChaincodeStub stub = ctx.getStub();
        getCurrentActor(ctx); // Autorisierungsprüfung

        byte[] actorState = stub.getState(actorId);

        if (actorState == null || actorState.length == 0) {
            throw new ChaincodeException(String.format("Akteur mit ID %s nicht gefunden.", actorId), PharmacyErrors.ACTOR_NOT_FOUND.toString());
        }

        return new String(actorState, StandardCharsets.UTF_8);
    }

    /**
     * Überprüft, ob ein Akteur mit der gegebenen ID existiert.
     *
     * @param ctx Der Transaktionskontext.
     * @param actorId Die ID des Akteurs.
     * @return True, wenn der Akteur existiert, sonst False.
     */
    // Beispielausführung: {"function":"actorExists","Args":["UUID-des-Herstellers"]}
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean actorExists(final Context ctx, final String actorId) {
        ChaincodeStub stub = ctx.getStub();
        return stub.getState(actorId).length > 0;
    }

    /**
     * Erstellt eine neue Medikamenteninformation im Ledger.
     * Nur ein Akteur mit der Rolle 'hersteller' kann Medikamenteninformationen erstellen.
     * Die ID der DrugInfo wird automatisch generiert.
     * Die erstellte DrugInfo erhält den Status "active".
     *
     * @param ctx Der Transaktionskontext.
     * @param gtin Die GTIN des Medikaments.
     * @param name Der Name des Medikaments.
     * @param description Die Beschreibung des Medikaments (Alias).
     * @return Die neu erstellte DrugInfo als JSON-String.
     */
    // Beispielausführung (als Hersteller): {"function":"createDrugInfo","Args":["01234567890142", "Aspirin", "Kopfschmerztablette 500mg"]}
    @Transaction()
    public String createDrugInfo(final Context ctx, final String gtin, final String name, final String description) {
        ChaincodeStub stub = ctx.getStub();
        Actor invoker = getCurrentActor(ctx);

        if (!"hersteller".equals(invoker.getRole())) {
            throw new ChaincodeException("Nur Hersteller können Medikamenteninformationen erstellen.", PharmacyErrors.PERMISSION_DENIED.toString());
        }

        String drugInfoId = generateDeterministicUUID(invoker.getActorId(), stub.getTxId() + gtin);

        // Obwohl unwahrscheinlich mit UUIDs, ist diese Prüfung wichtig, falls die Determinismus-Quelle nicht perfekt ist.
        if (drugInfoExists(ctx, drugInfoId)) {
            throw new ChaincodeException(String.format("Medikamenteninformation mit generierter ID %s existiert bereits. Dies sollte nicht passieren.", drugInfoId), PharmacyErrors.DRUG_INFO_ALREADY_EXISTS.toString());
        }

        DrugInfo drugInfo = new DrugInfo(drugInfoId, gtin, name, invoker.getActorId(), description, "active");
        stub.putState(drugInfo.getId(), drugInfo.toJSONString().getBytes(StandardCharsets.UTF_8));
        return drugInfo.toJSONString();
    }

    /**
     * Aktualisiert eine bestehende Medikamenteninformation im Ledger.
     * Nur der ursprüngliche Hersteller kann seine eigenen Medikamenteninformationen aktualisieren.
     *
     * @param ctx Der Transaktionskontext.
     * @param id Die ID der zu aktualisierenden Medikamenteninformation.
     * @param gtin Die aktualisierte GTIN.
     * @param name Der aktualisierte Name.
     * @param description Die aktualisierte Beschreibung (Alias).
     * @param status Der aktualisierte Status.
     * @return Die aktualisierte DrugInfo als JSON-String.
     */
    // Beispielausführung (als Hersteller): {"function":"updateDrugInfo","Args":["UUID-der-DrugInfo", "01234567890143", "Aspirin Forte", "Stärkere Kopfschmerztablette", "active"]}
    @Transaction()
    public String updateDrugInfo(final Context ctx, final String id, final String gtin, final String name, final String description, final String status) {
        ChaincodeStub stub = ctx.getStub();
        Actor invoker = getCurrentActor(ctx);

        byte[] drugInfoState = stub.getState(id);
        if (drugInfoState == null || drugInfoState.length == 0) {
            throw new ChaincodeException(String.format("Medikamenteninformation mit ID %s nicht gefunden.", id), PharmacyErrors.DRUG_INFO_NOT_FOUND.toString());
        }

        DrugInfo existingDrugInfo = genson.deserialize(new String(drugInfoState, StandardCharsets.UTF_8), DrugInfo.class);

        if (!"hersteller".equals(invoker.getRole()) || !invoker.getActorId().equals(existingDrugInfo.getManufacturerId())) {
            throw new ChaincodeException("Nur der Hersteller kann seine Medikamenteninformationen aktualisieren.", PharmacyErrors.PERMISSION_DENIED.toString());
        }

        // Konstruktor hat 6 Parameter, was konform ist.
        DrugInfo updatedDrugInfo = new DrugInfo(id, gtin, name, existingDrugInfo.getManufacturerId(), description, status);
        stub.putState(id, updatedDrugInfo.toJSONString().getBytes(StandardCharsets.UTF_8));
        return updatedDrugInfo.toJSONString();
    }

    /**
     * Liest die Details einer Medikamenteninformation aus dem Ledger.
     * Jeder genehmigte Akteur kann Medikamenteninformationen lesen.
     *
     * @param ctx Der Transaktionskontext.
     * @param id Die ID der Medikamenteninformation.
     * @return Die DrugInfo als JSON-String.
     */
    // Beispielausführung: {"function":"readDrugInfo","Args":["UUID-der-DrugInfo"]}
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String readDrugInfo(final Context ctx, final String id) {
        ChaincodeStub stub = ctx.getStub();
        getCurrentActor(ctx); // Autorisierungsprüfung

        byte[] drugInfoState = stub.getState(id);
        if (drugInfoState == null || drugInfoState.length == 0) {
            throw new ChaincodeException(String.format("Medikamenteninformation mit ID %s nicht gefunden.", id), PharmacyErrors.DRUG_INFO_NOT_FOUND.toString());
        }
        return new String(drugInfoState, StandardCharsets.UTF_8);
    }

    /**
     * Überprüft, ob eine Medikamenteninformation mit der gegebenen ID existiert.
     *
     * @param ctx Der Transaktionskontext.
     * @param id Die ID der Medikamenteninformation.
     * @return True, wenn die Medikamenteninformation existiert, sonst False.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean drugInfoExists(final Context ctx, final String id) {
        ChaincodeStub stub = ctx.getStub();
        return stub.getState(id).length > 0;
    }

    /**
     * Erstellt eine neue Charge eines Medikaments.
     * Nur ein Akteur mit der Rolle 'hersteller' kann Chargen erstellen,
     * und nur für Medikamenteninformationen, die sie selbst hergestellt haben.
     * Die ID der Charge wird automatisch generiert.
     *
     * @param ctx Der Transaktionskontext.
     * @param drugId Die ID der zugehörigen Medikamenteninformation.
     * @param quantity Die Menge der Medikamente in dieser Charge.
     * @param description Eine Beschreibung der Charge (Alias).
     * @param tagsJson Ein JSON-String, der eine Liste von Tags darstellt (z.B. "[\"tag1\", \"tag2\"]").
     * @return Die neu erstellte Batch als JSON-String.
     */
    // Beispielausführung (als Hersteller): {"function":"createBatch","Args":["UUID-der-DrugInfo", "500", "Neue Kleinpackung", "[\"schnellelieferung\"]"]}
    @Transaction()
    public String createBatch(final Context ctx, final String drugId, final long quantity, final String description, final String tagsJson) {
        ChaincodeStub stub = ctx.getStub();
        Actor invoker = getCurrentActor(ctx);

        if (!"hersteller".equals(invoker.getRole())) {
            throw new ChaincodeException("Nur Hersteller können Chargen erstellen.", PharmacyErrors.PERMISSION_DENIED.toString());
        }

        byte[] drugInfoState = stub.getState(drugId);
        if (drugInfoState == null || drugInfoState.length == 0) {
            throw new ChaincodeException(String.format("Medikamenteninformation mit ID %s nicht gefunden.", drugId), PharmacyErrors.DRUG_INFO_NOT_FOUND.toString());
        }
        DrugInfo drugInfo = genson.deserialize(new String(drugInfoState, StandardCharsets.UTF_8), DrugInfo.class);

        if (!invoker.getActorId().equals(drugInfo.getManufacturerId())) {
            throw new ChaincodeException(String.format("Hersteller %s ist nicht der Hersteller von Medikament %s.", invoker.getActorId(), drugId), PharmacyErrors.PERMISSION_DENIED.toString());
        }

        String batchId = generateDeterministicUUID(invoker.getActorId(), stub.getTxId() + drugId + quantity);

        if (batchExists(ctx, batchId)) {
            throw new ChaincodeException(String.format("Charge mit generierter ID %s existiert bereits. Dies sollte nicht passieren.", batchId), PharmacyErrors.BATCH_ALREADY_EXISTS.toString());
        }

        List<String> tags;
        try {
            tags = genson.deserialize(tagsJson, List.class);
        } catch (Exception e) {
            throw new ChaincodeException("Ungültiges Tags-JSON-Format: " + e.getMessage(), PharmacyErrors.INVALID_JSON.toString());
        }

        // Konstruktor hat 6 Parameter, was konform ist.
        Batch batch = new Batch(batchId, drugId, quantity, invoker.getActorId(), description, tags);
        stub.putState(batch.getId(), batch.toJSONString().getBytes(StandardCharsets.UTF_8));
        return batch.toJSONString();
    }

    /**
     * Liest die Details einer Charge aus dem Ledger.
     * Jeder genehmigte Akteur kann Chargen lesen.
     *
     * @param ctx Der Transaktionskontext.
     * @param batchId Die ID der Charge.
     * @return Die Batch als JSON-String.
     */
    // Beispielausführung: {"function":"readBatch","Args":["UUID-der-Batch"]}
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String readBatch(final Context ctx, final String batchId) {
        ChaincodeStub stub = ctx.getStub();
        getCurrentActor(ctx); // Autorisierungsprüfung

        byte[] batchState = stub.getState(batchId);
        if (batchState == null || batchState.length == 0) {
            throw new ChaincodeException(String.format("Charge mit ID %s nicht gefunden.", batchId), PharmacyErrors.BATCH_NOT_FOUND.toString());
        }
        return new String(batchState, StandardCharsets.UTF_8);
    }

    /**
     * Überprüft, ob eine Charge mit der gegebenen ID existiert.
     *
     * @param ctx Der Transaktionskontext.
     * @param batchId Die ID der Charge.
     * @return True, wenn die Charge existiert, sonst False.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean batchExists(final Context ctx, final String batchId) {
        ChaincodeStub stub = ctx.getStub();
        return stub.getState(batchId).length > 0;
    }

    /**
     * Erstellt eine einzelne Medikamenteneinheit.
     * Nur der Hersteller der zugehörigen Charge kann Medikamenteneinheiten erstellen.
     * Die ID der DrugUnit wird deterministisch generiert.
     * Der Eigentümer ist initial der Hersteller, und der Status ist 'manufactured'.
     *
     * @param ctx Der Transaktionskontext.
     * @param batchId Die ID der Charge, zu der diese Einheit gehört.
     * @param description Eine Beschreibung der Medikamenteneinheit (Alias).
     * @param tagsJson Ein JSON-String, der eine Liste von Tags darstellt.
     * @return Die neu erstellte DrugUnit als JSON-String.
     */
    // Beispielausführung (als Hersteller): {"function":"createDrugUnit","Args":["UUID-der-Batch", "Verkaufspackung", "[\"blister\",\"versiegelt\"]"]}
    @Transaction()
    public String createDrugUnit(final Context ctx, final String batchId, final String description, final String tagsJson) {
        ChaincodeStub stub = ctx.getStub();
        Actor invoker = getCurrentActor(ctx);

        byte[] batchState = stub.getState(batchId);
        if (batchState == null || batchState.length == 0) {
            throw new ChaincodeException(String.format("Charge mit ID %s nicht gefunden.", batchId), PharmacyErrors.BATCH_NOT_FOUND.toString());
        }
        Batch batch = genson.deserialize(new String(batchState, StandardCharsets.UTF_8), Batch.class);

        if (!"hersteller".equals(invoker.getRole()) || !invoker.getActorId().equals(batch.getManufacturerId())) {
            throw new ChaincodeException(String.format("Akteur %s ist nicht der Hersteller der Charge %s.", invoker.getActorId(), batchId), PharmacyErrors.PERMISSION_DENIED.toString());
        }

        // Deterministic UUID based on invoker ID, batchId and transaction ID
        String drugUnitId = generateDeterministicUUID(invoker.getActorId(), stub.getTxId() + batchId + description);

        if (drugUnitExists(ctx, drugUnitId)) {
            throw new ChaincodeException(String.format("Medikamenteneinheit mit generierter ID %s existiert bereits. Dies sollte nicht passieren.", drugUnitId), PharmacyErrors.DRUG_UNIT_ALREADY_EXISTS.toString());
        }

        List<String> tags;
        try {
            tags = genson.deserialize(tagsJson, List.class);
        } catch (Exception e) {
            throw new ChaincodeException("Ungültiges Tags-JSON-Format: " + e.getMessage(), PharmacyErrors.INVALID_JSON.toString());
        }

        // Das Builder-Pattern von DrugUnit macht die Konstruktorparameter-Anzahl irrelevant für diese Regel.
        DrugUnit drugUnit = new DrugUnit.Builder()
                .id(drugUnitId)
                .batchId(batch.getId())
                .drugId(batch.getDrugId())
                .owner(invoker.getActorId())
                .manufacturerId(batch.getManufacturerId())
                .description(description)
                .tags(tags)
                .currentState("manufactured")
                .build();

        stub.putState(drugUnitId, drugUnit.toJSONString().getBytes(StandardCharsets.UTF_8));
        return drugUnit.toJSONString();
    }

    /**
     * Überträgt den Besitz einer Medikamenteneinheit an einen neuen Eigentümer.
     * Nur der aktuelle Eigentümer kann den Besitz übertragen.
     * Der neue Eigentümer muss ein 'grosshaendler' oder 'apotheke' sein und genehmigt sein.
     * Der Status der Medikamenteneinheit wird auf 'in_transit' gesetzt.
     *
     * @param ctx Der Transaktionskontext.
     * @param drugUnitId Die ID der Medikamenteneinheit.
     * @param newOwnerId Die ID des neuen Eigentümers.
     * @return Die aktualisierte DrugUnit als JSON-String.
     */
    // Beispielausführung (als aktueller Besitzer): {"function":"transferDrugUnit","Args":["UUID-der-DrugUnit", "UUID-des-Grosshändlers"]}
    @Transaction()
    public String transferDrugUnit(final Context ctx, final String drugUnitId, final String newOwnerId) {
        ChaincodeStub stub = ctx.getStub();
        Actor invoker = getCurrentActor(ctx);

        byte[] drugUnitState = stub.getState(drugUnitId);
        if (drugUnitState == null || drugUnitState.length == 0) {
            throw new ChaincodeException(String.format("Medikamenteneinheit mit ID %s nicht gefunden.", drugUnitId), PharmacyErrors.DRUG_UNIT_NOT_FOUND.toString());
        }
        DrugUnit existingDrugUnit = genson.deserialize(new String(drugUnitState, StandardCharsets.UTF_8), DrugUnit.class);

        if (!invoker.getActorId().equals(existingDrugUnit.getOwner())) {
            throw new ChaincodeException("Nur der aktuelle Eigentümer kann eine Medikamenteneinheit übertragen.", PharmacyErrors.PERMISSION_DENIED.toString());
        }

        byte[] newOwnerState = stub.getState(newOwnerId);
        if (newOwnerState == null || newOwnerState.length == 0) {
            throw new ChaincodeException(String.format("Neuer Eigentümer mit ID %s nicht gefunden.", newOwnerId), PharmacyErrors.ACTOR_NOT_FOUND.toString());
        }
        Actor newOwner = genson.deserialize(new String(newOwnerState, StandardCharsets.UTF_8), Actor.class);

        if (!"approved".equals(newOwner.getStatus())) {
            throw new ChaincodeException(String.format("Neuer Eigentümer %s ist nicht genehmigt.", newOwnerId), PharmacyErrors.PERMISSION_DENIED.toString());
        }
        if (!("grosshaendler".equals(newOwner.getRole()) || "apotheke".equals(newOwner.getRole()))) {
            throw new ChaincodeException(String.format("Neuer Eigentümer %s hat keine gültige Rolle für den Transfer (muss Grosshändler oder Apotheke sein).", newOwnerId), PharmacyErrors.PERMISSION_DENIED.toString());
        }

        DrugUnit updatedDrugUnit = new DrugUnit.Builder()
                .fromExistingDrugUnit(existingDrugUnit) // Kopiert alle bestehenden Werte
                .owner(newOwnerId) // Neuer Eigentümer
                .currentState("in_transit") // Status auf 'in_transit' gesetzt
                .dispensedBy(null) // Zurücksetzen bei Transfer
                .dispensedTo(null) // Zurücksetzen bei Transfer
                .dispensingTimestamp(null) // Zurücksetzen bei Transfer
                .build();

        stub.putState(drugUnitId, updatedDrugUnit.toJSONString().getBytes(StandardCharsets.UTF_8));
        return updatedDrugUnit.toJSONString();
    }

    /**
     * Zeichnet die Abgabe einer Medikamenteneinheit an einen Endverbraucher oder Patienten auf.
     * Nur ein Akteur mit der Rolle 'apotheke' kann diese Transaktion ausführen.
     * Der Status der Medikamenteneinheit wird auf 'dispensed' gesetzt.
     *
     * @param ctx Der Transaktionskontext.
     * @param drugUnitId Die ID der Medikamenteneinheit.
     * @param dispensedToId Die ID des Empfängers (Patient, Krankenhaus etc.).
     * @return Die aktualisierte DrugUnit als JSON-String.
     */
    // Beispielausführung (als Apotheker): {"function":"dispenseDrugUnit","Args":["UUID-der-DrugUnit", "patient123"]}
    @Transaction()
    public String dispenseDrugUnit(final Context ctx, final String drugUnitId, final String dispensedToId) {
        ChaincodeStub stub = ctx.getStub();
        Actor invoker = getCurrentActor(ctx);

        if (!"apotheke".equals(invoker.getRole())) {
            throw new ChaincodeException("Nur Apotheken können Medikamenteneinheiten abgeben.", PharmacyErrors.PERMISSION_DENIED.toString());
        }

        byte[] drugUnitState = stub.getState(drugUnitId);
        if (drugUnitState == null || drugUnitState.length == 0) {
            throw new ChaincodeException(String.format("Medikamenteneinheit mit ID %s nicht gefunden.", drugUnitId), PharmacyErrors.DRUG_UNIT_NOT_FOUND.toString());
        }
        DrugUnit existingDrugUnit = genson.deserialize(new String(drugUnitState, StandardCharsets.UTF_8), DrugUnit.class);

        if (!invoker.getActorId().equals(existingDrugUnit.getOwner())) {
            throw new ChaincodeException("Die abgebende Apotheke muss der aktuelle Eigentümer der Medikamenteneinheit sein.", PharmacyErrors.PERMISSION_DENIED.toString());
        }
        // Zusätzliche Prüfungen für den Status könnten hier hinzugefügt werden, je nach Geschäftslogik.

        DrugUnit dispensedDrugUnit = new DrugUnit.Builder()
                .fromExistingDrugUnit(existingDrugUnit)
                .currentState("dispensed")
                .dispensedBy(invoker.getActorId())
                .dispensedTo(dispensedToId)
                .dispensingTimestamp(Instant.now().toString()) // Aktueller Zeitstempel
                .build();

        stub.putState(drugUnitId, dispensedDrugUnit.toJSONString().getBytes(StandardCharsets.UTF_8));
        return dispensedDrugUnit.toJSONString();
    }


    /**
     * Fügt eine Temperaturmessung zu einer Medikamenteneinheit hinzu.
     * Jeder genehmigte Akteur kann Temperaturmessungen hinzufügen.
     * Die Temperaturmessung sollte ein String-Format sein, das Zeitstempel und Wert enthält (z.B. "2023-10-27T10:00:00Z_22.5C").
     *
     * @param ctx Der Transaktionskontext.
     * @param drugUnitId Die ID der Medikamenteneinheit.
     * @param temperatureReading Die neue Temperaturmessung.
     * @return Die aktualisierte DrugUnit als JSON-String.
     */
    // Beispielausführung: {"function":"addTemperatureReading","Args":["UUID-der-DrugUnit", "2023-10-27T11:00:00Z_23.0C"]}
    @Transaction()
    public String addTemperatureReading(final Context ctx, final String drugUnitId, final String temperatureReading) {
        ChaincodeStub stub = ctx.getStub();
        getCurrentActor(ctx); // Autorisierungsprüfung (jeder genehmigte Akteur kann dies tun)

        byte[] drugUnitState = stub.getState(drugUnitId);
        if (drugUnitState == null || drugUnitState.length == 0) {
            throw new ChaincodeException(String.format("Medikamenteneinheit mit ID %s nicht gefunden.", drugUnitId), PharmacyErrors.DRUG_UNIT_NOT_FOUND.toString());
        }
        DrugUnit existingDrugUnit = genson.deserialize(new String(drugUnitState, StandardCharsets.UTF_8), DrugUnit.class);

        // Erstellen einer modifizierbaren Liste von Temperaturmessungen
        List<String> updatedTemperatureReadings = new ArrayList<>(existingDrugUnit.getTemperatureReadings());
        updatedTemperatureReadings.add(temperatureReading);

        DrugUnit updatedDrugUnit = new DrugUnit.Builder()
                .fromExistingDrugUnit(existingDrugUnit) // Kopiert alle bestehenden Werte
                .temperatureReadings(updatedTemperatureReadings) // Fügt neue Messung hinzu
                .build();

        stub.putState(drugUnitId, updatedDrugUnit.toJSONString().getBytes(StandardCharsets.UTF_8));
        return updatedDrugUnit.toJSONString();
    }


    /**
     * Liest die Details einer Medikamenteneinheit aus dem Ledger.
     * Jeder genehmigte Akteur kann die Details einer Medikamenteneinheit lesen.
     *
     * @param ctx Der Transaktionskontext.
     * @param drugUnitId Die ID der Medikamenteneinheit.
     * @return Die DrugUnit als JSON-String.
     */
    // Beispielausführung: {"function":"readDrugUnit","Args":["UUID-der-DrugUnit"]}
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String readDrugUnit(final Context ctx, final String drugUnitId) {
        ChaincodeStub stub = ctx.getStub();
        getCurrentActor(ctx); // Autorisierungsprüfung

        byte[] drugUnitState = stub.getState(drugUnitId);
        if (drugUnitState == null || drugUnitState.length == 0) {
            throw new ChaincodeException(String.format("Medikamenteneinheit mit ID %s nicht gefunden.", drugUnitId), PharmacyErrors.DRUG_UNIT_NOT_FOUND.toString());
        }
        return new String(drugUnitState, StandardCharsets.UTF_8);
    }

    /**
     * Überprüft, ob eine Medikamenteneinheit mit der gegebenen ID existiert.
     *
     * @param ctx Der Transaktionskontext.
     * @param drugUnitId Die ID der Medikamenteneinheit.
     * @return True, wenn die Medikamenteneinheit existiert, sonst False.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean drugUnitExists(final Context ctx, final String drugUnitId) {
        ChaincodeStub stub = ctx.getStub();
        return stub.getState(drugUnitId).length > 0;
    }

    /**
     * Führt eine Rich Query aus, um alle Medikamenteneinheiten zu finden, die einem bestimmten Besitzer gehören.
     *
     * @param ctx Der Transaktionskontext.
     * @param ownerId Die ID des Besitzers.
     * @return Eine JSON-Zeichenkette, die eine Liste von Medikamenteneinheiten darstellt.
     */
    // Beispielausführung: {"function":"queryDrugUnitsByOwner","Args":["UUID-des-Grosshändlers"]}
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryDrugUnitsByOwner(final Context ctx, final String ownerId) {
        getCurrentActor(ctx); // Autorisierungsprüfung
        String query = String.format("{\"selector\":{\"owner\":\"%s\"}}", ownerId);
        return richQuery(ctx, query);
    }

    /**
     * Führt eine Rich Query aus, um alle Medikamenteneinheiten für eine bestimmte Medikamenten-ID zu finden.
     *
     * @param ctx Der Transaktionskontext.
     * @param drugId Die ID des Medikaments (aus DrugInfo).
     * @return Eine JSON-Zeichenkette, die eine Liste von Medikamenteneinheiten darstellt.
     */
    // Beispielausführung: {"function":"queryDrugUnitsByDrugId","Args":["UUID-der-DrugInfo"]}
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryDrugUnitsByDrugId(final Context ctx, final String drugId) {
        getCurrentActor(ctx); // Autorisierungsprüfung
        String query = String.format("{\"selector\":{\"drugId\":\"%s\"}}", drugId);
        return richQuery(ctx, query);
    }

    /**
     * Ruft den Transaktionsverlauf für eine bestimmte Medikamenteneinheit ab.
     * Dies zeigt alle Änderungen an diesem Asset über die Zeit.
     *
     * @param ctx Der Transaktionskontext.
     * @param drugUnitId Die ID der Medikamenteneinheit.
     * @return Eine JSON-Zeichenkette, die den Verlauf der Medikamenteneinheit darstellt.
     */
    // Beispielausführung: {"function":"getDrugUnitHistory","Args":["UUID-der-DrugUnit"]}
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getDrugUnitHistory(final Context ctx, final String drugUnitId) {
        ChaincodeStub stub = ctx.getStub();
        getCurrentActor(ctx); // Autorisierungsprüfung

        if (!drugUnitExists(ctx, drugUnitId)) {
            throw new ChaincodeException(String.format("Medikamenteneinheit mit ID %s nicht gefunden.", drugUnitId), PharmacyErrors.DRUG_UNIT_NOT_FOUND.toString());
        }

        QueryResultsIterator<KeyModification> history = stub.getHistoryForKey(drugUnitId);
        Iterator<KeyModification> iterator = history.iterator();
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        while (iterator.hasNext()) {
            if (!first) {
                sb.append(",");
            }
            KeyModification mod = iterator.next();
            sb.append("{");
            sb.append("\"TxId\":\"").append(mod.getTxId()).append("\",");
            sb.append("\"Value\":").append(mod.getStringValue()).append(",");
            sb.append("\"Timestamp\":\"").append(mod.getTimestamp().toString()).append("\",");
            sb.append("\"IsDelete\":").append(mod.isDeleted());
            sb.append("}");
            first = false;
        }
        sb.append("]");
        try {
            history.close();
        } catch (Exception e) {
            throw new ChaincodeException("Fehler beim Schließen des Historie-Iterators: " + e.getMessage(), "HISTORY_ITERATOR_CLOSE_FAILED");
        }
        return sb.toString();
    }

    /**
     * Hilfsfunktion zum Abrufen des aktuellen Akteurs basierend auf der Client-Identität.
     * Überprüft, ob der Akteur im Ledger existiert und den Status 'approved' hat.
     * Die ActorId wird aus der CertId des Clients abgeleitet, welche den Common Name (CN) enthält.
     *
     * @param ctx Der Transaktionskontext.
     * @return Die Actor-Instanz des aufrufenden Akteurs.
     */
    private Actor getCurrentActor(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        String invokerCertId = ctx.getClientIdentity().getId(); // Beispiel: "CN=hersteller-user1::Org1MSP"
        String invokerMspId = ctx.getClientIdentity().getMSPID(); // MSPID des aufrufenden Clients

        // Wir versuchen zuerst die EnrollmentID direkt aus dem Zertifikat zu bekommen,
        // da dies der stabilste Bezeichner ist, der bei der CA-Registrierung vergeben wird.
        String enrollmentId = ctx.getClientIdentity().getAttributeValue("hf.EnrollmentID");

        // Fallback: Wenn hf.EnrollmentID nicht im Zertifikat, versuchen wir es aus dem CN zu parsen.
        if (enrollmentId == null || enrollmentId.isEmpty()) {
            enrollmentId = getEnrollmentIdFromCertId(invokerCertId); // FIX: Korrekter Methodenname
        }

        // Sonderbehandlung für den initialen Admin ("behoerdeAdmin"), dessen enrollmentId vielleicht nicht direkt vom Cert kommt
        // oder wenn die initLedger-Methode keine dynamische EnrollmentID gesetzt hat.
        if ("".equals(enrollmentId) && invokerCertId.contains("admin")) {
            enrollmentId = "behoerdeAdmin"; // Dies muss mit der in initLedger verwendeten EnrollmentID übereinstimmen
        }

        if ("".equals(enrollmentId)) {
            throw new ChaincodeException("Konnte Enrollment-ID aus Client-Zertifikat nicht ableiten. Stellen Sie sicher, dass 'hf.EnrollmentID' oder ein Common Name (CN) im Zertifikat enthalten ist.", PharmacyErrors.INVALID_ARGUMENT.toString());
        }

        // Finde den Akteur im Ledger über seine Enrollment-ID
        // Es ist besser, die Suche über die EnrollmentID zu machen, wenn wir sie haben,
        // da die ActorId eine generierte UUID ist.
        String query = String.format("{\"selector\":{\"enrollmentId\":\"%s\"}}", enrollmentId);
        Actor invokerActor = null;
        try (QueryResultsIterator<KeyValue> results = stub.getQueryResult(query)) {
            Iterator<KeyValue> iterator = results.iterator();
            if (iterator.hasNext()) {
                KeyValue result = iterator.next();
                invokerActor = genson.deserialize(result.getStringValue(), Actor.class); // FIX: Direkte Übergabe des Strings
            }
        } catch (Exception e) {
            throw new ChaincodeException("Fehler bei der Abfrage des aufrufenden Akteurs: " + e.getMessage(), PharmacyErrors.RICH_QUERY_FAILED.toString());
        }


        if (invokerActor == null) {
            throw new ChaincodeException(String.format("Aufrufender Akteur mit Enrollment-ID '%s' nicht im Ledger gefunden. Registrierung und Genehmigung erforderlich.", enrollmentId), PharmacyErrors.ACTOR_NOT_FOUND.toString());
        }

        // Zusätzliche Prüfung: Stellen Sie sicher, dass die certId im Ledger mit der aktuellen übereinstimmt,
        // um Spoofing zu verhindern.
        if (!invokerCertId.equals(invokerActor.getCertId())) {
            throw new ChaincodeException(String.format("Zertifikat-ID des Aufrufers stimmt nicht mit der für Akteur '%s' registrierten CertId überein. Potenzieller Identitätsdiebstahl.", invokerActor.getName()), PharmacyErrors.PERMISSION_DENIED.toString());
        }

        // Prüfe auch die MSPID zur Sicherheit
        if (!invokerMspId.equals(invokerActor.getMspId())) {
            throw new ChaincodeException(String.format("MSPID des Aufrufers ('%s') stimmt nicht mit der für Akteur '%s' registrierten MSPID ('%s') überein.", invokerMspId, invokerActor.getName(), invokerActor.getMspId()), PharmacyErrors.PERMISSION_DENIED.toString());
        }

        if (!"approved".equals(invokerActor.getStatus())) {
            throw new ChaincodeException(String.format("Akteur '%s' (ID: %s) ist nicht genehmigt und darf keine Transaktionen ausführen. Status: %s", invokerActor.getName(), invokerActor.getActorId(), invokerActor.getStatus()), PharmacyErrors.PERMISSION_DENIED.toString());
        }
        return invokerActor;
    }

    /**
     * Extrahiert die Enrollment-ID (Common Name oder hf.EnrollmentID) aus einer Zertifikats-ID.
     * Versucht zuerst, das "hf.EnrollmentID"-Attribut zu extrahieren.
     * Wenn nicht vorhanden, parst es den Common Name (CN) aus der CertId.
     *
     * @param certId Die Zertifikats-ID (z.B. "x509::/CN=hersteller-user1::Org1MSP").
     * @return Die extrahierte Enrollment-ID oder ein leerer String, wenn nicht gefunden.
     */
    private String getEnrollmentIdFromCertId(final String certId) {
        // Zuerst versuchen, hf.EnrollmentID-Attribut aus der CertId zu parsen, falls es direkt kodiert ist.
        // Fabric-Client-Identitäten geben oft CN=name::IssuerDN zurück.
        Pattern pattern = Pattern.compile("CN=([^,]+)(?:,|::)"); // Matcht CN=VALUE, oder CN=VALUE::
        Matcher matcher = pattern.matcher(certId);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * Erzeugt eine deterministische UUID basierend auf einer Seed-Zeichenkette und der Transaktions-ID.
     * Dies stellt sicher, dass dieselbe Eingabe immer dieselbe UUID erzeugt.
     * Die Seed-Zeichenkette sollte eindeutig sein (z.B. Enrollment-ID + TxId).
     *
     * @param seedString Eine Zeichenkette, die zur Generierung der UUID verwendet wird (z.B. Enrollment-ID oder ActorId).
     * @param txId Die Transaktions-ID, um weitere Eindeutigkeit hinzuzufügen.
     * @return Eine deterministische UUID als String.
     * @throws ChaincodeException Wenn ein Fehler bei der UUID-Generierung auftritt.
     */
    private String generateDeterministicUUID(final String seedString, final String txId) {
        try {
            String name = seedString + txId;
            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(nameBytes);

            // Setze die Version auf 5 (SHA-1)
            hash[6] &= 0x0f;
            hash[6] |= 0x50; // Version 5 = 0101b

            // Setze die Variante auf RFC 4122
            hash[8] &= 0x3f;
            hash[8] |= 0x80; // Variant 10xx = 1000b

            return UUID.nameUUIDFromBytes(hash).toString();
        } catch (NoSuchAlgorithmException e) {
            // Dieser Fehler sollte in einer Standard-Java-Umgebung nie auftreten.
            throw new ChaincodeException("Fehler bei der UUID-Generierung: SHA-1 nicht verfügbar.", PharmacyErrors.UUID_GENERATION_FAILED.toString());
        }
    }

    /**
     * Führt eine CouchDB Rich Query auf dem Ledger aus.
     *
     * @param ctx Der Transaktionskontext.
     * @param query Der CouchDB Query-String.
     * @return Eine JSON-Array-Zeichenkette der Abfrageergebnisse.
     */
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

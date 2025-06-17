package de.jklein.fabric.samples.assettransfer;

import de.jklein.fabric.samples.assettransfer.model.Actor;
import de.jklein.fabric.samples.assettransfer.model.Batch; //
import de.jklein.fabric.samples.assettransfer.model.Medication; //
import de.jklein.fabric.samples.assettransfer.model.MedicationUnit;
import de.jklein.fabric.samples.assettransfer.model.TransferInitiatedEvent; // Import hinzugefügt
import de.jklein.fabric.samples.assettransfer.permission.RoleConstants; //
import org.hyperledger.fabric.contract.Context; //
import org.hyperledger.fabric.contract.annotation.Contract; //
import org.hyperledger.fabric.contract.annotation.Default; //
import org.hyperledger.fabric.contract.annotation.Info; //
import org.hyperledger.fabric.contract.annotation.License; //
import org.hyperledger.fabric.contract.annotation.Transaction; //

import java.time.Instant;
import java.util.ArrayList; //
import java.util.Collections;
import java.util.List; //
import java.util.UUID;


@Contract(
        name = "pharma",
        info = @Info(
                title = "Pharma Supply Chain",
                description = "Manages medications, batches, shipments, and regulatory actions",
                version = "0.0.1",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"))) //
@Default //
public final class PharmaChaincode extends BaseContract {

    public PharmaChaincode() {
        super(); // Ruft den Konstruktor der Basisklasse auf, der die Services initialisiert
    }

    /**
     * Initialisiert das Ledger mit Beispieldaten.
     * Nur für Demo-Zwecke. In einer Produktionsumgebung wird dies meist entfernt.
     * @param ctx Der Transaktionskontext.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT) //
    public void InitLedger(final Context ctx) { //
        // Beispielhafte Initialisierung von Akteuren
        String adminActorId = ctx.getClientIdentity().getId(); // Annahme: InitLedger wird von einem Admin aufgerufen
        String adminPublicKey = "admin_pk_value"; // Platzhalter für echten Public Key

        // Behörde direkt als APPROVED registrieren
        registerActor(ctx, "RegAuthority1", "Bundesinstitut für Pharmazeutika", RoleConstants.BEHOERDE, adminPublicKey);
        // Hersteller muss erst genehmigt werden
        registerActor(ctx, "PharmaCorp", "Pharma Corp AG", RoleConstants.HERSTELLER, "pharma_pk_123");
        registerActor(ctx, "GrosshaendlerA", "Grosshändler AG A", RoleConstants.GROSSHAENDLER, "gross_pk_abc");
        registerActor(ctx, "ApothekeX", "Apotheke X GmbH", RoleConstants.APOTHEKE, "apo_pk_xyz");

        // Manuelle Genehmigung für PharmaCorp (durch RegAuthority1)
        // In einem echten Szenario müsste InitLedger die Rechte des Anlegers übergeben bekommen
        // oder die Freigabe separat erfolgen. Hier nur zur Initialisierung:
        // (Die 'approveActor' Transaktion sollte nur von einem BEHOERDE-Akteur aufgerufen werden)
        // Für InitLedger müssen wir die Prüfung für den Aufrufer hier umgehen oder davon ausgehen,
        // dass der Init-Aufrufer die Rechte hat.
        // Wir rufen die approveActor Methode direkt aus dem Service auf, um die Berechtigungsprüfung im Contract zu umgehen,
        // da InitLedger als 'admin' ausgeführt wird und alle Rechte haben sollte.
        actorService.approveActorRegistration(ctx, "PharmaCorp");
        actorService.approveActorRegistration(ctx, "GrosshaendlerA");
        actorService.approveActorRegistration(ctx, "ApothekeX");

        // Beispiel-Medikament (von PharmaCorp)
        String pharmaCorpActorId = "PharmaCorp"; // Annahme: Dies ist die ActorId des Herstellers
        Medication med1 = new Medication("MED001", "GTIN001", "Aspirin Complex", "PharmaCorp_Org", pharmaCorpActorId, RoleConstants.ERSTELLT, Collections.emptyList());
        assetService.putAsset(ctx, med1);

        // Medikament FREIGEGEBEN durch eine simulierte Behördenaktion (durch RegAuthority1)
        // Normalerweise eine separate Transaktion über RegulatoryContract
        Medication approvedMed1 = new Medication("MED001", "GTIN001", "Aspirin Complex", "PharmaCorp_Org", pharmaCorpActorId, RoleConstants.FREIGEGEBEN, Collections.emptyList());
        assetService.putAsset(ctx, approvedMed1);


        // Beispiel-Charge (von PharmaCorp)
        Batch batch1 = new Batch("BATCH001", approvedMed1.getKey(), "2025-01-01", "2027-01-01", 100, RoleConstants.ERSTELLT, Collections.emptyList());
        assetService.putAsset(ctx, batch1);

        // Charge FREIGEGEBEN (durch PharmaCorp, der Ersteller des Med.)
        Batch approvedBatch1 = new Batch("BATCH001", approvedMed1.getKey(), "2025-01-01", "2027-01-01", 100, RoleConstants.FREIGEGEBEN, Collections.emptyList());
        assetService.putAsset(ctx, approvedBatch1);


        // Medikamenteneinheiten für BATCH001 generieren (simuliert durch PharmaCorp als Creator)
        // Hier würde normalerweise 'addUnitsToBatch' im MedicationContract aufgerufen
        List<MedicationUnit> createdUnitsForBatch1 = new ArrayList<>();
        for (int i = 0; i < 5; i++) { // 5 Einheiten
            String unitId = "UNIT001-" + UUID.randomUUID().toString();
            MedicationUnit unit = new MedicationUnit(unitId, approvedMed1.getKey(), approvedBatch1.getKey(), pharmaCorpActorId, RoleConstants.FREIGEGEBEN, Collections.emptyList());
            assetService.putAsset(ctx, unit);
            createdUnitsForBatch1.add(unit);
            // Log for unit creation (simplified for InitLedger)
            eventService.logNewEvent(ctx, new TransferInitiatedEvent(
                    UUID.randomUUID().toString(), unit.getKey(), pharmaCorpActorId, pharmaCorpActorId, Instant.now().toString(), ctx.getStub().getTxId()
            ));
        }

        System.out.println("Ledger initialisiert mit Beispieldaten.");
    }

    /**
     * Registriert einen neuen Akteur im System.
     * @param ctx Der Transaktionskontext.
     * @param actorId Die eindeutige ID des Akteurs.
     * @param actorName Der Name des Akteurs/der Organisation.
     * @param roleType Die Rolle des Akteurs (z.B. RoleConstants.HERSTELLER).
     * @param publicKey Der öffentliche Schlüssel des Akteurs-Zertifikats.
     * @return Das neu erstellte Actor-Objekt.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT) //
    public Actor registerActor(final Context ctx, final String actorId, final String actorName, final String roleType, final String publicKey) {
        // Keine requireActorApproved() hier, da dies die Registrierungstransaktion ist.
        // Hier könnte man prüfen, ob der actorId des Aufrufers (aus dem Zertifikat) mit dem actorId übereinstimmt,
        // den er registrieren möchte, um Self-Registration zu erzwingen.
        // authService.requireCallerActorIdMatches(ctx, actorId); // z.B. eine neue Methode im AuthService

        return actorService.registerNewActor(ctx, actorId, actorName, roleType, publicKey);
    }

    /**
     * Genehmigt die Registrierung eines Akteurs.
     * Nur von einem BEHOERDE-Akteur aufrufbar.
     * @param ctx Der Transaktionskontext.
     * @param actorId Die ID des zu genehmigenden Akteurs.
     * @return Das aktualisierte Actor-Objekt.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT) //
    public Actor approveActor(final Context ctx, final String actorId) {
        // Prüfen, ob der Aufrufer eine Behörde ist und selbst approved.
        // Behörden sind direkt bei Registrierung approved.
        authService.requireActorApproved(ctx);
        authService.requireAnyOfRoles(ctx, RoleConstants.BEHOERDE); //

        return actorService.approveActorRegistration(ctx, actorId);
    }
}
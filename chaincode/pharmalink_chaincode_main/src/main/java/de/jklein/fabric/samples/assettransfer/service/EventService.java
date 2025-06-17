// src/main/java/de/jklein/fabric/samples/assettransfer/service/EventService.java
package de.jklein.fabric.samples.assettransfer.service;

import de.jklein.fabric.samples.assettransfer.model.Batch;
import de.jklein.fabric.samples.assettransfer.model.IAsset;
import de.jklein.fabric.samples.assettransfer.model.IEvent;
import de.jklein.fabric.samples.assettransfer.model.MedicationUnit;
import de.jklein.fabric.samples.assettransfer.model.TransferRecord;
import org.hyperledger.fabric.contract.Context; //
import org.hyperledger.fabric.shim.ChaincodeException; //
import org.hyperledger.fabric.shim.ledger.KeyValue; //
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator; //

import java.util.ArrayList; //
import java.util.Comparator;
import java.util.List; //
import java.util.Objects; //

/**
 * Service zur Verwaltung von Event-Objekten im Ledger.
 * Zuständig für das Logging von Events und das Abrufen chronologischer Event-Historien.
 */
public class EventService {

    private final AssetService assetService; // Abhängigkeit zum AssetService für Persistenz

    private static final String EVENT_KEY_PREFIX = "EVENT_";

    /**
     * Konstruktor für den EventService.
     * @param assetService Der AssetService zur Interaktion mit dem Ledger.
     */
    public EventService(final AssetService assetService) {
        Objects.requireNonNull(assetService, "AssetService cannot be null"); //
        this.assetService = assetService;
    }

    /**
     * Loggt ein neues Event im Ledger.
     * Jedes Event erhält einen eindeutigen Schlüssel basierend auf seiner EventId.
     * @param ctx Der Transaktionskontext.
     * @param event Das zu loggende Event-Objekt (muss IEvent implementieren).
     * @throws ChaincodeException wenn das Event bereits existiert.
     */
    public void logNewEvent(final Context ctx, final IEvent event) {
        String eventKey = EVENT_KEY_PREFIX + event.getEventId();
        if (assetService.assetExists(ctx, eventKey)) {
            throw new ChaincodeException(String.format("Event '%s' existiert bereits.", event.getEventId()), "EVENT_ALREADY_EXISTS");
        }
        // Event als Asset speichern
        assetService.putAsset(ctx, (IAsset) event); // IEvent muss auch IAsset implementieren oder Genson muss IEvent serialisieren können
        // HINWEIS: Hier ist ein Cast zu IAsset, das bedeutet IEvent müsste IAsset erweitern
        // Oder AssetService.putAsset muss ein generisches Object nehmen, das Genson verarbeiten kann.
        // Für dieses Beispiel gehen wir davon aus, dass IEvent auch IAsset ist, oder Genson direkt mit IEvent umgehen kann.
        // Alternativ: AssetService.putObject(ctx, key, object)
    }

    /**
     * Ruft eine chronologisch sortierte Liste aller Events ab, die sich auf eine gegebene Asset-Hierarchie beziehen.
     * Dies ist eine komplexe Abfrage, die in der Regel Rich Queries (CouchDB) nutzt.
     *
     * @param ctx Der Transaktionskontext.
     * @param topLevelAssetKey Der Schlüssel des obersten Assets der Hierarchie (z.B. "MED_123").
     * @return Eine chronologisch sortierte Liste von IEvent-Objekten.
     * @throws ChaincodeException bei Abfragefehlern.
     */
    public List<IEvent> getEventsForAssetHierarchy(final Context ctx, final String topLevelAssetKey) {
        List<String> relatedAssetKeys = new ArrayList<>();
        relatedAssetKeys.add(topLevelAssetKey); // Das Top-Level-Asset selbst

        // Schritt 1: Sammle alle relevanten Asset-Keys in der Hierarchie
        // Dies erfordert, dass man von oben nach unten navigieren kann.
        // Beispiel für MEDICATION -> BATCH -> UNIT
        if (topLevelAssetKey.startsWith("MED_")) {
            // Finde alle Batches, die zu diesem Medikament gehören
            String queryBatches = String.format("{\"selector\":{\"medicationKey\":\"%s\"}}", topLevelAssetKey);
            QueryResultsIterator<KeyValue> batchIterator = ctx.getStub().getQueryResult(queryBatches);
            for (KeyValue kv : batchIterator) {
                Batch batch = assetService.genson.deserialize(kv.getStringValue(), Batch.class);
                relatedAssetKeys.add(batch.getKey());

                // Finde alle Units, die zu diesem Batch gehören
                String queryUnits = String.format("{\"selector\":{\"batchKey\":\"%s\"}}", batch.getKey());
                QueryResultsIterator<KeyValue> unitIterator = ctx.getStub().getQueryResult(queryUnits);
                for (KeyValue unitKv : unitIterator) {
                    MedicationUnit unit = assetService.genson.deserialize(unitKv.getStringValue(), MedicationUnit.class);
                    relatedAssetKeys.add(unit.getKey());
                }
            }
        } else if (topLevelAssetKey.startsWith("BATCH_")) {
            // Finde alle Units, die zu diesem Batch gehören
            String queryUnits = String.format("{\"selector\":{\"batchKey\":\"%s\"}}", topLevelAssetKey);
            QueryResultsIterator<KeyValue> unitIterator = ctx.getStub().getQueryResult(queryUnits);
            for (KeyValue unitKv : unitIterator) {
                MedicationUnit unit = assetService.genson.deserialize(unitKv.getStringValue(), MedicationUnit.class);
                relatedAssetKeys.add(unit.getKey());
            }
        }
        // Annahme: Direkter Abruf von Events für Actor, Shipment, RegulatoryAction
        // Für diese Typen ist die Hierarchie flacher.

        // Schritt 2: Führe eine Rich Query aus, um alle Events für die gesammelten Keys abzurufen
        List<IEvent> events = new ArrayList<>();
        String queryString = "{\"selector\":{\"relatedAssetKey\":{\"$in\":" + assetService.genson.serialize(relatedAssetKeys) + "}}, \"sort\":[{\"timestamp\":\"asc\"}]}";

        try (QueryResultsIterator<KeyValue> resultsIterator = ctx.getStub().getQueryResult(queryString)) {
            for (KeyValue entry : resultsIterator) {
                // Event-Typ dynamisch deserialisieren, oder alle Event-Typen in einem Wrapper speichern
                // Für dieses Beispiel nehmen wir an, dass alle Events in einem generischen IEvent-Format gespeichert werden können
                // oder dass wir eine Factory haben, die basierend auf einem "eventType"-Feld den korrekten Typ deserialisiert.
                // Einfachheit halber deserialisieren wir hier als TransferRecord (beispielhaft), müsste generischer sein.
                // Besser wäre eine Map von Event-Type zu Class<T extends IEvent>
                try {
                    // Dies ist eine Vereinfachung. In einer echten Implementierung müsste
                    // der korrekte IEvent-Typ basierend auf dem Event-Key-Präfix oder einem im JSON gespeicherten Typ-Feld ermittelt werden.
                    // Für dieses Beispiel: Wir gehen davon aus, dass wir alle Event-Typen durchprobieren oder
                    // eine generische Event-Klasse haben, die alle Event-Payloads aufnehmen kann.
                    events.add(assetService.genson.deserialize(entry.getStringValue(), TransferRecord.class)); // Beispielhaft
                } catch (Exception e) {
                    // Loggen von Deserialisierungsfehlern
                    System.err.println("Fehler beim Deserialisieren eines Events: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new ChaincodeException("Fehler bei der Abfrage von Events: " + e.getMessage(), "EVENT_QUERY_ERROR");
        }

        // Schritt 3: Events chronologisch sortieren (falls die Rich Query dies nicht perfekt macht oder keine Rich Query verwendet wird)
        events.sort(Comparator.comparing(IEvent::getTimestamp));

        return events;
    }
}

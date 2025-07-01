package de.jklein.pharmalink.service;

import de.jklein.pharmalink.client.fabric.FabricClient;
import de.jklein.pharmalink.api.dto.CreateMedikamentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MedicationService {

    private final FabricClient fabricClient;

    /**
     * Erstellt ein Medikament mit den Daten aus dem DTO.
     * Ruft die spezifische Chaincode-Funktion 'createMedikament' auf.
     * Die Reihenfolge der Argumente muss exakt der Chaincode-Signatur entsprechen.
     *
     * @param identity Die Identität des Aufrufers (z.B. "hersteller-bayer-1").
     * @param request Das DTO mit den Medikamentendaten.
     * @return Das Ergebnis der Transaktion vom Chaincode.
     */
    public String createMedikament(final String identity, final CreateMedikamentRequest request) throws Exception {
        String[] tagsArray = request.getTags().toArray(new String[0]);

        // KORREKT: submitGenericTransaction mit der Identität als erstem Parameter aufrufen.
        return fabricClient.submitGenericTransaction(identity, "createMedikament",
                request.getBezeichnung(),
                request.getInfoblattHash(),
                request.getIpfsLink()
        );
    }

    /**
     * Erstellt Units für ein existierendes Medikament.
     *
     * @param identity Die Identität des Aufrufers.
     * @param medId Die ID des Medikaments.
     * @param amount Die Anzahl der zu erstellenden Units.
     */
    public String createUnitsForMedication(final String identity, final String medId, final int amount) throws Exception {
        // KORREKT: submitGenericTransaction verwenden.
        return fabricClient.submitGenericTransaction(identity, "createUnitsForMedikament", medId, String.valueOf(amount));
    }

    /**
     * Fragt ein spezifisches Medikament ab.
     *
     * @param identity Die Identität des Aufrufers.
     * @param medId Die ID des abzufragenden Medikaments.
     * @return Die JSON-Darstellung des Medikaments.
     */
    public String getMedikamentById(final String identity, final String medId) throws Exception {
        // KORREKT: Auch für Lese-Operationen wird hier submitGenericTransaction verwendet,
        // das Gateway leitet es als "evaluate" weiter, wenn die Chaincode-Methode so annotiert ist.
        return fabricClient.submitGenericTransaction(identity, "getMedikamentById", medId);
    }

    /**
     * Fragt eine spezifische Unit ab.
     *
     * @param identity Die Identität des Aufrufers.
     * @param unitId Die ID der abzufragenden Unit.
     * @return Die JSON-Darstellung der Unit.
     */
    public String getUnitById(final String identity, final String unitId) throws Exception {
        // KORREKT: submitGenericTransaction verwenden.
        return fabricClient.submitGenericTransaction(identity, "getUnitById", unitId);
    }
}
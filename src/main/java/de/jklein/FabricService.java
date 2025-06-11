package de.jklein;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hyperledger.fabric.client.*;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@ApplicationScoped
public class FabricService {

    private static final Logger LOG = Logger.getLogger(FabricService.class);

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_WAIT_SECONDS = 2;
    private static final String SET_TIME_TRANSACTION = "put";
    private static final String GET_TIME_TRANSACTION = "get";
    private static final String TIME_KEY = "time";

    @Inject
    Gateway gateway;

    @ConfigProperty(name = "hlf.channelName")
    String channelName;

    @ConfigProperty(name = "hlf.chaincodeName")
    String chaincodeName;

    /**
     * Aktualisiert die Zeit im Ledger auf eine blockierende Weise mit Retry-Logik.
     * @return Der aktualisierte Zeitstempel.
     * @throws GatewayException wenn die Operation endgültig fehlschlägt.
     * @throws InterruptedException wenn der Thread beim Warten unterbrochen wird.
     */
    public String updateAndGetLedgerTimeBlocking() throws GatewayException, InterruptedException {
        Network network = gateway.getNetwork(channelName);
        Contract contract = network.getContract(chaincodeName);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            String newTime = LocalDateTime.now().toString();
            LOG.infof("Versuch %d/%d: Setze Zeit auf '%s'", attempt, MAX_RETRIES, newTime);

            try {
                contract.submitTransaction(SET_TIME_TRANSACTION, TIME_KEY, newTime);
                LOG.infof("✅ Transaktion erfolgreich übermittelt für Zeit: '%s'", newTime);
                return newTime;

            } catch (CommitException e) {
                LOG.warnf("CommitException bei Versuch %d: %s. Starte Verifizierung...", attempt, e.getMessage());

                if (attempt == MAX_RETRIES) {
                    String errorMsg = "Zeit-Aktualisierung fehlgeschlagen nach maximalen Wiederholungen.";
                    LOG.error(errorMsg, e);
                    // Erstelle eine StatusRuntimeException, die an den GatewayException-Konstruktor übergeben wird
                    Status status = Status.ABORTED.withDescription(errorMsg).withCause(e);
                    throw new GatewayException(new StatusRuntimeException(status));
                }

                Thread.sleep(RETRY_WAIT_SECONDS * 1000);

                String ledgerTime = new String(contract.evaluateTransaction(GET_TIME_TRANSACTION, TIME_KEY), StandardCharsets.UTF_8);
                if (newTime.equals(ledgerTime)) {
                    LOG.infof("✅ Verifizierung erfolgreich: Zustand auf dem Ledger stimmt überein.");
                    return ledgerTime;
                } else {
                    LOG.warnf("Verifizierung für Versuch %d fehlgeschlagen. Starte nächsten Versuch.", attempt);
                }
            } catch (EndorseException | SubmitException | CommitStatusException e) {
                String errorMsg = "Permanenter Fehler bei der Transaktionsübermittlung.";
                LOG.error(errorMsg, e);
                Status status = Status.INTERNAL.withDescription(errorMsg).withCause(e);
                throw new GatewayException(new StatusRuntimeException(status));
            }
        }

        String finalErrorMsg = "Operation endgültig fehlgeschlagen nach " + MAX_RETRIES + " Versuchen.";
        Status finalStatus = Status.UNAVAILABLE.withDescription(finalErrorMsg);
        throw new GatewayException(new StatusRuntimeException(finalStatus));
    }
}
package de.jklein;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.hyperledger.fabric.client.GatewayException;
import org.jboss.logging.Logger;

@Path("/fabric")
public class FabricResource {
    private static final Logger LOG = Logger.getLogger(FabricResource.class);

    @Inject
    FabricService fabricService;

    @GET
    @Path("/time")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getLedgerTime() {
        LOG.info("Anfrage für Ledger-Zeit empfangen");
        try {
            // Aufruf der neuen, blockierenden Methode
            String ledgerTime = fabricService.updateAndGetLedgerTimeBlocking();
            LOG.info("Ledger-Zeit erfolgreich abgerufen: " + ledgerTime);
            return Response.ok("Zeit vom Ledger: " + ledgerTime).build();

        } catch (GatewayException e) {
            LOG.error("Fehler bei der Kommunikation mit Hyperledger Fabric", e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Fehler bei der Kommunikation mit der Blockchain: " + e.getMessage())
                    .build();
        } catch (InterruptedException e) {
            LOG.warn("Thread wurde während der Blockchain-Operation unterbrochen", e);
            Thread.currentThread().interrupt();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Der Server-Prozess wurde unterbrochen.")
                    .build();
        }
    }
}
package com.kildeen.sps.demo;

import com.kildeen.sps.BasicSpsEvents;
import com.kildeen.sps.IdWithReceipts;
import com.kildeen.sps.inlet.Inlet;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST endpoint for receiving events (subscriber side).
 * This is called by the SPS publisher when delivering events.
 */
@Path("/receive-event")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DemoInletResource {

    private static final Logger LOG = LoggerFactory.getLogger(DemoInletResource.class);

    @Inject
    Inlet inletService;

    @POST
    @Path("/{eventType}")
    public Response receiveEvent(@PathParam("eventType") String eventType, BasicSpsEvents events) {
        LOG.info("Receiving {} event(s) of type: {}", events.spsEvents().size(), eventType);

        IdWithReceipts receipts = inletService.receive(events.get());

        return Response.ok(receipts).build();
    }
}

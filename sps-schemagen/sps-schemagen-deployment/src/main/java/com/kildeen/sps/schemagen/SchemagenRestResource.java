package com.kildeen.sps.schemagen;

import static java.lang.invoke.MethodHandles.lookup;

import com.kildeen.sps.IdWithReceipts;
import com.kildeen.sps.SpsEvents;
import com.kildeen.sps.inlet.Inlet;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS resource for schema generation events.
 */
@Path("/receive-event")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Schemagen", description = "Schema generation endpoints")
public class SchemagenRestResource {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    @Inject
    Inlet inlet;

    @POST
    @Path("/{type}")
    @Operation(summary = "Receive schema events", description = "Receive events for schema generation processing")
    @APIResponse(responseCode = "204", description = "Events processed successfully")
    @APIResponse(responseCode = "400", description = "Invalid event data")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response receiveEvent(
            @PathParam("type") String type,
            @RequestBody(
                    description = "Events to process",
                    content = @Content(schema = @Schema(implementation = SpsEvents.class))
            )
            @Valid SpsEvents events) {

        LOG.debug("Receiving {} events of type {}", events.spsEvents().size(), type);

        try {
            IdWithReceipts result = inlet.receive(events);
            LOG.debug("Processed events: {}", result);
            return Response.noContent().build();
        } catch (Exception e) {
            LOG.error("Error processing events of type {}", type, e);
            return Response.serverError()
                    .entity(new ErrorResponse("Failed to process events: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Error response DTO
     */
    public record ErrorResponse(String message) {}
}

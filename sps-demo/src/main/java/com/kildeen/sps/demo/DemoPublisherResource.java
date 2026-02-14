package com.kildeen.sps.demo;

import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.publish.Publish;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Demo REST endpoint for publishing events.
 * 
 * Usage:
 *   POST /demo/publish?type=greeting_01
 *   Body: {"message": "Hello World!"}
 */
@Path("/demo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DemoPublisherResource {

    private static final Logger LOG = LoggerFactory.getLogger(DemoPublisherResource.class);

    @Inject
    Publish publishService;

    @Inject
    EventBroadcaster eventBroadcaster;

    @POST
    @Path("/publish")
    public Response publishEvent(
            @QueryParam("type") String eventType,
            Map<String, Object> payload) {
        
        if (eventType == null || eventType.isBlank()) {
            eventType = "demo_event_01";
        }

        String finalEventType = eventType;
        SpsEvent event = new SpsEvent() {
            private final String id = genId();

            @Override
            public String type() {
                return finalEventType;
            }

            @Override
            public String id() {
                return id;
            }

            @Override
            public Map<String, Object> data() {
                return payload != null ? payload : Map.of();
            }
        };

        LOG.info("Publishing event: type={}, id={}", event.type(), event.id());

        Publish.PublishResult result = publishService.publish(List.of(event));

        // Broadcast to dashboard
        eventBroadcaster.broadcast(DashboardEvent.published(event.id(), event.type(), event.data()));

        return Response.ok(Map.of(
                "status", "published",
                "eventId", event.id(),
                "eventType", event.type(),
                "result", result.toString()
        )).build();
    }

    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(Map.of("status", "UP", "service", "sps-demo")).build();
    }

    @POST
    @Path("/event-received")
    public Response eventReceived(DashboardEvent event) {
        LOG.info("Received callback: {} event {}", event.direction(), event.eventId());
        eventBroadcaster.broadcast(event);
        return Response.ok().build();
    }
}

package com.kildeen.sps.demo;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST resource for dashboard SSE streaming and statistics.
 */
@Path("/demo")
public class DashboardResource {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardResource.class);

    @Inject
    EventBroadcaster eventBroadcaster;

    private volatile SseBroadcaster sseBroadcaster;

    @GET
    @Path("/events/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void streamEvents(@Context SseEventSink eventSink, @Context Sse sse) {
        LOG.info("New SSE client connected");
        
        if (sseBroadcaster == null) {
            synchronized (this) {
                if (sseBroadcaster == null) {
                    sseBroadcaster = sse.newBroadcaster();
                    sseBroadcaster.onClose(sink -> LOG.info("SSE client disconnected"));
                    sseBroadcaster.onError((sink, e) -> LOG.warn("SSE error: {}", e.getMessage()));
                    eventBroadcaster.register(sse, sseBroadcaster);
                }
            }
        }
        
        sseBroadcaster.register(eventSink);
        
        // Send initial connection event
        eventSink.send(sse.newEventBuilder()
                .name("connected")
                .data(String.class, "{\"status\":\"connected\"}")
                .build());
    }

    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public EventBroadcaster.Stats getStats() {
        return eventBroadcaster.getStats();
    }

    @DELETE
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public EventBroadcaster.Stats resetStats() {
        eventBroadcaster.resetStats();
        return eventBroadcaster.getStats();
    }
}

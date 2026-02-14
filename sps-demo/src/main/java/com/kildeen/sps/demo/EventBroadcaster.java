package com.kildeen.sps.demo;

import com.kildeen.sps.json.JsonProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Broadcasts dashboard events to all connected SSE clients.
 */
@ApplicationScoped
public class EventBroadcaster {

    private static final Logger LOG = LoggerFactory.getLogger(EventBroadcaster.class);

    private final AtomicReference<SseBroadcaster> broadcaster = new AtomicReference<>();
    private final AtomicReference<Sse> sse = new AtomicReference<>();
    
    private final AtomicInteger publishedCount = new AtomicInteger(0);
    private final AtomicInteger receivedCount = new AtomicInteger(0);
    private final Map<String, AtomicInteger> publishedByType = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> receivedByType = new ConcurrentHashMap<>();

    public void register(Sse sse, SseBroadcaster broadcaster) {
        this.sse.set(sse);
        this.broadcaster.set(broadcaster);
        LOG.info("SSE broadcaster registered");
    }

    public void broadcast(DashboardEvent event) {
        // Update statistics regardless of SSE connection
        if (event.direction() == DashboardEvent.Direction.PUBLISHED) {
            publishedCount.incrementAndGet();
            publishedByType.computeIfAbsent(event.eventType(), k -> new AtomicInteger()).incrementAndGet();
        } else {
            receivedCount.incrementAndGet();
            receivedByType.computeIfAbsent(event.eventType(), k -> new AtomicInteger()).incrementAndGet();
        }

        SseBroadcaster bc = broadcaster.get();
        Sse sseInstance = sse.get();

        if (bc == null || sseInstance == null) {
            LOG.debug("No SSE clients connected, event stats updated but not broadcast");
            return;
        }

        try {
            String json = JsonProvider.json().write(event);
            OutboundSseEvent sseEvent = sseInstance.newEventBuilder()
                    .name("event")
                    .data(String.class, json)
                    .build();
            bc.broadcast(sseEvent);
            LOG.debug("Broadcasted event: {} {}", event.direction(), event.eventId());
        } catch (Exception e) {
            LOG.warn("Failed to broadcast event: {}", e.getMessage());
        }
    }

    public Stats getStats() {
        return new Stats(
                publishedCount.get(),
                receivedCount.get(),
                Map.copyOf(publishedByType.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()))),
                Map.copyOf(receivedByType.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())))
        );
    }

    public void resetStats() {
        publishedCount.set(0);
        receivedCount.set(0);
        publishedByType.clear();
        receivedByType.clear();
    }

    public record Stats(
            int publishedCount,
            int receivedCount,
            Map<String, Integer> publishedByType,
            Map<String, Integer> receivedByType
    ) {}
}

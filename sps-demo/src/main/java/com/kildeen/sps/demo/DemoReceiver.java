package com.kildeen.sps.demo;

import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.inlet.Receiver;
import com.kildeen.sps.json.JsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * Demo receiver that logs incoming events and notifies dashboard.
 * In a real application, this would contain your business logic.
 */
public class DemoReceiver implements Receiver {

    private static final Logger LOG = LoggerFactory.getLogger(DemoReceiver.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final String eventType;
    private final Consumer<DashboardEvent> onEventReceived;
    private final String callbackUrl;
    private int receivedCount = 0;

    public DemoReceiver(String eventType) {
        this(eventType, null, null);
    }

    public DemoReceiver(String eventType, Consumer<DashboardEvent> onEventReceived) {
        this(eventType, onEventReceived, null);
    }

    public DemoReceiver(String eventType, Consumer<DashboardEvent> onEventReceived, String callbackUrl) {
        this.eventType = eventType;
        this.onEventReceived = onEventReceived;
        this.callbackUrl = callbackUrl;
    }

    @Override
    public void receive(SpsEvent spsEvent) {
        receivedCount++;
        LOG.info("========================================");
        LOG.info("RECEIVED EVENT #{}", receivedCount);
        LOG.info("  Type: {}", spsEvent.type());
        LOG.info("  ID:   {}", spsEvent.id());
        LOG.info("  Data: {}", spsEvent.data());
        LOG.info("========================================");

        DashboardEvent dashboardEvent = DashboardEvent.received(
                spsEvent.id(), spsEvent.type(), spsEvent.data(), true);

        // Notify local dashboard
        if (onEventReceived != null) {
            onEventReceived.accept(dashboardEvent);
        }

        // Notify publisher dashboard via callback
        if (callbackUrl != null) {
            notifyPublisher(dashboardEvent);
        }
    }

    private void notifyPublisher(DashboardEvent event) {
        try {
            String json = JsonProvider.json().write(event);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(callbackUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(r -> LOG.debug("Callback sent: {}", r.statusCode()))
                    .exceptionally(e -> { LOG.warn("Callback failed: {}", e.getMessage()); return null; });
        } catch (Exception e) {
            LOG.warn("Failed to notify publisher: {}", e.getMessage());
        }
    }

    @Override
    public String eventType() {
        return eventType;
    }

    public int getReceivedCount() {
        return receivedCount;
    }
}

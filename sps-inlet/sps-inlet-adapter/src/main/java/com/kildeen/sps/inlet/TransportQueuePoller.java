package com.kildeen.sps.inlet;

import com.kildeen.sps.BasicSpsEvents;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.json.JsonProvider;
import com.kildeen.sps.persistence.Database;
import com.kildeen.sps.persistence.TransportQueueEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Polls the transport queue for events when database-based delivery is used.
 * This handles the receiving side of the transport fallback mechanism.
 */
public class TransportQueuePoller {

    private static final Logger LOG = LoggerFactory.getLogger(TransportQueuePoller.class);
    private static final int DEFAULT_POLL_INTERVAL_MS = 1000;
    private static final int DEFAULT_BATCH_SIZE = 100;

    private final Database database;
    private final String subscriberId;
    private final Map<String, Receiver> receiversByType;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final int pollIntervalMs;
    private final int batchSize;

    public TransportQueuePoller(Database database, String subscriberId, List<Receiver> receivers) {
        this(database, subscriberId, receivers, DEFAULT_POLL_INTERVAL_MS, DEFAULT_BATCH_SIZE);
    }

    public TransportQueuePoller(Database database, String subscriberId, List<Receiver> receivers,
                                 int pollIntervalMs, int batchSize) {
        this.database = database;
        this.subscriberId = subscriberId;
        this.receiversByType = receivers.stream()
                .collect(java.util.stream.Collectors.toMap(Receiver::eventType, r -> r));
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "transport-queue-poller-" + subscriberId);
            t.setDaemon(true);
            return t;
        });
        this.pollIntervalMs = pollIntervalMs;
        this.batchSize = batchSize;
    }

    /**
     * Start polling the transport queue.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            LOG.info("Starting transport queue poller for subscriber: {}", subscriberId);
            scheduler.scheduleWithFixedDelay(this::poll, 0, pollIntervalMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stop polling the transport queue.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            LOG.info("Stopping transport queue poller for subscriber: {}", subscriberId);
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void poll() {
        if (!running.get()) {
            return;
        }

        try {
            List<TransportQueueEntry> entries = database.pollTransportQueue(subscriberId, batchSize);
            
            if (!entries.isEmpty()) {
                LOG.debug("Polled {} entries from transport queue for subscriber {}", 
                        entries.size(), subscriberId);
            }

            for (TransportQueueEntry entry : entries) {
                processEntry(entry);
            }
        } catch (Exception e) {
            LOG.error("Error polling transport queue for subscriber {}: {}", 
                    subscriberId, e.getMessage(), e);
        }
    }

    private void processEntry(TransportQueueEntry entry) {
        try {
            Receiver receiver = receiversByType.get(entry.eventType());
            if (receiver == null) {
                LOG.warn("No receiver found for event type: {}, marking as processed", entry.eventType());
                database.markTransportProcessed(entry.eventId(), entry.subscriberId());
                return;
            }

            // Parse the payload to extract events (use BasicSpsEvents for deserialization)
            BasicSpsEvents basicEvents = JsonProvider.json().readValue(entry.payload(), BasicSpsEvents.class);

            // Find the specific event by ID and deliver it
            for (SpsEvent event : basicEvents.spsEvents()) {
                if (event.id().equals(entry.eventId())) {
                    LOG.info("Delivering event {} from transport queue to receiver", entry.eventId());
                    receiver.receive(event);
                    break;
                }
            }

            // Mark as processed
            database.markTransportProcessed(entry.eventId(), entry.subscriberId());
            LOG.debug("Marked event {} as processed for subscriber {}", 
                    entry.eventId(), entry.subscriberId());

        } catch (Exception e) {
            LOG.error("Error processing transport queue entry {}: {}", 
                    entry.eventId(), e.getMessage(), e);
        }
    }

    public boolean isRunning() {
        return running.get();
    }
}

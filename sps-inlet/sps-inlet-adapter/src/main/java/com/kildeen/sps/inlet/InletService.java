package com.kildeen.sps.inlet;

import com.kildeen.sps.ConfigurationException;
import com.kildeen.sps.IdWithReceipts;
import com.kildeen.sps.SpsEventType;
import com.kildeen.sps.SpsEvents;
import com.kildeen.sps.persistence.Database;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service for receiving and processing incoming events.
 *
 * <p>Note: Server-side circuit breakers have been removed. Clients should use
 * {@code ClientCircuitBreaker} for resilience when calling this service.
 *
 * <p>Supports both HTTP-based receiving and database polling for transport fallback.
 */
public class InletService implements Inlet {

    private final ReceiveEvent receiveEvent;
    private final TransportQueuePoller transportQueuePoller;

    private InletService(Builder builder) {
        receiveEvent = builder.receiveEvent;
        transportQueuePoller = builder.transportQueuePoller;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public IdWithReceipts receive(SpsEvents events) {
        List<IdWithReceipts.IdWithReceipt> result = new ArrayList<>();
        events.spsEvents().forEach(e -> result.add(new IdWithReceipts.IdWithReceipt(e.id(), e.type(),
                receiveEvent.receive(e),
                Instant.now())));
        return new IdWithReceipts(result);
    }

    /**
     * Stops the transport queue poller if running.
     */
    public void stop() {
        if (transportQueuePoller != null) {
            transportQueuePoller.stop();
        }
    }

    /**
     * Returns the transport queue poller for monitoring.
     */
    public TransportQueuePoller getTransportQueuePoller() {
        return transportQueuePoller;
    }

    public static final class Builder {
        private Collection<Receiver> spsReceivers;
        private Database database;
        private ReceiveEvent receiveEvent;
        private TransportQueuePoller transportQueuePoller;
        private String subId;
        private boolean enableTransportPolling = false;

        private Builder() {
        }

        public Builder withDatabase(Database database) {
            this.database = database;
            return this;
        }

        public Builder withReceivers(Collection<Receiver> receivers) {
            this.spsReceivers = receivers;
            return this;
        }

        public Builder withSubId(String subId) {
            this.subId = subId;
            return this;
        }

        /**
         * Enable or disable transport queue polling for database fallback.
         * Default is enabled.
         */
        public Builder withTransportPolling(boolean enabled) {
            this.enableTransportPolling = enabled;
            return this;
        }

        public InletService build() {
            validateConfiguration();

            Receiver healthcheckReceiver = new HealthCheckReceiver();

            // Map receivers directly by event type (no circuit breaker wrapper)
            Map<String, Receiver> mapped = Receiver.map(spsReceivers.stream());
            mapped.put(SpsEventType.healthcheck_01.toString(), healthcheckReceiver);

            receiveEvent = new ReceiveEvent(mapped,
                    new AckOrNackEvent(new RetryQueue(), new AckOrNackEventsImpl(database)),
                    database);

            // Create and start transport queue poller for database fallback
            if (enableTransportPolling) {
                transportQueuePoller = new TransportQueuePoller(database, subId, new ArrayList<>(spsReceivers));
                transportQueuePoller.start();
            }

            database.takeLeader(null);
            return new InletService(this);
        }

        private void validateConfiguration() {
            if (database == null) {
                throw new ConfigurationException(
                        "database",
                        "InletService requires a database",
                        "Call .withDatabase(database) on the builder"
                );
            }
            if (subId == null || subId.isBlank()) {
                throw new ConfigurationException(
                        "subscriberId",
                        "InletService requires a subscriber ID",
                        "Call .withSubId(\"your-subscriber-id\") on the builder"
                );
            }
            if (spsReceivers == null || spsReceivers.isEmpty()) {
                throw new ConfigurationException(
                        "receivers",
                        "InletService requires at least one receiver",
                        "Call .withReceivers(List.of(yourReceiver)) on the builder"
                );
            }
        }
    }
}

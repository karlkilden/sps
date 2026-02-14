package com.kildeen.sps.persistence;

import com.kildeen.sps.Receipt;
import com.kildeen.sps.Schemas;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.publish.Subscriptions;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface Database {
    void addSubscription(Subscriptions.Subscription subscription);

    Schemas schemas();

    Schemas.Schema schema(String eventType);

    void addSchema(Schemas.Schema schemaTuple);

    void ackOrNack(SpsEvent event, Receipt receipt);

    Subscriptions subscriptions(Set<String> eventTypes);

    Config fetchConfig();

    boolean isAck(String id);

    boolean isNack(String id);

    boolean isAbandoned(String s);

    int nackCount(String id);

    long firstNackInterval(String id);

    long firstNackToAck(String id);

    default Boolean isAck(String baseId, String subscriber) {
        return isAck(baseId + "_" + subscriber);
    }

    long nackCountByTypeSince(String eventType, Instant since);

    void tripCircuit(String subId, String eventType);

    void resetCircuit(String subId, String eventType);

    Map<String, Set<String>> trippedCircuits();

    boolean isTripped(String subId, String eventId);

    boolean takeLeader(UUID id);

    // Transport queue methods for database-based delivery fallback

    /**
     * Insert an event into the transport queue for database-based delivery.
     */
    default void insertTransportEvent(String eventId, String eventType, String subscriberId, String payload) {
        // Default no-op for backwards compatibility
    }

    /**
     * Poll pending events from the transport queue for a subscriber.
     */
    default List<TransportQueueEntry> pollTransportQueue(String subscriberId, int limit) {
        return List.of();
    }

    /**
     * Mark a transport queue entry as processed.
     */
    default void markTransportProcessed(String eventId, String subscriberId) {
        // Default no-op
    }

    /**
     * Delete processed entries older than the given instant.
     */
    default int cleanupTransportQueue(Instant olderThan) {
        return 0;
    }
}

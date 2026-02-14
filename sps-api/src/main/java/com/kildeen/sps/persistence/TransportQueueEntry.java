package com.kildeen.sps.persistence;

import java.time.Instant;

/**
 * Represents an entry in the transport queue for database-based event delivery.
 * Used when HTTP delivery fails and events are queued for polling.
 */
public record TransportQueueEntry(
        long id,
        String eventId,
        String eventType,
        String subscriberId,
        String payload,
        String status,
        Instant createdAt
) {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSED = "PROCESSED";
    public static final String STATUS_FAILED = "FAILED";
}

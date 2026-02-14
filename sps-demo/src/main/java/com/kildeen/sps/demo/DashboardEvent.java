package com.kildeen.sps.demo;

import java.time.Instant;
import java.util.Map;

/**
 * Event payload sent to dashboard clients via SSE.
 */
public record DashboardEvent(
        Direction direction,
        String eventId,
        String eventType,
        String timestamp,
        Map<String, Object> payload,
        Status status
) {
    public enum Direction {
        PUBLISHED, RECEIVED
    }

    public enum Status {
        SUCCESS, FAILED
    }

    public static DashboardEvent published(String eventId, String eventType, Map<String, Object> payload) {
        return new DashboardEvent(Direction.PUBLISHED, eventId, eventType, Instant.now().toString(), payload, Status.SUCCESS);
    }

    public static DashboardEvent received(String eventId, String eventType, Map<String, Object> payload, boolean success) {
        return new DashboardEvent(Direction.RECEIVED, eventId, eventType, Instant.now().toString(), payload,
                success ? Status.SUCCESS : Status.FAILED);
    }
}

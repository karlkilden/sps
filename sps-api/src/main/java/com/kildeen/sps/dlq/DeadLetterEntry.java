package com.kildeen.sps.dlq;

import com.kildeen.sps.SpsEvent;

import java.time.Instant;

/**
 * An entry in the dead letter queue containing the failed event and metadata.
 */
public record DeadLetterEntry(
        SpsEvent event,
        String reason,
        int retryCount,
        Instant timestamp
) {
}

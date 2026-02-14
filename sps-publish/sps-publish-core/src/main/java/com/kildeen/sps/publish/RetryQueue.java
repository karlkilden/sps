package com.kildeen.sps.publish;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread-safe retry queue for events awaiting retry.
 * Uses O(1) lookup for duplicate detection and atomic operations for thread safety.
 */
public class RetryQueue {

    private static final Logger LOG = LoggerFactory.getLogger(RetryQueue.class);

    private final ConcurrentLinkedQueue<TimestampedPublishableEvent> queue = new ConcurrentLinkedQueue<>();
    // O(1) lookup for duplicate detection instead of O(n) stream search
    private final Set<String> eventIds = ConcurrentHashMap.newKeySet();

    /**
     * Saves an event for retry if not already queued.
     *
     * @param event the event to retry
     * @param retryPolicy the retry policy to apply
     * @return true if saved, false if already in queue
     */
    public boolean save(PublishableEvent event, RetryPolicies.RetryPolicy retryPolicy) {
        // O(1) duplicate check using ConcurrentHashMap-backed Set
        if (!eventIds.add(event.id())) {
            LOG.debug("Event {} already in retry queue, skipping", event.id());
            return false;
        }

        TimestampedPublishableEvent stamped = new TimestampedPublishableEvent(
                event, Instant.now(), event.createdAt(), retryPolicy);
        queue.add(stamped);
        LOG.debug("Event {} added to retry queue", event.id());
        return true;
    }

    /**
     * Retrieves the next event ready for retry.
     * Uses atomic poll() to avoid race conditions between peek and poll.
     *
     * @return the next event if ready, null otherwise
     */
    public PublishableEvent next() {
        // Atomic poll - no race condition with peek/poll pattern
        TimestampedPublishableEvent polled = queue.poll();
        if (polled == null) {
            return null;
        }

        Duration elapsed = Duration.between(polled.saveForRetry, Instant.now());
        if (elapsed.toMillis() >= polled.retryPolicy().waitInMs()) {
            // Event is ready for retry - remove from ID tracking
            eventIds.remove(polled.event.id());
            LOG.debug("Event {} ready for retry after {} ms", polled.event.id(), elapsed.toMillis());
            return polled.event;
        } else {
            // Not ready yet - re-add to back of queue
            queue.add(polled);
            return null;
        }
    }

    /**
     * Returns the current queue size.
     */
    public int size() {
        return queue.size();
    }

    record TimestampedPublishableEvent(PublishableEvent event, Instant firstSeen, Instant saveForRetry,
                                       RetryPolicies.RetryPolicy retryPolicy) {
    }
}

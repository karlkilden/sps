package com.kildeen.sps.inlet;

import com.kildeen.sps.Receipt;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class RetryQueue {
    ConcurrentLinkedQueue<RetryAckOrNack> queue = new ConcurrentLinkedQueue<>();

    public void save(String id, Receipt receipt) {
        RetryAckOrNack stamped = new RetryAckOrNack(id, receipt, Instant.now(), new AtomicInteger());
        queue.add(stamped);
    }

    public void save(RetryAckOrNack retry) {
        queue.add(new RetryAckOrNack(retry.id,
                retry.receipt(),
                Instant.now(),
                new AtomicInteger(retry.retries.get())));
    }

    /**
     * Retrieves the next event ready for retry.
     * Uses atomic poll() to avoid race conditions between peek and poll.
     *
     * @return the next event if ready, null otherwise
     */
    public RetryAckOrNack next() {
        // Atomic poll - no race condition with peek/poll pattern
        RetryAckOrNack polled = queue.poll();
        if (polled == null) {
            return null;
        }

        Duration elapsed = Duration.between(polled.instant(), Instant.now());
        long requiredWaitSeconds = 5L * Math.max(1, polled.retries().get());

        if (elapsed.toSeconds() >= requiredWaitSeconds) {
            // Event is ready for retry
            return polled;
        } else {
            // Not ready yet - re-add to back of queue
            queue.add(polled);
            return null;
        }
    }

    public record RetryAckOrNack(String id, Receipt receipt, Instant instant, AtomicInteger retries) {
    }
}

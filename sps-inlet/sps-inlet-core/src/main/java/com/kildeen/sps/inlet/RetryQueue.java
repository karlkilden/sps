package com.kildeen.sps.inlet;

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

    record RetryAckOrNack(String id, Receipt receipt, Instant instant, AtomicInteger retries) {
    }

    public RetryAckOrNack next() {
        RetryAckOrNack peek = queue.peek();
        if (peek == null) {
            return null;
        }
        Duration res = Duration.between(peek.instant(), Instant.now());
        if (res.toSeconds() > 5L * peek.retries().get()) {
            RetryAckOrNack poll = queue.poll();
            if (peek == poll) {
                return poll;
            } else {
                queue.add(poll);
            }
        }
        return null;
    }
}

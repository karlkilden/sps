package com.kildeen.sps.publish;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RetryQueue {
    ConcurrentLinkedQueue<TimestampedPublishableEvent> queue = new ConcurrentLinkedQueue<>();

    public void save(PublishableEvent event) {
        TimestampedPublishableEvent stamped = new TimestampedPublishableEvent(event, Instant.now());
        queue.add(stamped);
    }

    record TimestampedPublishableEvent(PublishableEvent event, Instant instant) {
    }

    public PublishableEvent next() {
        TimestampedPublishableEvent peek = queue.peek();
        if (peek == null) {
            return null;
        }
        Duration res = Duration.between(peek.instant(), Instant.now());
        if (res.toSeconds() > 5L * peek.event.retries()) {
            TimestampedPublishableEvent poll = queue.poll();
            if (peek == poll) {
                return poll.event;
            } else {
                queue.add(poll);
            }
        }
        return null;
    }
}

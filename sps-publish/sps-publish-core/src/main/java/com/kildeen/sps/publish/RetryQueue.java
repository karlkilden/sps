package com.kildeen.sps.publish;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RetryQueue {

    ConcurrentLinkedQueue<TimestampedPublishableEvent> queue = new ConcurrentLinkedQueue<>();

    public boolean save(PublishableEvent event, RetryPolicies.RetryPolicy retryPolicy) {
        if (queue.stream().anyMatch(e -> e.event.id().equals(event.id()))) {
            return false;
        }
        TimestampedPublishableEvent stamped = new TimestampedPublishableEvent(event, Instant.now(), event.createdAt(), retryPolicy);
        queue.add(stamped);
        return true;
    }

    public PublishableEvent next() {
        TimestampedPublishableEvent peek = queue.peek();
        if (peek == null) {
            return null;
        }
        Duration res = Duration.between(peek.saveForRetry, Instant.now());

        if (res.toMillis() > peek.retryPolicy().waitInMs()) {
            TimestampedPublishableEvent poll = queue.poll();
            if (peek == poll) {
                return poll.event;
            } else {
                queue.add(poll);
            }
        }
        return null;
    }

    record TimestampedPublishableEvent(PublishableEvent event, Instant firstSeen, Instant saveForRetry,
                                       RetryPolicies.RetryPolicy retryPolicy) {
    }
}

package com.kildeen.embeddeddb;

import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.dlq.DeadLetterEntry;
import com.kildeen.sps.dlq.DeadLetterQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * In-memory implementation of DeadLetterQueue for testing and development.
 * Thread-safe and supports all DLQ operations.
 */
public class InMemoryDeadLetterQueue implements DeadLetterQueue {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDeadLetterQueue.class);

    private final ConcurrentLinkedDeque<DeadLetterEntry> queue = new ConcurrentLinkedDeque<>();
    private final ConcurrentHashMap<String, DeadLetterEntry> index = new ConcurrentHashMap<>();

    private static final InMemoryDeadLetterQueue INSTANCE = new InMemoryDeadLetterQueue();

    public static InMemoryDeadLetterQueue get() {
        return INSTANCE;
    }

    @Override
    public void send(SpsEvent event, String reason, int retryCount) {
        DeadLetterEntry entry = new DeadLetterEntry(event, reason, retryCount, Instant.now());

        // Avoid duplicates using index
        if (index.putIfAbsent(event.id(), entry) == null) {
            queue.addLast(entry);
            LOG.warn("Event {} sent to DLQ: {} (retry count: {})", event.id(), reason, retryCount);
        } else {
            LOG.debug("Event {} already in DLQ, skipping", event.id());
        }
    }

    @Override
    public List<DeadLetterEntry> peek(int limit) {
        return queue.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public boolean replay(String eventId) {
        DeadLetterEntry entry = index.remove(eventId);
        if (entry != null) {
            queue.remove(entry);
            LOG.info("Event {} replayed from DLQ", eventId);
            return true;
        }
        return false;
    }

    @Override
    public boolean purge(String eventId) {
        DeadLetterEntry entry = index.remove(eventId);
        if (entry != null) {
            queue.remove(entry);
            LOG.info("Event {} purged from DLQ", eventId);
            return true;
        }
        return false;
    }

    @Override
    public long count() {
        return queue.size();
    }

    /**
     * Clears all entries from the DLQ. For testing only.
     */
    public void clear() {
        queue.clear();
        index.clear();
    }
}

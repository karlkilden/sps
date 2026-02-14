package com.kildeen.embeddeddb;


import com.kildeen.sps.IdWithReceipts;
import com.kildeen.sps.Receipt;
import com.kildeen.sps.Schemas;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.persistence.Config;
import com.kildeen.sps.persistence.Database;
import com.kildeen.sps.persistence.TransportQueueEntry;
import com.kildeen.sps.publish.Retry;
import com.kildeen.sps.publish.Subscriptions;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * In-memory database implementation for testing and development.
 * Uses indexed lookups for O(1) performance on common operations.
 */
public class EmbeddedDatabase implements Database {

    private static final EmbeddedDatabase INSTANCE = new EmbeddedDatabase();

    /** Maximum receipts to keep (prevents unbounded memory growth) */
    private static final int MAX_RECEIPTS = 100_000;

    // Core data stores
    private final Queue<Subscriptions.Subscription> subs = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, Schemas.Schema> schemasByType = new ConcurrentHashMap<>();
    private final Map<Retry, AtomicInteger> retries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> trippedCircuitsBySubId = new ConcurrentHashMap<>();

    // Transport queue for database-based delivery fallback
    private final ConcurrentLinkedDeque<TransportQueueEntry> transportQueue = new ConcurrentLinkedDeque<>();
    private final AtomicInteger transportQueueIdGenerator = new AtomicInteger(0);

    // Receipt storage with bounded size and O(1) indexed lookup
    private final ConcurrentLinkedDeque<IdWithReceipts.IdWithReceipt> receipts = new ConcurrentLinkedDeque<>();
    private final ConcurrentHashMap<String, List<IdWithReceipts.IdWithReceipt>> receiptIndex = new ConcurrentHashMap<>();

    public static EmbeddedDatabase get() {
        return INSTANCE;
    }

    @Override
    public void addSubscription(Subscriptions.Subscription subscription) {
        subs.add(subscription);
    }

    @Override
    public Schemas schemas() {
        return new Schemas(new ArrayList<>(schemasByType.values()));
    }

    @Override
    public Schemas.Schema schema(String eventType) {
        return schemasByType.get(eventType);  // O(1) lookup
    }

    @Override
    public void addSchema(Schemas.Schema schema) {
        schemasByType.put(schema.eventType(), schema);  // O(1) upsert
    }

    @Override
    public void ackOrNack(SpsEvent event, Receipt receipt) {
        IdWithReceipts.IdWithReceipt entry = new IdWithReceipts.IdWithReceipt(
                event.id(), event.type(), receipt, Instant.now());

        // Add to bounded deque
        receipts.addLast(entry);

        // Add to index for O(1) lookup
        receiptIndex.computeIfAbsent(event.id(), k -> new ArrayList<>()).add(entry);

        // Enforce max size by removing oldest entries
        while (receipts.size() > MAX_RECEIPTS) {
            IdWithReceipts.IdWithReceipt removed = receipts.pollFirst();
            if (removed != null) {
                List<IdWithReceipts.IdWithReceipt> indexed = receiptIndex.get(removed.id());
                if (indexed != null) {
                    indexed.remove(removed);
                    if (indexed.isEmpty()) {
                        receiptIndex.remove(removed.id());
                    }
                }
            }
        }
    }

    @Override
    public Subscriptions subscriptions(Set<String> eventTypes) {
        return new Subscriptions(subs.stream()
                .filter(s -> eventTypes.contains(s.eventType()))
                .toList());
    }

    @Override
    public Config fetchConfig() {
        return new Config(new Config.SchemaGen("http://localhost:7201"));
    }

    @Override
    public boolean isAck(String id) {
        return findById(id).stream().anyMatch(is(Receipt.ACK));  // O(1) lookup
    }

    @Override
    public boolean isNack(String id) {
        return findById(id).stream().anyMatch(is(Receipt.NACK));  // O(1) lookup
    }

    @Override
    public boolean isAbandoned(String id) {
        return findById(id).stream().anyMatch(is(Receipt.ABANDONED));  // O(1) lookup
    }

    @Override
    public int nackCount(String id) {
        return (int) findById(id).stream().filter(is(Receipt.NACK)).count();
    }

    @Override
    public long firstNackInterval(String id) {
        List<IdWithReceipts.IdWithReceipt> nacks = findById(id).stream()
                .filter(is(Receipt.NACK))
                .toList();

        if (nacks.isEmpty()) {
            return -1;
        }
        if (nacks.size() == 1) {
            return 0;
        }
        return Duration.between(nacks.get(0).instant(), nacks.get(1).instant()).toMillis();
    }

    @Override
    public long firstNackToAck(String id) {
        List<IdWithReceipts.IdWithReceipt> entries = findById(id);

        Optional<IdWithReceipts.IdWithReceipt> firstNack = entries.stream()
                .filter(is(Receipt.NACK))
                .findFirst();

        Optional<IdWithReceipts.IdWithReceipt> ack = entries.stream()
                .filter(is(Receipt.ACK))
                .findFirst();

        if (firstNack.isEmpty() || ack.isEmpty()) {
            return -1;
        }
        return Duration.between(firstNack.get().instant(), ack.get().instant()).toMillis();
    }

    @Override
    public long nackCountByTypeSince(String eventType, Instant since) {
        // This still needs O(n) scan - could add type index if needed
        return receipts.stream()
                .filter(iwr -> iwr.instant().isAfter(since))
                .filter(iwr -> iwr.type().equals(eventType))
                .count();
    }

    @Override
    public void tripCircuit(String subId, String eventType) {
        trippedCircuitsBySubId.computeIfAbsent(subId, s -> new ConcurrentSkipListSet<>()).add(eventType);
    }

    @Override
    public void resetCircuit(String subId, String eventType) {
        Set<String> tripped = trippedCircuitsBySubId.get(subId);
        if (tripped != null) {
            tripped.remove(eventType);
        }
    }

    @Override
    public Map<String, Set<String>> trippedCircuits() {
        return trippedCircuitsBySubId;
    }

    @Override
    public boolean isTripped(String subId, String eventType) {
        Set<String> trippedTypes = trippedCircuitsBySubId.get(subId);
        return trippedTypes != null && trippedTypes.contains(eventType);
    }

    @Override
    public boolean takeLeader(UUID id) {
        return true;
    }

    // Transport queue methods

    @Override
    public void insertTransportEvent(String eventId, String eventType, String subscriberId, String payload) {
        // Check for duplicate (event_id + subscriber_id)
        boolean exists = transportQueue.stream()
                .anyMatch(e -> e.eventId().equals(eventId) && e.subscriberId().equals(subscriberId));
        if (!exists) {
            transportQueue.add(new TransportQueueEntry(
                    transportQueueIdGenerator.incrementAndGet(),
                    eventId,
                    eventType,
                    subscriberId,
                    payload,
                    TransportQueueEntry.STATUS_PENDING,
                    Instant.now()));
        }
    }

    @Override
    public List<TransportQueueEntry> pollTransportQueue(String subscriberId, int limit) {
        return transportQueue.stream()
                .filter(e -> e.subscriberId().equals(subscriberId))
                .filter(e -> TransportQueueEntry.STATUS_PENDING.equals(e.status()))
                .limit(limit)
                .toList();
    }

    @Override
    public void markTransportProcessed(String eventId, String subscriberId) {
        // Remove and re-add with PROCESSED status
        transportQueue.removeIf(e -> e.eventId().equals(eventId) && e.subscriberId().equals(subscriberId));
    }

    @Override
    public int cleanupTransportQueue(Instant olderThan) {
        int sizeBefore = transportQueue.size();
        transportQueue.removeIf(e ->
                TransportQueueEntry.STATUS_PROCESSED.equals(e.status()) &&
                e.createdAt().isBefore(olderThan));
        return sizeBefore - transportQueue.size();
    }

    /**
     * O(1) indexed lookup by event ID.
     */
    private List<IdWithReceipts.IdWithReceipt> findById(String id) {
        return receiptIndex.getOrDefault(id, List.of());
    }

    private Predicate<IdWithReceipts.IdWithReceipt> is(Receipt receipt) {
        return entry -> entry.receipt() == receipt;
    }

    /**
     * Clears all data. For testing only.
     */
    public void clear() {
        subs.clear();
        schemasByType.clear();
        receipts.clear();
        receiptIndex.clear();
        retries.clear();
        trippedCircuitsBySubId.clear();
        transportQueue.clear();
    }
}

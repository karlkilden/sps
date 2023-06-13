package com.kildeen.embeddeddb;


import com.kildeen.sps.IdWithReceipts;
import com.kildeen.sps.Receipt;
import com.kildeen.sps.Schemas;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.persistence.Config;
import com.kildeen.sps.persistence.Database;
import com.kildeen.sps.publish.Retry;
import com.kildeen.sps.publish.Subscriptions;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EmbeddedDatabase implements Database {
    private static final EmbeddedDatabase INSTANCE = new EmbeddedDatabase();
    Queue<Subscriptions.Subscription> subs = new ConcurrentLinkedQueue<>();
    Queue<Schemas.Schema> schemas = new ConcurrentLinkedQueue<>();
    Queue<IdWithReceipts.IdWithReceipt> idAndReceipts = new ConcurrentLinkedQueue<>();
    Map<Retry, AtomicInteger> retries = new ConcurrentHashMap<>();

    ConcurrentHashMap<String, Set<String>> trippedCircuitsBySubId = new ConcurrentHashMap<>();

    public static EmbeddedDatabase get() {
        return INSTANCE;
    }

    @Override
    public void addSubscription(Subscriptions.Subscription subscription) {
        subs.add(subscription);
    }

    @Override
    public Schemas schemas() {
        return new Schemas(new ArrayList<>(schemas));
    }

    @Override
    public Schemas.Schema schema(String eventType) {
        return schemas.stream().filter(s -> s.eventType().equals(eventType)).findFirst().orElse(null);
    }

    @Override
    public void addSchema(Schemas.Schema schema) {
        schemas.add(schema);
        schemas.stream()
                .filter(s -> s.eventType().equals(schema.eventType()) && s != schema)
                .findAny()
                .map(schemas::remove);

    }

    @Override
    public void ackOrNack(SpsEvent event, Receipt receipt) {
        idAndReceipts.add(new IdWithReceipts.IdWithReceipt(event.id(), event.type(), receipt, Instant.now()));
    }

    @Override
    public Subscriptions subscriptions(Set<String> eventTypes) {
        return new Subscriptions(new ArrayList<>(subs).stream().filter(s -> eventTypes.contains(s.eventType()))
                .toList());
    }

    @Override
    public Config fetchConfig() {
        return new Config(new Config.SchemaGen("http://localhost:7201"));
    }

    @Override
    public boolean isAck(String id) {
        return findById(id)
                .anyMatch(is(Receipt.ACK));
    }

    private Predicate<IdWithReceipts.IdWithReceipt> is(Receipt ack) {
        return idWithReceipt -> idWithReceipt.receipt() == ack;
    }

    private Stream<IdWithReceipts.IdWithReceipt> findById(String id) {
        return idAndReceipts.stream().filter(idWithReceipt -> idWithReceipt.id().equals(id));
    }

    @Override
    public boolean isAbandoned(String id) {
        return findById(id)
                .anyMatch(is(Receipt.ABANDONED));
    }

    @Override
    public int nackCount(String id) {
        return (int) findById(id)
                .filter(is(Receipt.NACK)).count();
    }

    @Override
    public long firstNackInterval(String id) {
        List<IdWithReceipts.IdWithReceipt> idWithReceipts =
                findById(id)
                        .filter(is(Receipt.NACK)).toList();

        if (idWithReceipts.isEmpty()) {
            return -1;
        }
        if (idWithReceipts.size() == 1) {
            return 0;
        } else {
            Duration d = Duration.between(idWithReceipts.get(0).instant(), idWithReceipts.get(1).instant());
            return d.toMillis();
        }
    }

    @Override
    public long firstNackToAck(String id) {
        Optional<IdWithReceipts.IdWithReceipt> firstNack =
                findById(id)
                        .filter(is(Receipt.NACK)).findFirst();

        Optional<IdWithReceipts.IdWithReceipt> ack =
                findById(id)
                        .filter(is(Receipt.ACK)).findFirst();

        if (firstNack.isEmpty()) {
            return -1;
        }
        if (ack.isEmpty()) {
            return -1;
        } else {
            Duration d = Duration.between(firstNack.get().instant(), ack.get().instant());
            return d.toMillis();
        }
    }

    @Override
    public long nackCountByTypeSince(String eventType, Instant since) {
        return idAndReceipts.stream()
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
        trippedCircuitsBySubId.computeIfAbsent(subId, s -> new ConcurrentSkipListSet<>()).remove(eventType);
    }
}

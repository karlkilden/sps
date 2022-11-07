package com.kildeen.embeddeddb;


import com.kildeen.sps.Database;
import com.kildeen.sps.IdAndReceiptTuple;
import com.kildeen.sps.ReceiptTuple;
import com.kildeen.sps.SchemaTuple;
import com.kildeen.sps.SubscriptionTuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class EmbeddedDatabase implements Database {
    Queue<SubscriptionTuple> subs = new ConcurrentLinkedQueue<>();
    Queue<SchemaTuple> schemas = new ConcurrentLinkedQueue<>();
    Queue<IdAndReceiptTuple> idAndReceipts = new ConcurrentLinkedQueue<>();

    private static final EmbeddedDatabase INSTANCE = new EmbeddedDatabase();

    public static EmbeddedDatabase get() {
        return INSTANCE;
    }

    @Override
    public void addSubscription(SubscriptionTuple subscriptionTuple) {
        subs.add(subscriptionTuple);
    }

    @Override
    public List<SchemaTuple> schemas() {
        return new ArrayList<>(schemas);
    }

    @Override
    public void addSchema(SchemaTuple schemaTuple) {
        schemas.add(schemaTuple);
    }

    @Override
    public void ackOrNack(String id, ReceiptTuple receipt) {
        idAndReceipts.add(new IdAndReceiptTuple(id, receipt));
    }

    public List<SubscriptionTuple> subscriptions(String eventType) {
        return new ArrayList<>(subs).stream().filter(s -> s.eventType().equals(eventType)).toList();
    }
}

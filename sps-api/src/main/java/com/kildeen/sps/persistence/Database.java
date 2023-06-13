package com.kildeen.sps.persistence;

import com.kildeen.sps.Receipt;
import com.kildeen.sps.Schemas;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.publish.Subscriptions;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;

public interface Database {
    void addSubscription(Subscriptions.Subscription subscription);

    Schemas schemas();

    Schemas.Schema schema(String eventType);

    void addSchema(Schemas.Schema schemaTuple);

    void ackOrNack(SpsEvent event, Receipt receipt);

    Subscriptions subscriptions(Set<String> eventTypes);

    Config fetchConfig();

    boolean isAck(String id);

    boolean isAbandoned(String s);

    int nackCount(String id);

    long firstNackInterval(String id);

    long firstNackToAck(String id);

    default Boolean isAck(String baseId, String subscriber) {
        return isAck(baseId + "_" + subscriber);
    }

    long nackCountByTypeSince(String eventType, Instant since);

    void tripCircuit(String subId, String eventType);

    void resetCircuit(String subId, String eventType);
}

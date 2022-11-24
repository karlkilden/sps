package com.kildeen.sps.persistence;

import com.kildeen.sps.Receipt;
import com.kildeen.sps.Schemas;
import com.kildeen.sps.publish.Subscriptions;

public interface Database {
    void addSubscription(Subscriptions.Subscription subscription);

    Schemas schemas();

    Schemas.Schema schema(String eventType);

    void addSchema(Schemas.Schema schemaTuple);

    void ackOrNack(String id, Receipt receipt);

    Subscriptions subscriptions(String eventType);

    Config fetchConfig();

    boolean isAck(String id);

    boolean isAbandoned(String s);

    int nackCount(String id);

    long firstNackInterval(String id);

    long firstNackToAck(String id);

    default Boolean isAck(String baseId, String subscriber) {
        return isAck(baseId + "_" + subscriber);
    }
}

package com.kildeen.sps;

import java.util.List;
import java.util.Map;

public interface Database {
    void addSubscription(SubscriptionTuple subscriptionTuple);

    List<SchemaTuple> schemas();

    void addSchema(SchemaTuple schemaTuple);

    void ackOrNack(String id, ReceiptTuple receipt);
}

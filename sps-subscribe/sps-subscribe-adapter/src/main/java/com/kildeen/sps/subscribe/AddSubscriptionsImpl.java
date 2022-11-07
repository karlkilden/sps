package com.kildeen.sps.subscribe;

import com.kildeen.sps.Database;
import com.kildeen.sps.SubscriptionTuple;

public class AddSubscriptionsImpl implements AddSubscriptions {

    private final Database database;

    public AddSubscriptionsImpl(Database database) {
        this.database = database;
    }

    @Override
    public void add(Subscription subscription) {
        database.addSubscription(new SubscriptionTuple(subscription.eventType(),
                subscription.url(),
                subscription.subId(),
                subscription.subSchema()));
    }
}

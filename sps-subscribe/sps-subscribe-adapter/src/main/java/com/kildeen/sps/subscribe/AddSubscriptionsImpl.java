package com.kildeen.sps.subscribe;

import com.kildeen.sps.persistence.DatabaseProvider;
import com.kildeen.sps.persistence.Database;
import com.kildeen.sps.publish.Subscriptions;

public class AddSubscriptionsImpl implements AddSubscriptions {

    private final Database database;

    public AddSubscriptionsImpl() {
        this.database = DatabaseProvider.database();
    }

    @Override
    public void add(Subscription subscription) {
        database.addSubscription(new Subscriptions.Subscription(
                new Subscriptions.Subscription.Subscriber(subscription.subId(), subscription.url()),
                subscription.eventType(),
                subscription.subSchema()
        ));
    }
}

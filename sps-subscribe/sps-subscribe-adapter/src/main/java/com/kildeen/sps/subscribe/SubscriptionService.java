package com.kildeen.sps.subscribe;

import com.kildeen.sps.SpsEvent;

public class SubscriptionService implements Subscribe {

    public static final SubscriptionService INSTANCE = new SubscriptionService();
    private AddSubscription addSubscription;

    public SubscriptionService inject() {
        addSubscription = new AddSubscription(new AddSubscriptionsImpl());
        return this;
    }

    @Override
    public void add(SpsEvent spsEvent) {
        addSubscription.add(spsEvent);
    }

}

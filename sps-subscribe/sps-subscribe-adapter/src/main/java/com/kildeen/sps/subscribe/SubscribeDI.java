package com.kildeen.sps.subscribe;

import com.kildeen.sps.SpsEvent;

public class SubscribeDI implements Subscribe {

    public static final SubscribeDI INSTANCE = new SubscribeDI();
    private AddSubscription addSubscription;

    public SubscribeDI inject() {
        addSubscription = new AddSubscription(new AddSubscriptionsImpl());
        return this;
    }

    @Override
    public void add(SpsEvent spsEvent) {
        addSubscription.add(spsEvent);
    }

}

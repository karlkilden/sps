package com.kildeen.sps.publish;


public class FetchSubscription {
    private final FetchSubscriptions fetchSubscriptions;

    FetchSubscription(FetchSubscriptions fetchSubscriptions) {
        this.fetchSubscriptions = fetchSubscriptions;
    }

    public Subscriptions fetch(String id) {
        return fetchSubscriptions.fetch(id);
    }
}
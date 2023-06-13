package com.kildeen.sps.publish;


import java.util.Set;

public class FetchSubscription {
    private final FetchSubscriptions fetchSubscriptions;

    FetchSubscription(FetchSubscriptions fetchSubscriptions) {
        this.fetchSubscriptions = fetchSubscriptions;
    }

    public Subscriptions fetch(Set<String> eventTypes) {
        return fetchSubscriptions.fetchSubscriptions(eventTypes);
    }

    public String fetchSchemaGenUrl() {
        return fetchSubscriptions.fetchSubscriptions();
    }
}
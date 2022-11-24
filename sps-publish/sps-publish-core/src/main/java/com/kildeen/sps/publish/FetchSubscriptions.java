package com.kildeen.sps.publish;

public interface FetchSubscriptions {
    Subscriptions fetchSubscriptions(String eventType);

    String fetchSubscriptions();
}

package com.kildeen.sps.publish;

import java.util.Set;

public interface FetchSubscriptions {
    Subscriptions fetchSubscriptions(Set<String> eventTypes);

    String fetchSubscriptions();
}

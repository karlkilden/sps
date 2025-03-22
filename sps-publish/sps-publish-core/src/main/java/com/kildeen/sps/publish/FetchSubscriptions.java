package com.kildeen.sps.publish;

import com.kildeen.sps.Bridge;

import java.util.Set;
@Bridge
public interface FetchSubscriptions {
    Subscriptions fetchSubscriptions(Set<String> eventTypes);

    String fetchSubscriptions();
}

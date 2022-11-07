package com.kildeen.sps.subscribe;

import java.util.List;
import java.util.Map;

record Subscriptions(List<Subscription> subscriptions) {
    record Subscription(Subscriber subscriber, String eventType, Map<String, String> schema) {
        record Subscriber(String url, String subId) {
        }
    }
}
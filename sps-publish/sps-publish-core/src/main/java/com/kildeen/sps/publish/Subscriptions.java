package com.kildeen.sps.publish;

import java.util.List;
import java.util.Map;

record Subscriptions(List<Subscription> subscriptions) {

    record Subscription(Subscriber subscriber, String eventType, Map<String, String> schema) {
        public String url() {
            return subscriber.url() + "/" + eventType;
        }

        record Subscriber(String url, String subId) {
        }
    }

}


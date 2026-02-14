package com.kildeen.sps.publish;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record Subscriptions(List<Subscription> subscriptions) {

    public boolean isEmpty() {
        return subscriptions().isEmpty();
    }

    public record Subscription(Subscriber subscriber, String eventType, Map<String, String> subSchema) {
        public String url() {
            return subscriber.resolveUrl() + "/receive-event/" + eventType;
        }

        public boolean refreshUrl() {
            return subscriber.refreshUrl();
        }

        public List<DeliveryType> deliveryType() {
            return subscriber.publishPolicy.acceptedDeliveryTypes();
        }

        public static class Subscriber {
            private static final Map<String, String> SUB_ID_TO_URL = new ConcurrentHashMap<>();
            private final String url;
            private final String subId;
            private final PublishPolicy publishPolicy;
            private boolean useRefreshed;


            public Subscriber(String subId, String url, PublishPolicy publishPolicy) {
                this.url = url;
                this.subId = subId;
                this.publishPolicy = publishPolicy;
                SUB_ID_TO_URL.put(subId, url);
            }

            public Subscriber(String subId, String url) {
                this(subId, url, new PublishPolicy(List.of(DeliveryType.HTTP, DeliveryType.DATABASE)));
            }

            public String resolveUrl() {
                return useRefreshed ? SUB_ID_TO_URL.get(subId) : url;
            }

            public String subId() {
                return subId;
            }

            public boolean refreshUrl() {
                String mappedUrl = SUB_ID_TO_URL.get(subId);
                if (mappedUrl != null && !this.url.equals(mappedUrl)) {
                    return useRefreshed = true;
                }
                return false;
            }
        }
    }

}
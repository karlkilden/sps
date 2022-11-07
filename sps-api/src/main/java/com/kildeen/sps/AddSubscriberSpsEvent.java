package com.kildeen.sps;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AddSubscriberSpsEvent implements AdvancedPublishSpsEvent {

    private final String requestedEventType;
    private final Map<String, String> subSchema;
    private final String url;
    private final String subId;

    public AddSubscriberSpsEvent(String requestedEventType,
                                 Map<String, String> subSchema,
                                 String url,
                                 String subId) {

        this.requestedEventType = requestedEventType;
        this.subSchema = subSchema;
        this.url = url;
        this.subId = subId;
    }

    @Override
    public String type() {
        return SpsSubscriberType.add_subscriber.toString();
    }

    @Override
    public Map<String, Object> data() {
        Map<String, Object> data = new HashMap<>();
        data.put("requestedEventType", requestedEventType);
        data.put("url", url);
        data.put("subId", subId);
        data.put("subSchema", subSchema);
        return data;
    }

    @Override
    public String description() {
        return """
    Publish this event to add a subscription.
    requestedEventType: eventType to subscribe to
    subSchema: custom key translation if any
    url: Url to receiver
    subId: the unique id for this subscriber
    """;
    }

    @Override
    public Set<String> tags() {
        return Set.of("sps");
    }

    public String requestedEventType() {
        return requestedEventType;
    }

    public Map<String, String> subSchema() {
        return subSchema;
    }

    public String url() {
        return url;
    }

    public String subId() {
        return subId;
    }

    public static AddSubscriberSpsEvent from(SpsEvent spsEvent) {
        String requestedEventType = (String) spsEvent.data().get("requestedEventType");
        String url = (String) spsEvent.data().get("url");
        String subId = (String) spsEvent.data().get("subId");
        @SuppressWarnings("unchecked") Map<String, String> subSchema =
                (Map<String, String>) spsEvent.data().get("subSchema");

        return new AddSubscriberSpsEvent(requestedEventType, subSchema, url, subId);
    }
}

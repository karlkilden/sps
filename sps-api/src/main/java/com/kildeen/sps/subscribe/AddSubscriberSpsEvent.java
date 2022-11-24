package com.kildeen.sps.subscribe;

import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEventType;
import com.kildeen.sps.publish.PublishSpsEvent;

import java.util.HashMap;
import java.util.Map;

public final class AddSubscriberSpsEvent implements PublishSpsEvent {
    private final String requestedEventType;
    private final Map<String, String> subSchema;
    private final String url;
    private final String subId;
    private final String id;

    public AddSubscriberSpsEvent(String requestedEventType,
                                 Map<String, String> subSchema,
                                 String url,
                                 String subId) {

        this.requestedEventType = requestedEventType;
        this.subSchema = subSchema;
        this.url = url;
        this.subId = subId;
        this.id = genId();
    }

    public static AddSubscriberSpsEvent from(SpsEvent spsEvent) {
        String requestedEventType = (String) spsEvent.data().get("requestedEventType");
        String url = (String) spsEvent.data().get("resolveUrl");
        String subId = (String) spsEvent.data().get("subId");
        @SuppressWarnings("unchecked") Map<String, String> subSchema =
                (Map<String, String>) spsEvent.data().get("subSchema");

        return new AddSubscriberSpsEvent(requestedEventType, subSchema, url, subId);
    }

    @Override
    public String type() {
        return SpsEventType.add_subscriber_01.toString();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Map<String, Object> data() {
        Map<String, Object> data = new HashMap<>();
        data.put("requestedEventType", requestedEventType);
        data.put("resolveUrl", url);
        data.put("subId", subId);
        data.put("subSchema", subSchema);
        return data;
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
}

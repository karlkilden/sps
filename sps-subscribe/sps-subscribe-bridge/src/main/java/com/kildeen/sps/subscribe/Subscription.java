package com.kildeen.sps.subscribe;

import java.util.Map;

public record Subscription(String eventType, String url, String subId, Map<String, String> subSchema) {
}

package com.kildeen.sps.subscription;

import java.util.HashMap;
import java.util.Map;

public record AddSubscriptionRequest(String eventType, String url, String subId, Map<String, String> subSchema) {
}

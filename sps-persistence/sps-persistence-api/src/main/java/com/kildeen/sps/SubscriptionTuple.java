package com.kildeen.sps;

import java.util.Map;

public record SubscriptionTuple(String eventType, String url, String subId, Map<String, String> subSchema) {
}
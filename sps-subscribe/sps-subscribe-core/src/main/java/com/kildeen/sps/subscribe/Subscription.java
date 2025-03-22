package com.kildeen.sps.subscribe;

import com.kildeen.sps.Bridge;

import java.util.Map;

@Bridge
public record Subscription(
        String eventType,
        String url,
        String subId,
        Map<String, String> subSchema
) {
}

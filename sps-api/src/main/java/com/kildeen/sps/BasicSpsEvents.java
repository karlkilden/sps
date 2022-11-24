package com.kildeen.sps;

import java.util.List;
import java.util.Map;

public record BasicSpsEvents(String eventType, List<BasicSpsEvent> spsEvents) {
    public SpsEvents get() {
        return new SpsEvents(this);
    }
    public record BasicSpsEvent(
            String type,
            String id,
            Map<String, Object> data) implements SpsEvent {
    }
}

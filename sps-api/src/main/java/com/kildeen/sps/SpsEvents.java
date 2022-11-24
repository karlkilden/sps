package com.kildeen.sps;

import java.util.List;

public record SpsEvents(String eventType, List<SpsEvent> spsEvents) {
    public SpsEvents(BasicSpsEvents events) {
        this(events.eventType(), events.spsEvents().stream().map(e -> (SpsEvent) e).toList());
    }
}

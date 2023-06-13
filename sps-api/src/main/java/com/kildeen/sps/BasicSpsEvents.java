package com.kildeen.sps;

import java.util.List;
import java.util.Map;

/**
 * Simple wrapper over multiple SpsEvents
 *
 * @param eventType Type for all events. Different types for different spsEvent passed in is not supported
 * @param spsEvents The actual events
 */
@Contract
public record BasicSpsEvents(String eventType, List<BasicSpsEvent> spsEvents) {
    /**
     * * A utility method as most signatures expects exactly that representation
     *
     * @return the the exact data but as the interface version.
     */
    public SpsEvents get() {
        return new SpsEvents(this);
    }

    /**
     * Simplistic SpsEvent implementation meant for deserialization
     *
     * @param type type
     * @param id   id
     * @param data data
     */
    public record BasicSpsEvent(
            String type,
            String id,
            Map<String, Object> data) implements SpsEvent {
    }
}

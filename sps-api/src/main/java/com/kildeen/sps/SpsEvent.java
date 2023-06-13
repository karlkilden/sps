package com.kildeen.sps;

import com.kildeen.sps.publish.PublishSpsEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The core representation of an event.
 * <br>
 * See: {@link BasicSpsEvents} for a basic implementation that can be used for deserializing SpsEvents
 * <br>
 * See: {@link com.kildeen.sps.publish.PublishSpsEvent} for a built-in specialized version, that allows
 * automatic publishing of the same event under as different types.
 * <br>
 * See: {@link SpsEvents} for a handy wrapper over many events that also have an utility constructor,
 * that accepts {@link BasicSpsEvents}
 */
@Contract
public interface SpsEvent {

    static Set<String> resolveTypes(Collection<SpsEvent> events) {
        Set<String> eventTypes = new HashSet<>();
        events.forEach(e -> {
            if (e instanceof PublishSpsEvent publishSpsEvent) {
                eventTypes.addAll(publishSpsEvent.types());
            }
            eventTypes.add(e.type());
        });
        return eventTypes;
    }

    /**
     * @return The unique identifier for this type of event.
     * Meaning, SPS will treat all events with the same type as the same type of event.
     * SPS convention is short and versioned strings such as 'new_customer_01'
     * The size constraints for a type is undefined and large, but it's recommended to not surpass a UUID in size
     */
    String type();

    /**
     * @return Unique id for this specific event. See {@link SpsEvent#genId()}
     */
    String id();

    /**
     * @return Generates a new unique id based on:
     * <p>
     * type() + "_" + UUID.randomUUID() + "_" + System.currentTimeMillis()
     * </p>
     * Meant to be used to assign an id field in the constructor e.g. id = genId()
     */
    default String genId() {
        return type() + "_" + UUID.randomUUID() + "_" + System.currentTimeMillis();
    }

    /**
     * @return the actual data that the event consists of.
     */
    Map<String, Object> data();

}

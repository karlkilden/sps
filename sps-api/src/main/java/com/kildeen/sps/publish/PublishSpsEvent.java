package com.kildeen.sps.publish;

import com.kildeen.sps.SpsEvent;

import java.util.List;
import java.util.Map;

public interface PublishSpsEvent extends SpsEvent {

    /**
     * When publishing an event, it may be desired to send identical events on as different types
     * This is to support cases where e.g. subscriber is expected to upgrade to a newer version of the event
     * but for a limited period, both the old and the new event type needs to be supported.
     * @return All types this event should be sent as
     */
    default List<String> types() {
        return List.of(type());
    }

}

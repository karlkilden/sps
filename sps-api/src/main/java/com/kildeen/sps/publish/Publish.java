package com.kildeen.sps.publish;

import com.kildeen.sps.Contract;
import com.kildeen.sps.SpsEvent;

import java.util.Collection;

@Contract
public interface Publish {

    /**
     *
     * @param events that all share the same type()
     *               A mix of different types is not allowed
     * @return the publish result
     */
    PublishResult publish(Collection<SpsEvent> events);

    enum PublishResult {
        SCHEMA_GEN_PUBLISH, PUBLISH
    }
}

package com.kildeen.sps.publish;

import com.kildeen.sps.SpsEvent;

import java.util.Collection;

public interface Publish {
    PublishResult publish(String type, Collection<SpsEvent> events);

    enum PublishResult {
        SCHEMA_GEN_PUBLISH, PUBLISH
    }
}

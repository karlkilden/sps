package com.kildeen.sps;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface SpsEvent {
    String type();

    default String id() {
        return type() + "_" + UUID.randomUUID() + "_" + System.currentTimeMillis();
    }

    Map<String, Object> data();

}

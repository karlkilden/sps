package com.kildeen.sps;

import java.util.Map;
import java.util.UUID;

public interface SpsEvent {
    String type();

    String id();

    default String genId() {
        return type() + "_" + UUID.randomUUID() + "_" + System.currentTimeMillis();
    }

    Map<String, Object> data();

}

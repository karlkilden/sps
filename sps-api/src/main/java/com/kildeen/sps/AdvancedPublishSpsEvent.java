package com.kildeen.sps;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface AdvancedPublishSpsEvent extends SpsEvent {

    String type();

    default List<String> types() {
        return List.of(type());
    }

    default String id() {
        return type() + "_" + UUID.randomUUID() + "_" + System.currentTimeMillis();
    }

    Map<String, Object> data();

    default Map<String, Map<String, Object>> dataBySubId() {
        return Map.of(SpsSubscriberType.schema_gen.toString(),
                Map.of("description" + UniqueKey.KEY, description(),
                        "tags" + UniqueKey.KEY, tags()
                )
        );
    }

    default String description() {
        return "N/A";
    }

    default Set<String> tags() {
        return Set.of("default");
    }


}

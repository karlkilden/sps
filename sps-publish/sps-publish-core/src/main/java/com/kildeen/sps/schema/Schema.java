package com.kildeen.sps.schema;

import java.util.List;
import java.util.Set;

public record Schema(String eventType, String description, List<String> keySchema, Set<String> tags) {
    public Schema(PublishSchemaTuple schemaTuple) {
        this(schemaTuple.eventType(), schemaTuple.description(), schemaTuple.keySchema(), schemaTuple.tags());
    }
}

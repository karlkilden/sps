package com.kildeen.sps;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record Schemas(List<Schema> schemas) {

    public record Schema(String eventType, String eventDocumentation,
                         Map<String, String> fieldDocumentation,
                         Set<String> tags, int version) {
    }
}

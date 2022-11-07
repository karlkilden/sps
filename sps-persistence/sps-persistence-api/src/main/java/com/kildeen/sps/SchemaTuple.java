package com.kildeen.sps;

import java.util.List;
import java.util.Set;

public record SchemaTuple(String eventType, String description, List<String> keySchema, Set<String> tags) {
}

package com.kildeen.sps.schema;

import java.util.List;
import java.util.Set;

public record PublishSchemaTuple(String eventType, String description, List<String> keySchema, Set<String> tags) {

}

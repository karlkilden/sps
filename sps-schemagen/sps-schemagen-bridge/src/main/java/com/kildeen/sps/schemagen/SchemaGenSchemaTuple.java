package com.kildeen.sps.schemagen;

import java.util.List;
import java.util.Set;

public record SchemaGenSchemaTuple(String eventType, String description, List<String> keySchema, Set<String> tags) {

}

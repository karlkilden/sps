package com.kildeen.sps.schemagen;

import com.kildeen.sps.Bridge;

import java.util.List;
import java.util.Set;
@Bridge
public record SchemaGenSchemaTuple(String eventType, String description, List<String> keySchema, Set<String> tags) {

}

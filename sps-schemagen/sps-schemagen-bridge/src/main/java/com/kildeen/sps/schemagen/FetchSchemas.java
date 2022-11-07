package com.kildeen.sps.schemagen;

import java.util.List;
import java.util.Set;

public interface FetchSchemas {
    List<SchemaGenSchemaTuple> fetch(Set<String> tags);
}

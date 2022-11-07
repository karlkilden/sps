package com.kildeen.sps.schemagen;

import com.kildeen.sps.SpsEvent;

import java.util.List;
import java.util.Set;

public class FetchSchema {

    private final FetchSchemas fetchSchemas;

    FetchSchema(FetchSchemas fetchSchemas) {
        this.fetchSchemas = fetchSchemas;
    }

    List<Schema> fetch(SpsEvent event) {
        @SuppressWarnings("unchecked") Set<String> tagSet = (Set<String>) event.data().get("tags");
            List<SchemaGenSchemaTuple> schemas = fetchSchemas.fetch(event.data().get("tags") == null ? Set.of()
                    : tagSet);
            return schemas.stream()
                    .map(Schema::new).toList();

    }
}

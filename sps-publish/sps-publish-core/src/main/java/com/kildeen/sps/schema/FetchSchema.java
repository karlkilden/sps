package com.kildeen.sps.schema;

import java.util.List;

class FetchSchema {
//TODO: add annotation/interface to describe event with tags and description

    private final FetchSchemas fetchSchemas;

    FetchSchema(FetchSchemas fetchSchemas) {
        this.fetchSchemas = fetchSchemas;
    }

    List<Schema> fetch(List<String> tags) {
        List<PublishSchemaTuple> schemas = fetchSchemas.fetch(tags);
        return schemas.stream()
                .map(Schema::new).toList();
    }
}
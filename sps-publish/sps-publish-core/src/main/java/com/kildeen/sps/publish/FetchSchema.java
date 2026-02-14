package com.kildeen.sps.publish;

import com.kildeen.sps.Schemas;

public class FetchSchema {
    private final FetchSchemas fetchSchemas;

    public FetchSchema(FetchSchemas fetchSchemas) {
        this.fetchSchemas = fetchSchemas;
    }

    Schemas.Schema fetch(String eventType) {
        return fetchSchemas.fetch(eventType);
    }
}
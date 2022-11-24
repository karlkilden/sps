package com.kildeen.sps.schemagen;

import com.kildeen.sps.Schemas;
import com.kildeen.sps.SpsEvent;

import java.util.Set;

public class FetchSchema {

    private final FetchSchemas fetchSchemas;

    FetchSchema(FetchSchemas fetchSchemas) {
        this.fetchSchemas = fetchSchemas;
    }

    Schemas fetch(SpsEvent event) {
        @SuppressWarnings("unchecked") Set<String> tagSet = (Set<String>) event.data().get("tags");
        return fetchSchemas.fetch(tagSet);

    }
}

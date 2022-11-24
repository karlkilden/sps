package com.kildeen.sps.publish;

import com.kildeen.sps.Schemas;
import com.kildeen.sps.persistence.Database;

public class FetchSchemasImpl implements FetchSchemas {

    private final Database database;

    FetchSchemasImpl(Database database) {
        this.database = database;
    }

    @Override
    public Schemas.Schema fetch(String eventType) {
        return database.schema(eventType);
    }
}

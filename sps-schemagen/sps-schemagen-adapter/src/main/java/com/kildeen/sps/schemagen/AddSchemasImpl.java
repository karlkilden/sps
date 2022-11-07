package com.kildeen.sps.schemagen;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.SchemaTuple;

public class AddSchemasImpl implements AddSchemas {

    @Override
    public void add(Schema schema) {
        SchemaTuple schemaTuple = new SchemaTuple(schema.eventType(),
                schema.description(), schema.keySchema(), schema.tags());
        EmbeddedDatabase embeddedDatabase = EmbeddedDatabase.get();
        embeddedDatabase.addSchema(schemaTuple);
    }
}

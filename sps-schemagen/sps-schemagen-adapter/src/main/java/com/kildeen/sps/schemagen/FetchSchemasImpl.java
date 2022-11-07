package com.kildeen.sps.schemagen;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.Database;
import com.kildeen.sps.SchemaTuple;
import com.kildeen.sps.schema.PublishSchemaTuple;

import javax.xml.crypto.Data;
import java.util.List;
import java.util.Set;

public class FetchSchemasImpl implements FetchSchemas {
    @Override
    public List<SchemaGenSchemaTuple> fetch(Set<String> tags) {
        EmbeddedDatabase db = EmbeddedDatabase.get();
        return db.schemas().stream()
                .map(s -> new SchemaGenSchemaTuple(s.eventType(), s.description(), s.keySchema(), s.tags()))
                .toList();
    }
}

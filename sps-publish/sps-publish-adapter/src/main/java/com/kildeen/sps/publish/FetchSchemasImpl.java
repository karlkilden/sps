package com.kildeen.sps.publish;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.schema.FetchSchemas;
import com.kildeen.sps.schema.PublishSchemaTuple;

import java.util.List;

public class FetchSchemasImpl implements FetchSchemas {

    FetchSchemasImpl() {
    }

    @Override
    public List<PublishSchemaTuple> fetch(List<String> tags) {
        EmbeddedDatabase db = EmbeddedDatabase.get();
        return db.schemas().stream()
                .map(s -> new PublishSchemaTuple(s.eventType(), s.description(), s.keySchema(), s.tags()))
                .toList();
    }
}

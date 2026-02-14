package com.kildeen.sps.schemagen;

import com.kildeen.sps.Schemas;
import com.kildeen.sps.persistence.DatabaseProvider;
import com.kildeen.sps.persistence.Database;

import java.util.Collections;
import java.util.Set;

public class FetchSchemasImpl implements FetchSchemas {

    private final Database database;

    public FetchSchemasImpl() {
        this.database = DatabaseProvider.database();
    }


    @Override
    public Schemas fetch(Set<String> tags) {
        return new Schemas(database.schemas().schemas().stream()
                .filter(s -> !Collections.disjoint(tags, s.tags())).toList());
    }
}

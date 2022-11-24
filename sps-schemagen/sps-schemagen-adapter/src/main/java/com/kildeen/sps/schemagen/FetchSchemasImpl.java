package com.kildeen.sps.schemagen;

import com.kildeen.sps.Schemas;
import com.kildeen.sps.persistence.DataBaseProvider;
import com.kildeen.sps.persistence.Database;

import java.util.Collections;
import java.util.Set;

public class FetchSchemasImpl implements FetchSchemas {

    private final Database database;

    public FetchSchemasImpl() {
        this.database = DataBaseProvider.database();
    }


    @Override
    public Schemas fetch(Set<String> tags) {
        return new Schemas(database.schemas().schemas().stream()
                .filter(s -> !Collections.disjoint(tags, s.tags())).toList());
    }
}

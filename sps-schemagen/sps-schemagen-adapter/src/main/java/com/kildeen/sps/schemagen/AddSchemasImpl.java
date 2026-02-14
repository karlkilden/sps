package com.kildeen.sps.schemagen;

import com.kildeen.sps.Schemas;
import com.kildeen.sps.persistence.DatabaseProvider;
import com.kildeen.sps.persistence.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class AddSchemasImpl implements AddSchemas {

    static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Database database;

    public AddSchemasImpl() {
        this.database = DatabaseProvider.database();
    }

    @Override
    public void add(Schemas.Schema schema) {
        LOG.info("Schema add:{}", schema);
        database.addSchema(schema);
        LOG.info("Schema state:{}", database.schemas());
    }
}

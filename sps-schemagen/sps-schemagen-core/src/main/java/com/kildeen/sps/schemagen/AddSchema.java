package com.kildeen.sps.schemagen;


import com.kildeen.sps.Schemas;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.json.JsonProvider;

public class AddSchema {

    private final AddSchemas addSchemas;

    AddSchema(AddSchemas addSchemas) {
        this.addSchemas = addSchemas;
    }

    void add(SpsEvent event) {
        Schemas.Schema schema = JsonProvider.json().readValue((String) event.data().get("json"), Schemas.Schema.class);
        addSchemas.add(schema);
    }
}

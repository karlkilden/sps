package com.kildeen.sps.schemagen;


import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.UniqueKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AddSchema {

    private final AddSchemas addSchemas;

    AddSchema(AddSchemas addSchemas) {
        this.addSchemas = addSchemas;
    }

    void add(SpsEvent event) {
        String description = (String) event.data().get("description" + UniqueKey.KEY);
        @SuppressWarnings("unchecked") Set<String> tags = (Set<String>) event.data().get("tags" + UniqueKey.KEY);
        List<String> keys = new ArrayList<>(event.data().keySet());
        keys.remove("description" + UniqueKey.KEY);
        keys.remove("tags" + UniqueKey.KEY);
        Schema schema = new Schema(event.type(), description, keys, tags);
        addSchemas.add(schema);
    }
}

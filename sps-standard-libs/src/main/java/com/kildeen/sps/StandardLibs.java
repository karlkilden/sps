package com.kildeen.sps;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.json.JsonProvider;
import com.kildeen.sps.persistence.DataBaseProvider;
import json.JacksonJson;

public class StandardLibs {

    public static void configure() {
        JsonProvider.configure(new JacksonJson());
        DataBaseProvider.configure(EmbeddedDatabase.get());
    }
}

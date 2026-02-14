package com.kildeen.sps;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.json.JsonProvider;
import com.kildeen.sps.persistence.DatabaseProvider;
import com.kildeen.sps.json.JacksonJson;

public class StandardLibs {

    public static void configure() {
        JsonProvider.configure(new JacksonJson());
        DatabaseProvider.configure(EmbeddedDatabase.get());
    }
}

package com.kildeen.sps.schemagen;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.persistence.DataBaseProvider;
import com.kildeen.sps.publish.Publish;
import com.kildeen.sps.publish.PublishDI;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

@Tag("systest")
public class SchemaGenIT {
    static {
        DataBaseProvider.configure(EmbeddedDatabase.get());
    }

    @Test
    void name() {
        Publish publish = PublishDI.newBuilder().withDatabase(DataBaseProvider.database()).build();
        publish.publish(List.of(new SpsEvent() {
            private String id = genId();

            @Override
            public String type() {
                return "schemagen_test01";
            }

            @Override
            public String id() {
                return id;
            }

            @Override
            public Map<String, Object> data() {
                return Map.of("other_value", 10);
            }
        }));
    }
}

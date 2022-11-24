package com.kildeen.sps.schemagen;

import com.kildeen.sps.Schemas;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEventType;
import com.kildeen.sps.json.JsonProvider;
import com.kildeen.sps.publish.Publish;

import java.util.List;
import java.util.Map;

public class PublishSchemasImpl implements PublishSchemas {
    private final Publish publish;

    public PublishSchemasImpl(Publish publish) {
        this.publish = publish;
    }

    @Override
    public void publish(SpsEvent spsEvent, Schemas schemas) {
        List<SpsEvent> events = schemas.schemas().stream().map(schema -> new SpsEvent() {
            private final String id = genId();

            @Override
            public String type() {
                return schema.eventType();
            }

            @Override

            public String id() {
                return id;
            }

            @Override
            public Map<String, Object> data() {
                return Map.of("json", JsonProvider.json().write(schema));
            }
        }).map(e -> (SpsEvent) e).toList();
        publish.publish(SpsEventType.list_schemas_01.toString(), events);
    }
}

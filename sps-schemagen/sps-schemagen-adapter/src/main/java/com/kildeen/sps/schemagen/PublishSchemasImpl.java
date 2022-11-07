package com.kildeen.sps.schemagen;

import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEventType;
import com.kildeen.sps.publish.Publish;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PublishSchemasImpl implements PublishSchemas {

    private final Publish publish;

    public PublishSchemasImpl(Publish publish) {
        this.publish = publish;
    }

    @Override
    public void publish(SpsEvent spsEvent, List<Schema> schemas) {
        AtomicInteger atomicInteger = new AtomicInteger();
        Collection<SpsEvent> events = schemas.stream().map(schema -> {
            //TODO: make ; illegal in format
            String keys = String.join(";", schema.keySchema());
            String tags = String.join(";", schema.tags());
            Map<String, Object> data = new HashMap<>();
            data.put("keys", keys);
            data.put("tags", tags);
            data.put("description", schema.description());
            data.put("type", schema.eventType());
            data.put("requestId", spsEvent.id());
            data.put("row", atomicInteger.getAndIncrement());
            data.put("rowCount", schemas.size());
            return (SpsEvent) new Event(data);
        }).toList();
        publish.publish(SpsEventType.list_schemas_01.toString(), events);
    }

    record Event(Map<String, Object> data) implements SpsEvent {
        @Override
        public String type() {
            return SpsEventType.list_schemas_01.toString();
        }

        @Override
        public Map<String, Object> data() {
            return data;
        }
    }
}

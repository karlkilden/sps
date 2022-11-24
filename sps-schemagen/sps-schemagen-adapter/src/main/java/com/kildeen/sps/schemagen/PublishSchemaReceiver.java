package com.kildeen.sps.schemagen;

import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEventType;
import com.kildeen.sps.inlet.Receiver;

public class PublishSchemaReceiver implements Receiver {

    private final PublishSchema publishSchema;

    public PublishSchemaReceiver(PublishSchema publishSchema) {
        this.publishSchema = publishSchema;
    }

    @Override
    public void receive(SpsEvent spsEvent) {
        publishSchema.receive(spsEvent);
    }

    @Override
    public String eventType() {
        return SpsEventType.list_schemas_01.toString();
    }
}

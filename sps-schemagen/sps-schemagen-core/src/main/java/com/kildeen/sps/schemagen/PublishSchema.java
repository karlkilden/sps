package com.kildeen.sps.schemagen;

import com.kildeen.sps.Schemas;
import com.kildeen.sps.SpsEvent;

public class PublishSchema {
    private final FetchSchema fetchSchema;
    private final PublishSchemas publishSchemas;

    public PublishSchema(FetchSchema fetchSchema, PublishSchemas publishSchemas) {
        this.fetchSchema = fetchSchema;
        this.publishSchemas = publishSchemas;
    }

    public void receive(SpsEvent spsEvent) {
        Schemas fetched = fetchSchema.fetch(spsEvent);
        publishSchemas.publish(spsEvent, fetched);
    }
}

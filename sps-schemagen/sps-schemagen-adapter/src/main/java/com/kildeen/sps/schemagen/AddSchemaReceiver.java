package com.kildeen.sps.schemagen;

import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEventType;
import com.kildeen.sps.inlet.Receiver;

public class AddSchemaReceiver implements Receiver {

    private final AddSchema addSchema;

    public AddSchemaReceiver(AddSchema addSchema) {
        this.addSchema = addSchema;
    }

    @Override
    public void receive(SpsEvent spsEvent) {
        addSchema.add(spsEvent);
    }

    @Override
    public String eventType() {
        return SpsEventType.add_schema_01.toString();
    }
}

package com.kildeen.sps.schemagen;

import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEventType;
import com.kildeen.sps.inlet.Receiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class AddSchemaReceiver implements Receiver {
    static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final AddSchema addSchema;

    public AddSchemaReceiver(AddSchema addSchema) {
        this.addSchema = addSchema;
    }

    @Override
    public void receive(SpsEvent spsEvent) {
        LOG.info("Event received {}", spsEvent);
        addSchema.add(spsEvent);
    }

    @Override
    public String eventType() {
        return SpsEventType.add_schema_01.toString();
    }
}

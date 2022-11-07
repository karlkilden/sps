package com.kildeen.sps.schemagen;

import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEvents;
import com.kildeen.sps.inlet.Inlet;
import com.kildeen.sps.inlet.InletDI;

import java.util.Set;

public class SchemagenResource {
    private final Inlet inlet;
    public SchemagenResource(Inlet inlet) {
        this.inlet = inlet;
    }

    public String handle(SpsEvents events) {
        inlet.receive(events);
        return "";
    }

    public final String receiveEndpoint (){
        return "/receive-event/{type}";
    }
}

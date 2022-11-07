package com.kildeen.sps.subscribe;

import com.kildeen.sps.SpsEvents;
import com.kildeen.sps.inlet.Inlet;

public class AddSubscriptionResource {
    private final Inlet inlet;
    public AddSubscriptionResource(Inlet inlet) {
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

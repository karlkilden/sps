package com.kildeen.sps.subscribe;

import com.kildeen.sps.IdWithReceipts;
import com.kildeen.sps.SpsEvents;
import com.kildeen.sps.inlet.Inlet;

public class AddSubscriptionResource {
    private final Inlet inlet;

    public AddSubscriptionResource(Inlet inlet) {
        this.inlet = inlet;
    }

    public IdWithReceipts handle(SpsEvents events) {
        return inlet.receive(events);
    }

    public final String receiveEndpoint() {
        return "/receive-event/{type}";
    }
}

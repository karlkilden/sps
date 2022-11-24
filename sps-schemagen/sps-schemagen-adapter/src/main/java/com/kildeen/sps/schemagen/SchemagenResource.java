package com.kildeen.sps.schemagen;

import com.kildeen.sps.IdWithReceipts;
import com.kildeen.sps.IdWithReceiptsResponse;
import com.kildeen.sps.SpsEvents;
import com.kildeen.sps.inlet.Inlet;

public class SchemagenResource {
    private final Inlet inlet;

    public SchemagenResource(Inlet inlet) {
        this.inlet = inlet;
    }

    public IdWithReceiptsResponse handle(SpsEvents events) {
        IdWithReceipts idWithReceipts = inlet.receive(events);
        return idWithReceipts.toResponse();
    }

    public final String receiveEndpoint() {
        return "/receive-event/{type}";
    }
}

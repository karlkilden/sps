package com.kildeen.sps.inlet;

import com.kildeen.sps.Database;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEvents;
import org.jdbi.v3.core.Jdbi;

import java.util.Map;

public class InletDI implements Inlet {

    public static final InletDI INSTANCE = new InletDI();
    private ReceiveEvent receiveEvent;

    public Inlet inject(Map<String, Receiver> spsReceivers, Database database) {
        receiveEvent = new ReceiveEvent(spsReceivers,
                new AckOrNackEvent(new RetryQueue(), new AckOrNackEventsImpl(database)));
        return this;
    }

    @Override
    public void receive(SpsEvents events) {
        events.spsEvents().forEach(receiveEvent::receive);
    }
}

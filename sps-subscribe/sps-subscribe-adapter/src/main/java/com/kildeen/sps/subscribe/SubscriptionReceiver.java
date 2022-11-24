package com.kildeen.sps.subscribe;

import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEventType;
import com.kildeen.sps.inlet.Receiver;

public class SubscriptionReceiver implements Receiver {

    private final Subscribe subscribe;

    public SubscriptionReceiver(Subscribe subscribe) {
        this.subscribe = subscribe;
    }

    @Override
    public void receive(SpsEvent spsEvent) {
        subscribe.add(spsEvent);
    }

    @Override
    public String eventType() {
        return SpsEventType.add_subscriber_01.toString();
    }
}

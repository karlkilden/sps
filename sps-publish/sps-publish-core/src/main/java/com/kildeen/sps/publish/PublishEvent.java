package com.kildeen.sps.publish;

import com.kildeen.sps.SpsEvent;

import java.util.Collection;

public class PublishEvent {

    Publisher publisher;


    public PublishEvent(Publisher publisher) {
        this.publisher = publisher;
    }

    void publish(Subscriptions subscriptions, Collection<SpsEvent> spsEvents) {
        publisher.publish(subscriptions, spsEvents);
    }
}
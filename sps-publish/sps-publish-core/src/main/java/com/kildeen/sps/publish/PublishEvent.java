package com.kildeen.sps.publish;

import com.kildeen.sps.SpsEvent;

import java.util.Collection;

public class PublishEvent {

    Publisher publisher;
    FetchSubscription fetchSubscription;

    public PublishEvent(Publisher publisher, FetchSubscription fetchSubscription) {
        this.publisher = publisher;
        this.fetchSubscription = fetchSubscription;
    }

    void publish(String type, Collection<SpsEvent> spsEvents) {
        publisher.publish(fetchSubscription.fetch(type), spsEvents);
    }
}

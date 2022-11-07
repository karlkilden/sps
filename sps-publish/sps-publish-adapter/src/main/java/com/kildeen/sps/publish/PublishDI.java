package com.kildeen.sps.publish;

import com.kildeen.sps.SpsEvent;
import org.jdbi.v3.core.Jdbi;

import java.util.Collection;

public class PublishDI implements Publish {

    public static final PublishDI INSTANCE = new PublishDI();
    private FetchSubscription fetchSubscription;
    private PublishEvent publishEvent;

    public PublishDI inject(Jdbi jdbi) {
        Publisher publisher = new Publisher(new HttpSender(), new RetryQueue());
        fetchSubscription = new FetchSubscription(new FetchSubscriptionsImpl());
        publishEvent = new PublishEvent(publisher, fetchSubscription);
        return this;
    }
    @Override
    public void publish(String type, Collection<SpsEvent> events) {
        publishEvent.publish(type, events);
    }
}

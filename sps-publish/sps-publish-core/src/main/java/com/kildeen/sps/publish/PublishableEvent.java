package com.kildeen.sps.publish;

import com.kildeen.sps.SpsEvent;

import java.time.Instant;
import java.util.List;

public sealed interface PublishableEvent permits EventFork.ForkedEvents.Fork, Publisher.RetryEvent {

    default int retries() {
        return 0;
    }

    Subscriptions.Subscription subscription();

    List<SpsEvent> forkedEvents();

    Instant createdAt();

    List<DeliveryType> deliveryTypes();

    default String id() {
        return forkedEvents().getFirst().id();
    }

}

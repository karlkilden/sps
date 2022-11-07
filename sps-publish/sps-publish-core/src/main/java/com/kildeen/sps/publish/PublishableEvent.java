package com.kildeen.sps.publish;

import com.kildeen.sps.SpsEvent;

import java.util.List;
import java.util.Map;

public sealed interface PublishableEvent permits EventFork.ForkedEvents.Fork, Publisher.RetryEvent  {

    default int retries() {
        return 0;
    }

    Subscriptions.Subscription subscription();

    List<SpsEvent> forkedEvents();
}

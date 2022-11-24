package com.kildeen.sps.subscribe;

import com.kildeen.sps.SpsEvent;

//TODO: should be called x timeunit, perhaps also a policy on how to handle refresh
class AddSubscription {

    private final AddSubscriptions addSubscriptions;

    AddSubscription(AddSubscriptions addSubscriptions) {
        this.addSubscriptions = addSubscriptions;
    }

    //TODO: Pass a policy on how to handle missmatched schema keys
    void add(SpsEvent spsEvent) {
        AddSubscriberSpsEvent addSubscriberSpsEvent = AddSubscriberSpsEvent.from(spsEvent);
        addSubscriptions.add(new Subscription(addSubscriberSpsEvent.requestedEventType(),
                addSubscriberSpsEvent.url(),
                addSubscriberSpsEvent.subId(),
                addSubscriberSpsEvent.subSchema()));
    }
}

package com.kildeen.sps.publish;


import com.kildeen.embeddeddb.EmbeddedDatabase;

public class FetchSubscriptionsImpl implements FetchSubscriptions {

    @Override
    public Subscriptions fetch(String eventType) {
        EmbeddedDatabase embeddedDatabase = EmbeddedDatabase.get();
        //return embeddedDatabase.subscriptions(eventType);
        return null;
    }
}

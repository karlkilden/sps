package com.kildeen.sps.publish;


import com.kildeen.sps.persistence.Config;
import com.kildeen.sps.persistence.Database;

public class FetchSubscriptionsImpl implements FetchSubscriptions {

    private final Database database;

    public FetchSubscriptionsImpl(Database database) {
        this.database = database;
    }

    @Override
    public Subscriptions fetchSubscriptions(String eventType) {

        return database.subscriptions(eventType);
    }

    @Override
    public String fetchSubscriptions() {
        Config config = database.fetchConfig();
        return config.gen().url();
    }
}

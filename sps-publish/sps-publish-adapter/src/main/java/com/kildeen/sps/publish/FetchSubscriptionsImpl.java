package com.kildeen.sps.publish;


import com.kildeen.sps.persistence.Config;
import com.kildeen.sps.persistence.Database;

import java.util.Set;

public class FetchSubscriptionsImpl implements FetchSubscriptions {

    private final Database database;

    public FetchSubscriptionsImpl(Database database) {
        this.database = database;
    }

    @Override
    public Subscriptions fetchSubscriptions(Set<String> eventTypes) {

        return database.subscriptions(eventTypes);
    }

    @Override
    public String fetchSubscriptions() {
        Config config = database.fetchConfig();
        return config.gen().url();
    }
}

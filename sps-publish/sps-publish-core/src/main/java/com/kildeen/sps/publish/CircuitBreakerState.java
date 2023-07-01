package com.kildeen.sps.publish;

import com.kildeen.sps.persistence.Database;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class is used by the publisher to check if any eventTypes have their circuits tripped
 */
public class CircuitBreakerState {

    private final Database database;
    private final ScheduledExecutorService checkForTrippedCircuits;
    private Map<String, Set<String>> trippedCircuitsForTypes = Map.of();

    public CircuitBreakerState(Database database) {
        this.database = database;
        checkForTrippedCircuits = Executors.newScheduledThreadPool(1);
        checkForTrippedCircuits.scheduleAtFixedRate(this::cacheTripped,
                200,
                4000,
                TimeUnit.MILLISECONDS);
    }

    private void cacheTripped() {
        this.trippedCircuitsForTypes = database.trippedCircuits();
    }

    public Map<String, Set<String>> trippedCircuitsForTypes() {
        return trippedCircuitsForTypes;
    }

    public boolean isTripped(PublishableEvent fork) {
        return trippedCircuitsForTypes().getOrDefault(fork.subscription().subscriber().subId(),
                Set.of()).contains(fork.subscription().eventType());
    }

}

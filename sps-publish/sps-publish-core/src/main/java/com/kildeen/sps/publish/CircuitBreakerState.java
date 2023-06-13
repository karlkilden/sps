package com.kildeen.sps.publish;

import com.kildeen.sps.persistence.Database;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

public class CircuitBreakerState {

    private Database database;
    private ScheduledExecutorService executorService;

    public CircuitBreakerState(Database database) {
        this.database = database;
    }

    public Map<String, Set<String>> trippedCircuitsForTypes() {
        return null;
    }

}

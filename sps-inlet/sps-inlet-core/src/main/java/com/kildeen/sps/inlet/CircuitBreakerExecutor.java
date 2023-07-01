package com.kildeen.sps.inlet;

import com.kildeen.sps.persistence.Database;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CircuitBreakerExecutor {

    private final Map<String, Receiver> mapped;
    private Database database;
    private ScheduledExecutorService executorService;

    public CircuitBreakerExecutor(Map<String, Receiver> mapped,
                                  Database database) {
        this.mapped = mapped;
        this.database = database;
        executorService = Executors.newScheduledThreadPool(2);
        executorService.scheduleAtFixedRate(this::checkIfTripped,
                200,
                1000,
                TimeUnit.MILLISECONDS);

        executorService.scheduleAtFixedRate(this::setIsAttemptingReset,
                200,
                1000,
                TimeUnit.MILLISECONDS);
    }

    private void setIsAttemptingReset() {
        System.out.println("setIsAttemptingReset");

        mapped.forEach((key, value) -> {
            if (value instanceof CircuitBreakEnabledReceiver receiver) {
                if (!database.isTripped(receiver.subId(), receiver.eventType())) {
                    return;
                }
                System.out.println("setIsAttemptingReset" + receiver.eventType());
                if (receiver.trippedCircuit().hasRemainingEvents() == false) {
                    database.resetCircuit(receiver.subId(), receiver.eventType());
                }
                else {
                    database.tripCircuit(receiver.subId(), receiver.eventType());
                }

            }
        });

    }

    private void checkIfTripped() {
        Instant now = Instant.now();
        mapped.forEach((key, value) -> {
            if (value instanceof CircuitBreakEnabledReceiver receiver) {
                long nackCount = database.nackCountByTypeSince(key, now
                        .minus(receiver.breaker().checkWindowInMs(), ChronoUnit.MILLIS));
                System.out.println(receiver.breaker().type() + " nack count:" + nackCount);
                if (nackCount > receiver.breaker().threshold()) {
                    receiver.trip();
                    System.out.println("Tripped:" + receiver.eventType());
                    database.tripCircuit(receiver.subId(), receiver.eventType());
                }
            }
        });
    }
}
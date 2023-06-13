package com.kildeen.sps;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public record CircuitBreakers(List<CircuitBreaker> circuitBreakers, CircuitBreaker defaultBreaker) {
    public record CircuitBreaker(String type,
                                 long threshold,
                                 long checkWindowInMs,
                                 List<Integer> resetCircuitTiers,
                                 int waitBetweenResetAttemptsInSeconds) {

        public static CircuitBreaker defaultForType(String eventType) {
            return new CircuitBreaker(eventType,
                    40,
                    Duration.of(1, ChronoUnit.MINUTES).toMillis(),
                    List.of(10, 50),
                    60);
        }
    }

}

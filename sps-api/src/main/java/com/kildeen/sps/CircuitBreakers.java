package com.kildeen.sps;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public record CircuitBreakers(List<CircuitBreaker> circuitBreakers, DefaultBreaker defaultBreaker) {

    /**
         * A circuit breaker but untyped. Representing a general purpose breaker
         */
        public record DefaultBreaker(long threshold, long checkWindowInMs, List<Integer> resetCircuitTiers,
                                     int waitBetweenResetAttemptsInSeconds) {
        public static CircuitBreaker createDefault(CircuitBreakers circuitBreakers, String eventType) {
            if (circuitBreakers.defaultBreaker() == null) {
                return defaultForType(eventType);
            }

            return new CircuitBreaker(eventType,
                    circuitBreakers.defaultBreaker().threshold(),
                    circuitBreakers.defaultBreaker().checkWindowInMs(),
                    circuitBreakers.defaultBreaker().resetCircuitTiers(),
                    circuitBreakers.defaultBreaker().waitBetweenResetAttemptsInSeconds());
        }

        public static CircuitBreaker defaultForType(String eventType) {

            if ("a1sczac_sps_internal_test_id".equals(eventType)) {
                return new CircuitBreaker(eventType,
                        10,
                        Duration.of(1, ChronoUnit.MINUTES).toMillis(),
                        List.of(10, 50),
                        60);
            }
            return new CircuitBreaker(eventType,
                    40,
                    Duration.of(1, ChronoUnit.MINUTES).toMillis(),
                    List.of(10, 50),
                    60);

        }
    }

    /**
     *
     * @param type
     * @param threshold
     * @param checkWindowInMs
     * @param resetCircuitTiers 10, 20, 30 means first try 10 events,
     *                          if that is ok try 20 and so forth until all tiers ok
     * @param waitBetweenResetAttemptsInSeconds
     */
    public record CircuitBreaker(String type,
                                 long threshold,
                                 long checkWindowInMs,
                                 List<Integer> resetCircuitTiers,
                                 int waitBetweenResetAttemptsInSeconds) {

    }

}

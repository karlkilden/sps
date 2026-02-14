package com.kildeen.sps.client;

/**
 * States for the client-side circuit breaker.
 */
public enum CircuitState {
    /**
     * Circuit is closed - requests flow normally.
     * Transitions to OPEN when failure threshold is exceeded.
     */
    CLOSED,

    /**
     * Circuit is open - requests are rejected immediately.
     * After timeout, transitions to HALF_OPEN to test if service recovered.
     */
    OPEN,

    /**
     * Circuit is half-open - allowing a limited number of test requests.
     * Successful requests transition to CLOSED, failures transition back to OPEN.
     */
    HALF_OPEN
}

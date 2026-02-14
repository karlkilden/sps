package com.kildeen.sps.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Client-side circuit breaker implementation.
 * Follows the industry-standard pattern where the client (consumer) manages its own
 * resilience to server failures, rather than relying on server-side circuit breakers.
 *
 * <p>States:
 * <ul>
 *   <li>CLOSED: Normal operation, requests flow through</li>
 *   <li>OPEN: After threshold failures, requests rejected immediately</li>
 *   <li>HALF_OPEN: After timeout, limited test requests allowed</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * ClientCircuitBreaker breaker = ClientCircuitBreaker.builder()
 *     .failureThreshold(5)
 *     .resetTimeoutMs(30000)
 *     .build();
 *
 * try {
 *     breaker.execute(() -> sendRequest());
 * } catch (CircuitOpenException e) {
 *     // Circuit is open, request not sent
 * }
 * </pre>
 */
public class ClientCircuitBreaker {

    private static final Logger LOG = LoggerFactory.getLogger(ClientCircuitBreaker.class);

    private final int failureThreshold;
    private final long resetTimeoutMs;
    private final int halfOpenPermits;

    private final AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger halfOpenAttempts = new AtomicInteger(0);
    private volatile Instant lastFailureTime;
    private volatile Instant openedAt;

    private ClientCircuitBreaker(Builder builder) {
        this.failureThreshold = builder.failureThreshold;
        this.resetTimeoutMs = builder.resetTimeoutMs;
        this.halfOpenPermits = builder.halfOpenPermits;
    }

    /**
     * Executes an action protected by the circuit breaker.
     *
     * @param action the action to execute
     * @param <T> the return type
     * @return the result of the action
     * @throws CircuitOpenException if the circuit is open
     */
    public <T> T execute(Supplier<T> action) throws CircuitOpenException {
        if (!allowRequest()) {
            throw new CircuitOpenException("Circuit breaker is open");
        }

        try {
            T result = action.get();
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordFailure();
            throw e;
        }
    }

    /**
     * Executes a void action protected by the circuit breaker.
     *
     * @param action the action to execute
     * @throws CircuitOpenException if the circuit is open
     */
    public void executeVoid(Runnable action) throws CircuitOpenException {
        execute(() -> {
            action.run();
            return null;
        });
    }

    /**
     * Checks if a request should be allowed through.
     */
    public boolean allowRequest() {
        CircuitState currentState = state.get();

        switch (currentState) {
            case CLOSED:
                return true;

            case OPEN:
                if (shouldAttemptReset()) {
                    if (state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN)) {
                        LOG.info("Circuit breaker transitioning to HALF_OPEN");
                        halfOpenAttempts.set(0);
                    }
                    return true;
                }
                return false;

            case HALF_OPEN:
                return halfOpenAttempts.incrementAndGet() <= halfOpenPermits;

            default:
                return false;
        }
    }

    /**
     * Records a successful request.
     */
    public void recordSuccess() {
        CircuitState currentState = state.get();

        if (currentState == CircuitState.HALF_OPEN) {
            // Successful request in half-open state resets the circuit
            if (state.compareAndSet(CircuitState.HALF_OPEN, CircuitState.CLOSED)) {
                LOG.info("Circuit breaker reset to CLOSED after successful request");
                failureCount.set(0);
                halfOpenAttempts.set(0);
            }
        } else if (currentState == CircuitState.CLOSED) {
            // Reset failure count on success
            failureCount.set(0);
        }
    }

    /**
     * Records a failed request.
     */
    public void recordFailure() {
        lastFailureTime = Instant.now();
        int failures = failureCount.incrementAndGet();

        CircuitState currentState = state.get();

        if (currentState == CircuitState.HALF_OPEN) {
            // Failure in half-open state opens the circuit again
            if (state.compareAndSet(CircuitState.HALF_OPEN, CircuitState.OPEN)) {
                LOG.warn("Circuit breaker reopened after failure in HALF_OPEN state");
                openedAt = Instant.now();
            }
        } else if (currentState == CircuitState.CLOSED && failures >= failureThreshold) {
            // Threshold exceeded, open the circuit
            if (state.compareAndSet(CircuitState.CLOSED, CircuitState.OPEN)) {
                LOG.warn("Circuit breaker opened after {} failures", failures);
                openedAt = Instant.now();
            }
        }
    }

    private boolean shouldAttemptReset() {
        if (openedAt == null) {
            return true;
        }
        Duration elapsed = Duration.between(openedAt, Instant.now());
        return elapsed.toMillis() >= resetTimeoutMs;
    }

    /**
     * Returns the current state of the circuit breaker.
     */
    public CircuitState getState() {
        return state.get();
    }

    /**
     * Returns the current failure count.
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * Force resets the circuit to closed state. Use with caution.
     */
    public void reset() {
        state.set(CircuitState.CLOSED);
        failureCount.set(0);
        halfOpenAttempts.set(0);
        openedAt = null;
        LOG.info("Circuit breaker manually reset");
    }

    /**
     * Creates a new builder for ClientCircuitBreaker.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int failureThreshold = 5;
        private long resetTimeoutMs = 30000; // 30 seconds
        private int halfOpenPermits = 1;

        /**
         * Sets the number of failures before opening the circuit.
         */
        public Builder failureThreshold(int threshold) {
            this.failureThreshold = threshold;
            return this;
        }

        /**
         * Sets the timeout in milliseconds before attempting to reset.
         */
        public Builder resetTimeoutMs(long timeoutMs) {
            this.resetTimeoutMs = timeoutMs;
            return this;
        }

        /**
         * Sets the number of test requests allowed in half-open state.
         */
        public Builder halfOpenPermits(int permits) {
            this.halfOpenPermits = permits;
            return this;
        }

        public ClientCircuitBreaker build() {
            return new ClientCircuitBreaker(this);
        }
    }

    /**
     * Exception thrown when the circuit breaker is open.
     */
    public static class CircuitOpenException extends RuntimeException {
        public CircuitOpenException(String message) {
            super(message);
        }
    }
}

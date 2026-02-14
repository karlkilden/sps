package com.kildeen.sps.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the client-side circuit breaker.
 */
@DisplayName("Client Circuit Breaker Tests")
public class ClientCircuitBreakerTest {

    private ClientCircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = ClientCircuitBreaker.builder()
                .failureThreshold(3)
                .resetTimeoutMs(100) // Short timeout for testing
                .halfOpenPermits(1)
                .build();
    }

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @Test
        @DisplayName("Should start in CLOSED state")
        void startsInClosedState() {
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitState.CLOSED);
        }

        @Test
        @DisplayName("Should transition to OPEN after threshold failures")
        void opensAfterThresholdFailures() {
            // Record failures up to threshold
            for (int i = 0; i < 3; i++) {
                circuitBreaker.recordFailure();
            }

            assertThat(circuitBreaker.getState()).isEqualTo(CircuitState.OPEN);
        }

        @Test
        @DisplayName("Should not open before threshold")
        void doesNotOpenBeforeThreshold() {
            circuitBreaker.recordFailure();
            circuitBreaker.recordFailure();

            assertThat(circuitBreaker.getState()).isEqualTo(CircuitState.CLOSED);
        }

        @Test
        @DisplayName("Should transition to HALF_OPEN after timeout")
        void transitionsToHalfOpenAfterTimeout() throws InterruptedException {
            // Open the circuit
            for (int i = 0; i < 3; i++) {
                circuitBreaker.recordFailure();
            }
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitState.OPEN);

            // Wait for reset timeout
            Thread.sleep(150);

            // Try to make a request - should transition to HALF_OPEN
            boolean allowed = circuitBreaker.allowRequest();
            assertThat(allowed).isTrue();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitState.HALF_OPEN);
        }

        @Test
        @DisplayName("Should close after successful request in HALF_OPEN")
        void closesAfterSuccessInHalfOpen() throws InterruptedException {
            // Open the circuit
            for (int i = 0; i < 3; i++) {
                circuitBreaker.recordFailure();
            }

            // Wait for timeout and transition to HALF_OPEN
            Thread.sleep(150);
            circuitBreaker.allowRequest();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitState.HALF_OPEN);

            // Record success
            circuitBreaker.recordSuccess();

            assertThat(circuitBreaker.getState()).isEqualTo(CircuitState.CLOSED);
        }

        @Test
        @DisplayName("Should reopen after failure in HALF_OPEN")
        void reopensAfterFailureInHalfOpen() throws InterruptedException {
            // Open the circuit
            for (int i = 0; i < 3; i++) {
                circuitBreaker.recordFailure();
            }

            // Wait for timeout and transition to HALF_OPEN
            Thread.sleep(150);
            circuitBreaker.allowRequest();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitState.HALF_OPEN);

            // Record failure
            circuitBreaker.recordFailure();

            assertThat(circuitBreaker.getState()).isEqualTo(CircuitState.OPEN);
        }
    }

    @Nested
    @DisplayName("Request Flow")
    class RequestFlow {

        @Test
        @DisplayName("Should allow requests when CLOSED")
        void allowsRequestsWhenClosed() {
            AtomicInteger callCount = new AtomicInteger(0);

            String result = circuitBreaker.execute(() -> {
                callCount.incrementAndGet();
                return "success";
            });

            assertThat(result).isEqualTo("success");
            assertThat(callCount.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should reject requests when OPEN")
        void rejectsRequestsWhenOpen() {
            // Open the circuit
            for (int i = 0; i < 3; i++) {
                circuitBreaker.recordFailure();
            }

            assertThatThrownBy(() -> circuitBreaker.execute(() -> "should not run"))
                    .isInstanceOf(ClientCircuitBreaker.CircuitOpenException.class);
        }

        @Test
        @DisplayName("Should record failure on exception")
        void recordsFailureOnException() {
            assertThatThrownBy(() ->
                    circuitBreaker.execute(() -> {
                        throw new RuntimeException("error");
                    })
            ).isInstanceOf(RuntimeException.class);

            assertThat(circuitBreaker.getFailureCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should reset failure count on success")
        void resetsFailureCountOnSuccess() {
            circuitBreaker.recordFailure();
            circuitBreaker.recordFailure();
            assertThat(circuitBreaker.getFailureCount()).isEqualTo(2);

            circuitBreaker.recordSuccess();
            assertThat(circuitBreaker.getFailureCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Manual Control")
    class ManualControl {

        @Test
        @DisplayName("Should manually reset circuit")
        void manualReset() {
            // Open the circuit
            for (int i = 0; i < 3; i++) {
                circuitBreaker.recordFailure();
            }
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitState.OPEN);

            // Manual reset
            circuitBreaker.reset();

            assertThat(circuitBreaker.getState()).isEqualTo(CircuitState.CLOSED);
            assertThat(circuitBreaker.getFailureCount()).isEqualTo(0);
        }
    }
}

package com.kildeen.sps.publish;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimiter Tests")
class RateLimiterTest {

    @Test
    @DisplayName("Should allow requests up to max limit")
    void shouldAllowRequestsUpToMaxLimit() {
        RateLimiter limiter = new RateLimiter(5, 1000);
        String subscriberId = "test-sub";

        for (int i = 0; i < 5; i++) {
            assertThat(limiter.allowRequest(subscriberId))
                    .as("Request %d should be allowed", i + 1)
                    .isTrue();
        }

        assertThat(limiter.getCurrentCount(subscriberId)).isEqualTo(5);
    }

    @Test
    @DisplayName("Should reject requests after max limit reached")
    void shouldRejectRequestsAfterMaxLimit() {
        RateLimiter limiter = new RateLimiter(3, 1000);
        String subscriberId = "test-sub";

        // Use up the limit
        for (int i = 0; i < 3; i++) {
            limiter.allowRequest(subscriberId);
        }

        // Next request should be rejected
        assertThat(limiter.allowRequest(subscriberId)).isFalse();
        assertThat(limiter.allowRequest(subscriberId)).isFalse();
    }

    @Test
    @DisplayName("Should track separate limits per subscriber")
    void shouldTrackSeparateLimitsPerSubscriber() {
        RateLimiter limiter = new RateLimiter(2, 1000);

        // Sub1 uses its limit
        assertThat(limiter.allowRequest("sub1")).isTrue();
        assertThat(limiter.allowRequest("sub1")).isTrue();
        assertThat(limiter.allowRequest("sub1")).isFalse();

        // Sub2 should still have its own limit
        assertThat(limiter.allowRequest("sub2")).isTrue();
        assertThat(limiter.allowRequest("sub2")).isTrue();
        assertThat(limiter.allowRequest("sub2")).isFalse();
    }

    @Test
    @DisplayName("Should reset after window expires")
    void shouldResetAfterWindowExpires() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(2, 100); // 100ms window
        String subscriberId = "test-sub";

        // Use up the limit
        assertThat(limiter.allowRequest(subscriberId)).isTrue();
        assertThat(limiter.allowRequest(subscriberId)).isTrue();
        assertThat(limiter.allowRequest(subscriberId)).isFalse();

        // Wait for window to expire
        Thread.sleep(150);

        // Should be allowed again
        assertThat(limiter.allowRequest(subscriberId)).isTrue();
    }

    @Test
    @DisplayName("Should handle concurrent requests without exceeding limit")
    void shouldHandleConcurrentRequestsWithoutExceedingLimit() throws InterruptedException {
        int maxRequests = 10;
        RateLimiter limiter = new RateLimiter(maxRequests, 5000);
        String subscriberId = "concurrent-test";
        int threadCount = 50;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        // Submit concurrent requests
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (limiter.allowRequest(subscriberId)) {
                        allowedCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads at once
        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify: exactly maxRequests allowed, rest rejected
        assertThat(allowedCount.get())
                .as("Should allow exactly %d requests", maxRequests)
                .isEqualTo(maxRequests);
        assertThat(rejectedCount.get())
                .as("Should reject %d requests", threadCount - maxRequests)
                .isEqualTo(threadCount - maxRequests);
    }

    @Test
    @DisplayName("Should reset subscriber counter")
    void shouldResetSubscriberCounter() {
        RateLimiter limiter = new RateLimiter(2, 1000);
        String subscriberId = "test-sub";

        limiter.allowRequest(subscriberId);
        limiter.allowRequest(subscriberId);
        assertThat(limiter.getCurrentCount(subscriberId)).isEqualTo(2);

        limiter.reset(subscriberId);

        assertThat(limiter.getCurrentCount(subscriberId)).isEqualTo(0);
        assertThat(limiter.allowRequest(subscriberId)).isTrue();
    }

    @Test
    @DisplayName("Default limiter should allow 100 requests per second")
    void defaultLimiterShouldAllow100RequestsPerSecond() {
        RateLimiter limiter = RateLimiter.defaultLimiter();
        String subscriberId = "default-test";

        for (int i = 0; i < 100; i++) {
            assertThat(limiter.allowRequest(subscriberId)).isTrue();
        }

        assertThat(limiter.allowRequest(subscriberId)).isFalse();
    }
}

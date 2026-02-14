package com.kildeen.sps.publish;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter for controlling event publishing throughput.
 * Implements a sliding window rate limiting algorithm per subscriber.
 *
 * <p>This replaces server-side circuit breakers with a more appropriate
 * server-side pattern: rate limiting. Clients should use
 * {@code ClientCircuitBreaker} for their own resilience needs.
 */
public class RateLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiter.class);

    private final int maxRequestsPerWindow;
    private final long windowMs;
    private final ConcurrentHashMap<String, WindowedCounter> subscriberCounters = new ConcurrentHashMap<>();

    /**
     * Creates a new rate limiter.
     *
     * @param maxRequestsPerWindow maximum requests allowed per window
     * @param windowMs duration of the sliding window in milliseconds
     */
    public RateLimiter(int maxRequestsPerWindow, long windowMs) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowMs = windowMs;
    }

    /**
     * Creates a rate limiter with default settings (100 requests per second).
     */
    public static RateLimiter defaultLimiter() {
        return new RateLimiter(100, 1000);
    }

    /**
     * Checks if a request should be allowed for the given subscriber.
     *
     * @param subscriberId the subscriber ID to check
     * @return true if the request is allowed, false if rate limited
     */
    public boolean allowRequest(String subscriberId) {
        WindowedCounter counter = subscriberCounters.computeIfAbsent(
                subscriberId,
                k -> new WindowedCounter(windowMs)
        );

        if (counter.incrementIfAllowed(maxRequestsPerWindow)) {
            return true;
        }

        LOG.debug("Rate limit exceeded for subscriber {}", subscriberId);
        return false;
    }

    /**
     * Returns the current request count for a subscriber within the window.
     */
    public int getCurrentCount(String subscriberId) {
        WindowedCounter counter = subscriberCounters.get(subscriberId);
        return counter == null ? 0 : counter.getCount();
    }

    /**
     * Resets the rate limit counter for a subscriber.
     */
    public void reset(String subscriberId) {
        subscriberCounters.remove(subscriberId);
    }

    /**
     * Windowed counter that tracks requests within a sliding time window.
     */
    private static class WindowedCounter {
        private final long windowMs;
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile Instant windowStart;

        WindowedCounter(long windowMs) {
            this.windowMs = windowMs;
            this.windowStart = Instant.now();
        }

        boolean incrementIfAllowed(int max) {
            Instant now = Instant.now();
            Duration elapsed = Duration.between(windowStart, now);

            // Reset if window expired
            if (elapsed.toMillis() >= windowMs) {
                synchronized (this) {
                    if (Duration.between(windowStart, Instant.now()).toMillis() >= windowMs) {
                        count.set(0);
                        windowStart = now;
                    }
                }
            }

            // Increment and check
            int current = count.incrementAndGet();
            return current <= max;
        }

        int getCount() {
            return count.get();
        }
    }

    /**
     * Result of a rate limit check.
     */
    public record RateLimitResult(
            boolean allowed,
            int currentCount,
            int maxRequests,
            long windowMs,
            long retryAfterMs
    ) {
        public static RateLimitResult allowed(int currentCount, int maxRequests, long windowMs) {
            return new RateLimitResult(true, currentCount, maxRequests, windowMs, 0);
        }

        public static RateLimitResult rejected(int currentCount, int maxRequests, long windowMs, long retryAfterMs) {
            return new RateLimitResult(false, currentCount, maxRequests, windowMs, retryAfterMs);
        }
    }
}

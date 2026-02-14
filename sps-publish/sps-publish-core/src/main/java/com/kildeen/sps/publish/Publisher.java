package com.kildeen.sps.publish;

import com.kildeen.sps.IdWithReceipts;
import com.kildeen.sps.IdWithReceiptsResult;
import com.kildeen.sps.Receipt;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.dlq.DeadLetterQueue;
import com.kildeen.sps.persistence.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Publishes events to subscribers with retry support.
 * Implements AutoCloseable for proper resource cleanup.
 *
 * <p>Note: Server-side circuit breakers have been removed. Clients should use
 * {@code ClientCircuitBreaker} for resilience. Server-side rate limiting via
 * {@code RateLimiter} is available for protecting the server.
 */
public class Publisher implements AutoCloseable {
    static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ScheduledExecutorService retryQueueScheduler;
    private final ExecutorService retryExecutor;
    private final Sender sender;
    private final RetryQueue retryQueue;
    private final RetryPolicies retryPolicies;
    private final Database database;
    private final DeadLetterQueue deadLetterQueue;
    private volatile boolean closed = false;

    public Publisher(Sender sender,
                     RetryQueue retryQueue,
                     RetryPolicies retryPolicies,
                     Database database) {
        this(sender, retryQueue, retryPolicies, database, null);
    }

    public Publisher(Sender sender,
                     RetryQueue retryQueue,
                     RetryPolicies retryPolicies,
                     Database database,
                     DeadLetterQueue deadLetterQueue) {
        this.sender = sender;
        this.retryQueue = retryQueue;
        this.retryPolicies = retryPolicies;
        this.deadLetterQueue = deadLetterQueue;
        this.database = database;

        // Instance-scoped executors with proper thread naming
        this.retryQueueScheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "publisher-retry-scheduler");
            t.setDaemon(true);
            return t;
        });
        this.retryExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "publisher-retry-worker");
            t.setDaemon(true);
            return t;
        });

        retryQueueScheduler.scheduleAtFixedRate(this::retryFromQueue,
                200,
                2000,
                TimeUnit.MILLISECONDS);

        LOG.info("Publisher initialized with retry support");
    }

    private void retryFromQueue() {
        if (closed) {
            return;
        }
        PublishableEvent event = retryQueue.next();
        if (event != null) {
            RetryPolicies.RetryPolicy retryPolicy = retryPolicies.forAttempt(event.retries(), event.deliveryTypes());
            retry(event, retryPolicy);
        }
    }

    int publish(Subscriptions subscriptions, Collection<SpsEvent> events) {
        EventFork eventFork = new EventFork(events, subscriptions.subscriptions());
        eventFork.fork().forks().forEach(this::sendAsync);
        return 10;
    }

    private void sendAsync(PublishableEvent fork) {
        try {
            CompletableFuture<IdWithReceiptsResult> response = sender.send(fork);
            response.thenAccept(res -> handleResponse(res, fork));
        } catch (CompletionException e) {
            handleRetry(fork, e);
        }
    }

    private void handleRetry(PublishableEvent fork, CompletionException e) {
        LOG.warn("Could not send {}.", fork, e);
        RetryPolicies.RetryPolicy retryPolicy = retryPolicies.forAttempt(fork.retries(), fork.deliveryTypes());
        retry(fork, retryPolicy);
    }

    private void handleResponse(IdWithReceiptsResult res, PublishableEvent event) {
        if (res.allEvents() == Receipt.ACK) {
            return;
        }
        List<SpsEvent> spsEvents = event.forkedEvents().stream()
                .filter(e -> failed(e, res))
                .toList();

        if (spsEvents.isEmpty()) {
            return;
        }
        RetryPolicies.RetryPolicy retryPolicy = retryPolicies.forAttempt(event.retries(), event.deliveryTypes());
        retry(new RetryEvent(event.subscription(),
                spsEvents,
                event.retries(),
                deliveryTypes(retryPolicy.deliveryType()),
                event.createdAt()), retryPolicy);
    }

    private List<DeliveryType> deliveryTypes(DeliveryType deliveryType) {
        return List.of(deliveryType);
    }

    private boolean failed(SpsEvent e, IdWithReceiptsResult res) {
        Optional<IdWithReceipts.IdWithReceipt> any = res.idWithReceipts().stream()
                .filter(id -> id.id().equals(e.id())).findAny();
        return any.isEmpty() || any.get().receipt() != Receipt.ACK;

    }

    private void retry(PublishableEvent event, RetryPolicies.RetryPolicy retryPolicy) {
        if (closed) {
            LOG.debug("Publisher closed, skipping retry for event {}", event.id());
            return;
        }

        if (retryPolicy == null) {
            return;
        }

        boolean saved = false;
        if (retryPolicy.retention() == RetryPolicies.RetryPolicy.RetentionType.PERSISTENT) {
            saved = retryQueue.save(event, retryPolicy);
        }

        // Check for event abandonment
        if (retryPolicy.abandonEventAfterMs() > 0) {
            Duration aliveDuration = Duration.between(event.createdAt(), Instant.now());
            if (aliveDuration.toMillis() > retryPolicy.abandonEventAfterMs()) {
                LOG.warn("Abandoning event {} after {} ms (retries: {})", event.id(), aliveDuration.toMillis(), event.retries());
                // Route to Dead Letter Queue if configured
                if (deadLetterQueue != null) {
                    event.forkedEvents().forEach(e ->
                            deadLetterQueue.send(e, "max_retries_exceeded", event.retries()));
                }
                event.forkedEvents().forEach(e -> database.ackOrNack(e, Receipt.ABANDONED));
                return;
            }
        }

        if (retryPolicy.refreshSubscription()) {
            event.subscription().refreshUrl();
        }

        // Non-blocking retry: schedule with delay instead of Thread.sleep()
        long waitMs = retryPolicy.waitInMs();
        if ((saved || retryPolicy.retention() == RetryPolicies.RetryPolicy.RetentionType.IN_MEMORY)
                && waitMs > 0) {
            // Schedule delayed retry - does not block the current thread
            retryQueueScheduler.schedule(
                    () -> doRetry(event, retryPolicy.deliveryType()),
                    waitMs,
                    TimeUnit.MILLISECONDS
            );
        } else {
            // Immediate retry
            doRetry(event, retryPolicy.deliveryType());
        }
    }

    private void doRetry(PublishableEvent event, DeliveryType deliveryType) {
        if (closed) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            RetryEvent retryEvent = new RetryEvent(event.subscription(), event.forkedEvents(),
                    event.retries() + 1, deliveryTypes(deliveryType), event.createdAt());
            sendAsync(retryEvent);
        }, retryExecutor);
    }

    @Override
    public void close() {
        closed = true;
        LOG.info("Shutting down Publisher...");

        retryQueueScheduler.shutdown();
        retryExecutor.shutdown();

        try {
            if (!retryQueueScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                retryQueueScheduler.shutdownNow();
            }
            if (!retryExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                retryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            retryQueueScheduler.shutdownNow();
            retryExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOG.info("Publisher shutdown complete");
    }

    record RetryEvent(
            Subscriptions.Subscription subscription,
            List<SpsEvent> forkedEvents,
            int retries,
            List<DeliveryType> deliveryTypes,
            Instant createdAt
    ) implements PublishableEvent {
    }
}

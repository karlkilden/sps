package com.kildeen.sps.publish;

import com.kildeen.sps.IdWithReceipts;
import com.kildeen.sps.IdWithReceiptsResult;
import com.kildeen.sps.Receipt;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.persistence.DataBaseProvider;
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

public class Publisher {
    static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final ScheduledExecutorService RETRY_QUEUE_SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(1);
    private static final ExecutorService RETRY_EXECUTOR = Executors.newFixedThreadPool(2);
    private final Sender sender;
    private final RetryQueue retryQueue;
    private final RetryPolicies retryPolicies;
    private final Database database;
    private final CircuitBreakerState circuitBreakerState;

    public Publisher(Sender sender,
                     RetryQueue retryQueue,
                     RetryPolicies retryPolicies,
                     CircuitBreakerState circuitBreakerState) {
        this.sender = sender;
        this.retryQueue = retryQueue;
        this.retryPolicies = retryPolicies;
        this.circuitBreakerState = circuitBreakerState;
        RETRY_QUEUE_SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::retryFromQueue,
                200,
                2000,
                TimeUnit.MILLISECONDS);
        this.database = DataBaseProvider.database();

    }

    private void retryFromQueue() {
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
            if (circuitBreakerState.isTripped(fork)) {
                handleRetry(fork, new CompletionException(new IllegalStateException("Circuit tripped")));
            } else {
                CompletableFuture<IdWithReceiptsResult> response = sender.send(fork);
                response.thenAccept(res -> handleResponse(res, fork));
            }
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

        if (retryPolicy == null) {
            return;
        }

        boolean saved = false;
        if (retryPolicy.retention() == RetryPolicies.RetryPolicy.RetentionType.PERSISTENT) {
            saved = retryQueue.save(event, retryPolicy);
        }

        if ((saved || retryPolicy.retention() == RetryPolicies.RetryPolicy.RetentionType.IN_MEMORY)
                && retryPolicy.waitInMs() > 0) {
            try {
                TimeUnit.MILLISECONDS.sleep(retryPolicy.waitInMs());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (retryPolicy.abandonEventAfterMs() > 0) {
            Duration aliveDuration = Duration.between(event.createdAt(), Instant.now());

            if (aliveDuration.toMillis() > retryPolicy.abandonEventAfterMs()) {
                event.forkedEvents().forEach(e -> database.ackOrNack(e, Receipt.ABANDONED));
            }
        }

        if (retryPolicy.refreshSubscription()) {
            event.subscription().refreshUrl();
        }

        doRetry(event, retryPolicy.deliveryType());
    }

    private void doRetry(PublishableEvent event, DeliveryType deliveryType) {

        CompletableFuture.runAsync(() -> {
            RetryEvent retryEvent = new RetryEvent(event.subscription(), event.forkedEvents(),
                    event.retries() + 1, deliveryTypes(deliveryType), event.createdAt());
            sendAsync(retryEvent);
        }, RETRY_EXECUTOR);
    }

    record RetryEvent(Subscriptions.Subscription subscription, List<SpsEvent> forkedEvents,
                      int retries, List<DeliveryType> deliveryTypes, Instant createdAt) implements PublishableEvent {
    }
}

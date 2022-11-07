package com.kildeen.sps.publish;

import com.kildeen.sps.SpsEvent;

import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Publisher {
    HttpSender httpSender;
    RetryQueue retryQueue;

    public Publisher(HttpSender httpSender, RetryQueue retryQueue) {
        this.httpSender = httpSender;
        this.retryQueue = retryQueue;
        RETRY_QUEUE_SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::retryFromQueue,
                200,
                2000,
                TimeUnit.MILLISECONDS);
    }

    private void retryFromQueue() {
        PublishableEvent event = retryQueue.next();
        if (event != null) {
            retry(event);
        }
    }

    private static final ExecutorService RETRY_EXECUTOR = Executors.newFixedThreadPool(2);
    static final ScheduledExecutorService RETRY_QUEUE_SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(1);

    int publish(Subscriptions subscriptions, Collection<SpsEvent> events) {
        EventFork eventFork = new EventFork(events, subscriptions.subscriptions());
        eventFork.fork().forks().forEach(this::sendAsync);
        return 10;
    }

    private void sendAsync(PublishableEvent fork) {
        try {
            CompletableFuture<HttpResponse<String>> response = httpSender.send(fork);
            response.thenAccept(res -> handleResponse(res, fork));
        } catch (CompletionException e) {
            retry(fork);
        }
    }

    private void handleResponse(HttpResponse<String> res, PublishableEvent event) {
        if (res.statusCode() != 200) {
            retry(event);
        }
    }

    private void retry(PublishableEvent event) {
        if (event.retries() >= 4) {
            System.out.println("No more retries");
            return;
        }

        if (event.retries() == 2) {
            retryQueue.save(event);
            return;
        }

        CompletableFuture.runAsync(() -> {

            Subscriptions.Subscription subscription = event.retries() == 1 ?
                    refreshSubscription(event) : event.subscription();
            RetryEvent retryEvent = new RetryEvent(subscription, event.forkedEvents(),
                    event.retries() + 1);
            sendAsync(retryEvent);
        }, RETRY_EXECUTOR);
    }

    private Subscriptions.Subscription refreshSubscription(PublishableEvent event) {
        //TODO: refresh
        return event.subscription();
    }

    record RetryEvent(Subscriptions.Subscription subscription, List<SpsEvent>  forkedEvents,
                      int retries) implements PublishableEvent {
    }
}

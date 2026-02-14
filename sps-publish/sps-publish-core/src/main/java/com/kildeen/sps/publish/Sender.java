package com.kildeen.sps.publish;

import com.kildeen.sps.Client;
import com.kildeen.sps.IdWithReceiptsResult;
import com.kildeen.sps.SpsEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Sender {

    private static final Logger LOG = LoggerFactory.getLogger(Sender.class);

    private final List<Client> clients;

    public Sender(List<Client> clients) {
        this.clients = clients;
    }

    public CompletableFuture<IdWithReceiptsResult> send(PublishableEvent fork) {
        SpsEvents spsEvents = new SpsEvents(fork.subscription().eventType(), fork.forkedEvents());
        List<DeliveryType> deliveryTypes = fork.deliveryTypes();

        // Try each delivery type in order, falling back on failure
        return sendWithFallback(fork.subscription(), spsEvents, deliveryTypes, 0);
    }

    private CompletableFuture<IdWithReceiptsResult> sendWithFallback(
            Subscriptions.Subscription subscription,
            SpsEvents spsEvents,
            List<DeliveryType> deliveryTypes,
            int currentIndex) {

        if (currentIndex >= deliveryTypes.size()) {
            return CompletableFuture.failedFuture(
                    new RuntimeException("All delivery methods failed for: " + deliveryTypes));
        }

        DeliveryType deliveryType = deliveryTypes.get(currentIndex);
        Client client = findClient(deliveryType);

        if (client == null) {
            LOG.warn("No client found for delivery type: {}, trying next", deliveryType);
            return sendWithFallback(subscription, spsEvents, deliveryTypes, currentIndex + 1);
        }

        LOG.debug("Attempting delivery via {} for event type {}", deliveryType, spsEvents.eventType());

        return client.post(subscription, spsEvents)
                .handle((result, throwable) -> {
                    // Only fallback on connection/transport exceptions, not on NACK responses
                    // NACK is a valid response that should be returned for the retry system to handle
                    if (throwable != null) {
                        LOG.warn("Delivery via {} failed with exception: {}, attempting fallback",
                                deliveryType, throwable.getMessage());
                        return null; // Signal to try fallback
                    }
                    LOG.debug("Delivery via {} completed for event type {}", deliveryType, spsEvents.eventType());
                    return result;
                })
                .thenCompose(result -> {
                    if (result == null) {
                        // Connection failed, try next delivery type
                        return sendWithFallback(subscription, spsEvents, deliveryTypes, currentIndex + 1);
                    }
                    return CompletableFuture.completedFuture(result);
                });
    }

    private Client findClient(DeliveryType deliveryType) {
        for (Client client : clients) {
            if (client.supports().contains(deliveryType)) {
                return client;
            }
        }
        return null;
    }
}

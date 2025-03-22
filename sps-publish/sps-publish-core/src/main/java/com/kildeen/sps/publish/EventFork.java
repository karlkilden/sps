package com.kildeen.sps.publish;

import com.kildeen.sps.SpsEvent;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventFork {

    private final List<Subscriptions.Subscription> subscriptions;
    private final Collection<SpsEvent> events;

    public EventFork(Collection<SpsEvent> events, List<Subscriptions.Subscription> subscriptions) {
        this.events = events;
        this.subscriptions = subscriptions;
    }

    public ForkedEvents fork() {
        return new ForkedEvents(subscriptions.stream()
                .map(this::forkSubscription)
                .collect(Collectors.toList()));
    }

    private ForkedEvents.Fork forkSubscription(Subscriptions.Subscription subscription) {
        List<SpsEvent> forkedSpsEvents = events.stream()
                .map(event -> createForkedEvent(event, subscription))
                .toList();

        return new ForkedEvents.Fork(subscription, forkedSpsEvents, subscription.deliveryType(), Instant.now());
    }

    private SpsEvent createForkedEvent(SpsEvent originalEvent, Subscriptions.Subscription subscription) {
        Map<String, Object> forkedData = subscription.subSchema().isEmpty() ?
                new HashMap<>(originalEvent.data()) : new HashMap<>();

        if (!subscription.subSchema().isEmpty()) {
            subscription.subSchema().keySet().forEach(key ->
                    forkedData.put(subscription.subSchema().get(key), originalEvent.data().get(key)));
        }

        return new ForkedEvents.Fork.ForkSpsEvent(subscription.eventType(),
                originalEvent.id() + "_" + subscription.subscriber().subId(), forkedData);
    }

    public record ForkedEvents(List<PublishableEvent> forks) {
        public record Fork(Subscriptions.Subscription subscription, List<SpsEvent> forkedEvents,
                           List<DeliveryType> deliveryTypes, Instant createdAt) implements PublishableEvent {
            public record ForkSpsEvent(String type, String id, Map<String, Object> data) implements SpsEvent {
                @Override
                public String type() {
                    return type;
                }

                @Override
                public String id() {
                    return id;
                }

                @Override
                public Map<String, Object> data() {
                    return data;
                }
            }
        }
    }
}

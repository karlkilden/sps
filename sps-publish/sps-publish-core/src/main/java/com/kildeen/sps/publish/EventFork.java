package com.kildeen.sps.publish;

import com.kildeen.sps.SpsEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventFork {


    private final List<Subscriptions.Subscription> subscriptions;
    private final Collection<SpsEvent> events;

    EventFork(Collection<SpsEvent> events, List<Subscriptions.Subscription> subscriptions) {
        this.events = events;
        this.subscriptions = subscriptions;
    }

    ForkedEvents fork() {
        return new ForkedEvents(subscriptions.stream()
                .map(subscription -> {
                    List<SpsEvent> forkedSpsEvents = events.stream().map(e -> {
                        Map<String, Object> forkedData = subscription.subSchema().isEmpty() ? new HashMap<>(e.data()) :
                                new HashMap<>();
                        if (!subscription.subSchema().isEmpty()) {
                            subscription.subSchema().
                                    keySet()
                                    .forEach(key -> forkedData.put(subscription.subSchema().get(key), e.data().get(key)));
                        }

                        return (SpsEvent) new ForkedEvents.Fork.ForkSpsEvent(subscription.eventType(),
                                e.id() + "_" + subscription.subscriber().subId(), forkedData);

                    }).toList();
                    return new ForkedEvents.Fork(subscription, forkedSpsEvents, subscription.deliveryType(), Instant.now());

                }).collect(Collectors.toList()));
    }

    record ForkedEvents(List<PublishableEvent> forks) {
        record Fork(Subscriptions.Subscription subscription,
                    List<SpsEvent> forkedEvents, List<DeliveryType> deliveryTypes, Instant createdAt) implements PublishableEvent {


            record ForkSpsEvent(String type, String id, Map<String, Object> data) implements SpsEvent {
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

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

                        return (SpsEvent) new ForkedEvents.Fork.ForkSpsEvent(e.type(),
                                e.id() + "_" + subscription.subscriber().subId(), forkedData);

                    }).toList();
                    List<SpsEvent> forkedByVersion = new ArrayList<>();
                    forkedSpsEvents.forEach(e -> {
                        if (e instanceof PublishSpsEvent advanced) {
                            if (advanced.types().size() > 1) {
                                advanced.types().stream().filter(type -> !advanced.type().equals(type))
                                        .forEach(type -> forkedByVersion.add(new ForkedEvents.Fork.ForkSpsEvent(type,
                                                advanced.id(),
                                                advanced.data())));
                            }
                        }
                    });

                    forkedByVersion.addAll(forkedSpsEvents);
                    ForkedEvents.Fork.ForkSpsEvent event = (ForkedEvents.Fork.ForkSpsEvent) forkedByVersion.get(0);
                    return new ForkedEvents.Fork(subscription, forkedByVersion, Instant.now());

                }).collect(Collectors.toList()));
    }

    record ForkedEvents(List<PublishableEvent> forks) {
        record Fork(Subscriptions.Subscription subscription,
                    List<SpsEvent> forkedEvents, Instant createdAt) implements PublishableEvent {


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

package com.kildeen.sps.publish;

import com.kildeen.sps.AdvancedPublishSpsEvent;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.publish.Subscriptions.Subscription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventFork {


    private final List<Subscription> subscriptions;
    private final Collection<SpsEvent> events;

    EventFork(Collection<SpsEvent> events, List<Subscription> subscriptions) {
        this.events = events;
        this.subscriptions = subscriptions;
    }

    ForkedEvents fork() {
        return new ForkedEvents(subscriptions.stream()
                .map(subscription -> {
                    List<SpsEvent> forkedSpsEvents = events.stream().map(e -> {
                        Map<String, Object> forkedData = subscription.schema().isEmpty() ? new HashMap<>(e.data()) :
                                new HashMap<>();
                        if (!subscription.schema().isEmpty()) {
                            subscription.schema().
                                    keySet()
                                    .forEach(key -> forkedData.put(subscription.schema().get(key), e.data().get(key)));
                        }

                        if (e instanceof AdvancedPublishSpsEvent advanced) {
                            Map<String, Object> subSpecificData = advanced.dataBySubId().get(subscription.subscriber()
                                    .subId());
                            if (subSpecificData != null) {
                                forkedData.putAll(subSpecificData);
                            }
                        }

                        return (SpsEvent) new ForkedEvents.Fork.ForkSpsEvent(e.type(),
                                e.id() + "_" + subscription.subscriber().subId(), forkedData);

                    }).toList();
                    List<SpsEvent> forkedByVersion = new ArrayList<>();
                    forkedSpsEvents.forEach(e -> {
                        if (e instanceof AdvancedPublishSpsEvent advanced) {
                            if (advanced.types().size() > 1) {
                                advanced.types().stream().filter(type -> !advanced.type().equals(type))
                                        .forEach(type -> forkedByVersion.add(new ForkedEvents.Fork.ForkSpsEvent(type,
                                                advanced.id(),
                                        advanced.data())));
                            }
                        }
                    });

                    forkedByVersion.addAll(forkedSpsEvents);

                    return new ForkedEvents.Fork(subscription, forkedByVersion);



                }).collect(Collectors.toList()));
    }

    record ForkedEvents(List<PublishableEvent> forks) {
        record Fork(
                Subscription subscription,
                List<SpsEvent> forkedEvents) implements PublishableEvent {

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

package com.kildeen.sps.publish;

import com.kildeen.sps.BasicSpsEvents;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class EventForkTest {

    @Test
    void one_fork_per_sub_and_respects_subSchema() {
        BasicSpsEvents.BasicSpsEvent event =
                new BasicSpsEvents.BasicSpsEvent("type", "id", Map.of("key", "value"));

        Subscriptions subscriptions = new Subscriptions(
                List.of(
                        new Subscriptions.Subscription(
                                new Subscriptions.Subscription.Subscriber("firstsub", "firsturl"),
                                "type",
                                Map.of()
                        ),
                        new Subscriptions.Subscription(
                                new Subscriptions.Subscription.Subscriber("secondsub", "secondurl"),
                                "type",
                                Map.of("key", "legacykey")
                        )
                )
        );

        EventFork eventFork = new EventFork(List.of(event), subscriptions.subscriptions());

        EventFork.ForkedEvents fork = eventFork.fork();

        assertAll(
                () -> assertThat(fork.forks().size()).isEqualTo(2),
                () -> assertThat(
                        fork.forks().stream()
                                .filter(e -> e.id().equals("id_secondsub"))
                                .findFirst()
                                .orElseThrow()
                                .forkedEvents().get(0).data().get("legacykey"))
                        .isEqualTo("value")
                );

    }
}
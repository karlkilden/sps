package com.kildeen.sps.publish;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.BasicSpsEvents;
import com.kildeen.sps.JvmLocalPostImpl;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.TestInit;
import com.kildeen.sps.inlet.Inlet;
import com.kildeen.sps.inlet.InletDI;
import com.kildeen.sps.inlet.Receiver;
import com.kildeen.sps.persistence.DataBaseProvider;
import com.kildeen.sps.subscribe.AddSubscriptionsImpl;
import com.kildeen.sps.subscribe.Subscription;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class PublishRetryTest {
    static Inlet inlet;

    static {
        TestInit.init();
    }

    @BeforeAll
    static void setUp() {
        inlet = InletDI.newBuilder()
                .withDatabase(DataBaseProvider.configure(EmbeddedDatabase.get()))
                .withReceivers(List.of(new Receiver() {
                    Map<String, AtomicInteger> failCount = new HashMap<>();

                    @Override
                    public void receive(SpsEvent spsEvent) {
                        Integer desiredFailCount = (Integer) spsEvent.data().get("fail");
                        if (failCount.
                                computeIfAbsent(spsEvent.id(), id -> new AtomicInteger()).get() == desiredFailCount) {
                            return;
                        }
                        if (failCount.get(spsEvent.id()).get() < desiredFailCount) {
                            failCount.get(spsEvent.id()).getAndIncrement();
                            throw new RuntimeException();
                        }
                    }

                    @Override
                    public String eventType() {
                        return "event01";
                    }
                }))
                .build();

        new AddSubscriptionsImpl().add(new Subscription("event01", "resolveUrl", "sub01", Map.of()));

        two_retries_using_default_no_given_policy_given();
        two_retries_using_default_no_given_policy_minWait_200ms_given();
        two_retries_using_default_no_given_policy_minWait_200ms_timeout_at_100_given();
        only_uses_retries_for_start_and_end_given();
    }

    private static void two_retries_using_default_no_given_policy_given() {
        var publish = baseBuilder()
                .withRetryPolicies(new RetryPolicies(List.of(),
                        List.of(RetryPolicies.RetryPolicy.newBuilder()
                                .withForRetryAttempt(0, 2)
                                .build())))
                .build();
        BasicSpsEvents.BasicSpsEvent event =
                new BasicSpsEvents.BasicSpsEvent("event01", "99", Map.of("fail", 2));
        publish.publish("event01", List.of(event));
    }

    private static void two_retries_using_default_no_given_policy_minWait_200ms_timeout_at_100_given() {
        var publish = baseBuilder()
                .withRetryPolicies(new RetryPolicies(List.of(),
                        List.of(RetryPolicies.RetryPolicy.newBuilder()
                                .withForRetryAttempt(0, 2)
                                .withAbandonEventAfterMs(100)
                                .withWaitInMs(200)
                                .build())))
                .build();
        BasicSpsEvents.BasicSpsEvent event =
                new BasicSpsEvents.BasicSpsEvent("event01", "299", Map.of("fail", 2));
        publish.publish("event01", List.of(event));
    }

    private static void two_retries_using_default_no_given_policy_minWait_200ms_given() {
        var publish = baseBuilder()
                .withRetryPolicies(new RetryPolicies(List.of(),
                        List.of(RetryPolicies.RetryPolicy.newBuilder()
                                .withForRetryAttempt(0, 5)
                                .withWaitInMs(200)
                                .build())))
                .build();
        BasicSpsEvents.BasicSpsEvent event =
                new BasicSpsEvents.BasicSpsEvent("event01", "199", Map.of("fail", 3));
        publish.publish("event01", List.of(event));
    }

    private static void only_uses_retries_for_start_and_end_given() {
        var publish = baseBuilder()
                .withRetryPolicies(new RetryPolicies(List.of(),
                        List.of(RetryPolicies.RetryPolicy.newBuilder()
                                        .withForRetryAttempt(1, 5)
                                        .withWaitInMs(0)
                                        .build(),
                                RetryPolicies.RetryPolicy.newBuilder()
                                        .withForRetryAttempt(0, 1)
                                        .withWaitInMs(200)
                                        .build())))
                .build();
        BasicSpsEvents.BasicSpsEvent event =
                new BasicSpsEvents.BasicSpsEvent("event01", "399", Map.of("fail", 3));
        publish.publish("event01", List.of(event));
    }

    static PublishDI.Builder baseBuilder() {
        return PublishDI.newBuilder().withDatabase(DataBaseProvider.configure(EmbeddedDatabase.get()))
                .withClient(new SameJVMClient(new JvmLocalPostImpl(List.of(inlet))));
    }

    @Test()
    void two_retries_using_default_no_given_policy() {
        await().until(() -> DataBaseProvider.database().isAck("99_sub01"));
        assertThat(DataBaseProvider.database().nackCount("99_sub01")).isEqualTo(2);
    }

    @Test()
    void two_retries_using_default_no_given_policy_minWait_200ms() {
        await().until(() -> DataBaseProvider.database().isAck("199_sub01"));
        assertThat(DataBaseProvider.database().nackCount("199_sub01")).isEqualTo(3);
        assertThat(DataBaseProvider.database().firstNackInterval("199_sub01")).isBetween(200L, 300L);
    }

    @Test()
    void two_retries_using_default_no_given_policy_minWait_200ms_timeout_at_100() {
        await().until(() -> DataBaseProvider.database().isAbandoned("299_sub01"));
        assertThat(DataBaseProvider.database().nackCount("299_sub01")).isEqualTo(2);
    }

    @Test()
    void only_uses_retries_for_start_and_end() {
        await().until(() -> DataBaseProvider.database().isAck("399_sub01"));
        assertThat(DataBaseProvider.database().nackCount("399_sub01")).isEqualTo(3);
        assertThat(DataBaseProvider.database().firstNackToAck("399_sub01")).isBetween(200L, 300L);
    }
}
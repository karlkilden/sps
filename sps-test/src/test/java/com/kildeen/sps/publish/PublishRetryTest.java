package com.kildeen.sps.publish;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.BasicSpsEvents;
import com.kildeen.sps.JvmLocalPostImpl;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.TestInit;
import com.kildeen.sps.inlet.Inlet;
import com.kildeen.sps.inlet.InletService;
import com.kildeen.sps.inlet.Receiver;
import com.kildeen.sps.persistence.DatabaseProvider;
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
    public static final String INTERNAL_TEST_ID = "esksjapOZ_sps_internal_test_id";
    public static final String INTERNAL_TEST_ID2 = "esksjapOZ_sps_internal_test_id-2";

    static Inlet inlet;

    static {
        TestInit.init();
    }

    @BeforeAll
    static void setUp() {
        inlet = InletService
                .newBuilder()
                .withSubId("test")
                .withDatabase(DatabaseProvider.configure(EmbeddedDatabase.get()))
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
                        return INTERNAL_TEST_ID;
                    }
                }))
                .build();

        new AddSubscriptionsImpl().add(new Subscription(INTERNAL_TEST_ID, "resolveUrl", "sub01", Map.of()));

        //fire all events asap so the tests run faster
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
                new BasicSpsEvents.BasicSpsEvent(INTERNAL_TEST_ID, "99", Map.of("fail", 2));
        publish.publish(List.of(event));
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
                new BasicSpsEvents.BasicSpsEvent(INTERNAL_TEST_ID, "299", Map.of("fail", 2));
        publish.publish(List.of(event));
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
                new BasicSpsEvents.BasicSpsEvent(INTERNAL_TEST_ID, "199", Map.of("fail", 3));
        publish.publish(List.of(event));
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
                new BasicSpsEvents.BasicSpsEvent(INTERNAL_TEST_ID, "399", Map.of("fail", 3));
        publish.publish( List.of(event));
    }

    static PublishService.Builder baseBuilder() {
        return PublishService.newBuilder().withDatabase(DatabaseProvider.configure(EmbeddedDatabase.get()))
                .withClient(new SameJVMClient(new JvmLocalPostImpl(List.of(inlet))));
    }

    @Test()
    void two_retries_using_default_no_given_policy() {
        await().until(() -> DatabaseProvider.database().isAck("99_sub01"));
        assertThat(DatabaseProvider.database().nackCount("99_sub01")).isEqualTo(2);
    }

    @Test()
    void two_retries_using_default_no_given_policy_minWait_200ms() {
        await().until(() -> DatabaseProvider.database().isAck("199_sub01"));
        assertThat(DatabaseProvider.database().nackCount("199_sub01")).isEqualTo(3);
        assertThat(DatabaseProvider.database().firstNackInterval("199_sub01")).isBetween(200L, 300L);
    }

    @Test()
    void two_retries_using_default_no_given_policy_minWait_200ms_timeout_at_100() {
        await().until(() -> DatabaseProvider.database().isAbandoned("299_sub01"));
        assertThat(DatabaseProvider.database().nackCount("299_sub01")).isEqualTo(2);
    }

    @Test()
    void only_uses_retries_for_start_and_end() {
        await().until(() -> DatabaseProvider.database().isAck("399_sub01"));
        assertThat(DatabaseProvider.database().nackCount("399_sub01")).isEqualTo(3);
        assertThat(DatabaseProvider.database().firstNackToAck("399_sub01")).isBetween(200L, 300L);
    }
}
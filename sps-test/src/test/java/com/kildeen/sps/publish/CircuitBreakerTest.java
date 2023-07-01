package com.kildeen.sps.publish;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.BasicSpsEvents;
import com.kildeen.sps.CircuitBreakers;
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

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;

public class CircuitBreakerTest {
    public static final String INTERNAL_TEST_ID = "a1sczac_sps_internal_test_id";
    public static final String INTERNAL_TEST_ID2 = "kgeieja_sps_internal_test_id-2";

    static Inlet inlet;

    static {
        TestInit.init();
    }

    @BeforeAll
    static void setUp() {
        inlet = InletDI
                .newBuilder()
                .withSubId("test")
                .withDatabase(DataBaseProvider.configure(EmbeddedDatabase.get()))
                .withReceivers(List.of(failTenTimesUsingDefaultBreaker(),
                        recoveryTestBreaker()
                ))
                .withCircuitBreakers(new CircuitBreakers(List.of(
                        new CircuitBreakers.CircuitBreaker(INTERNAL_TEST_ID2,
                                5,
                                Duration.ofSeconds(2).toMillis(),
                                List.of(2),
                                1)
                ), null))
                .build();

        new AddSubscriptionsImpl().add(new Subscription(INTERNAL_TEST_ID, "resolveUrl", "test", Map.of()));
        new AddSubscriptionsImpl().add(new Subscription(INTERNAL_TEST_ID2, "resolveUrl", "test", Map.of()));

        //fire all events asap so the tests run faster
        circuit_breaker_is_tripped_when_passed_threshold_for_default_breaker_given();
        circuit_breaker_is_tripped_then_recovered_given();
    }

    private static Receiver failTenTimesUsingDefaultBreaker() {
        return new Receiver() {
            final Map<String, AtomicInteger> failCount = new HashMap<>();

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
        };
    }

    private static Receiver recoveryTestBreaker() {
        return new Receiver() {
            final Map<String, AtomicInteger> failCount = new HashMap<>();

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
                return INTERNAL_TEST_ID2;
            }
        };
    }

    private static void circuit_breaker_is_tripped_when_passed_threshold_for_default_breaker_given() {
        var publish = baseBuilder()
                .withRetryPolicies(new RetryPolicies(List.of(),
                        List.of(RetryPolicies.RetryPolicy.newBuilder()
                                .withMaxRetries(0)
                                .build())))
                .build();
        for (int i = 0; i < 11; i++) {
            BasicSpsEvents.BasicSpsEvent event =
                    new BasicSpsEvents.BasicSpsEvent(INTERNAL_TEST_ID, "399" + i, Map.of("fail", 1));
            System.out.println(event);
            publish.publish(List.of(event));
        }

    }

    private static void circuit_breaker_is_tripped_then_recovered_given() {
        var publish = baseBuilder()
                .withRetryPolicies(new RetryPolicies(List.of(),
                        List.of(RetryPolicies.RetryPolicy.newBuilder()
                                .withMaxRetries(0)
                                .build())))
                .build();
        for (int i = 0; i < 6; i++) {
            BasicSpsEvents.BasicSpsEvent event =
                    new BasicSpsEvents.BasicSpsEvent(INTERNAL_TEST_ID2, "499" + i, Map.of("fail", 1));
            System.out.println(event);
            publish.publish(List.of(event));
        }

    }

    static PublishDI.Builder baseBuilder() {
        return PublishDI.newBuilder().withDatabase(DataBaseProvider.configure(EmbeddedDatabase.get()))
                .withClient(new SameJVMClient(new JvmLocalPostImpl(List.of(inlet))));
    }

    //There is a hardcoded breaker in the actual impl for "a1sczac_sps_internal_test_id"
    @Test()
    void circuit_breaker_is_tripped_when_passed_threshold_for_default_breaker() {
        await().until(() -> DataBaseProvider.database().isNack("3999_test"));
        await().until(() -> !DataBaseProvider.database().trippedCircuits().isEmpty());
    }

    @Test()
    void circuit_breaker_is_tripped_then_recovered() {
        await().until(() -> DataBaseProvider.database().isTripped("test", INTERNAL_TEST_ID2));
        var publish = baseBuilder()
                .withRetryPolicies(new RetryPolicies(List.of(),
                        List.of(RetryPolicies.RetryPolicy.newBuilder()
                                .withMaxRetries(0)
                                .build())))
                .build();
        for (int i = 0; i < 10; i++) {
            BasicSpsEvents.BasicSpsEvent event =
                    new BasicSpsEvents.BasicSpsEvent(INTERNAL_TEST_ID2, "399" + i, Map.of("fail", 0));
            publish.publish(List.of(event));
        }

        await().until(() -> !DataBaseProvider.database().isTripped("test", INTERNAL_TEST_ID2));

    }
}
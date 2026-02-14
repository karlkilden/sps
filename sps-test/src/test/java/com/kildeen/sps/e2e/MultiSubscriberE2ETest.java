package com.kildeen.sps.e2e;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.BasicSpsEvents;
import com.kildeen.sps.JvmLocalPostImpl;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.TestInit;
import com.kildeen.sps.inlet.Inlet;
import com.kildeen.sps.inlet.InletService;
import com.kildeen.sps.inlet.Receiver;
import com.kildeen.sps.persistence.DatabaseProvider;
import com.kildeen.sps.publish.Publish;
import com.kildeen.sps.publish.PublishService;
import com.kildeen.sps.publish.RetryPolicies;
import com.kildeen.sps.publish.SameJVMClient;
import com.kildeen.sps.subscribe.AddSubscriptionsImpl;
import com.kildeen.sps.subscribe.Subscribe;
import com.kildeen.sps.subscribe.SubscriptionService;
import com.kildeen.sps.subscribe.Subscription;
import com.kildeen.sps.subscribe.SubscriptionReceiver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * E2E tests for multi-subscriber scenarios.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Multi-Subscriber E2E Tests")
public class MultiSubscriberE2ETest {

    static {
        TestInit.init();
    }

    private static final String MULTI_EVENT_TYPE = "multi_sub_test_event";
    private static final String SUBSCRIBER_1 = "multi_subscriber_1";
    private static final String SUBSCRIBER_2 = "multi_subscriber_2";
    private static final String SUBSCRIBER_3 = "multi_subscriber_3";

    private Publish publish;
    private ConcurrentHashMap<String, CopyOnWriteArrayList<SpsEvent>> subscriberEvents;
    private AtomicInteger failingSubscriberCount;

    @BeforeAll
    void setUp() {
        DatabaseProvider.configure(EmbeddedDatabase.get());
        subscriberEvents = new ConcurrentHashMap<>();
        subscriberEvents.put(SUBSCRIBER_1, new CopyOnWriteArrayList<>());
        subscriberEvents.put(SUBSCRIBER_2, new CopyOnWriteArrayList<>());
        subscriberEvents.put(SUBSCRIBER_3, new CopyOnWriteArrayList<>());
        failingSubscriberCount = new AtomicInteger(0);

        Subscribe subscribe = SubscriptionService.INSTANCE.inject();
        SubscriptionReceiver subscriptionReceiver = new SubscriptionReceiver(subscribe);

        // Create a receiver that routes to the appropriate subscriber list
        Receiver multiReceiver = new Receiver() {
            @Override
            public void receive(SpsEvent spsEvent) {
                String eventId = spsEvent.id();
                // Extract subscriber ID from forked event ID
                if (eventId.contains(SUBSCRIBER_1)) {
                    subscriberEvents.get(SUBSCRIBER_1).add(spsEvent);
                } else if (eventId.contains(SUBSCRIBER_2)) {
                    // This subscriber fails sometimes
                    int count = failingSubscriberCount.incrementAndGet();
                    if (count <= 2) {
                        throw new RuntimeException("Simulated failure for subscriber 2");
                    }
                    subscriberEvents.get(SUBSCRIBER_2).add(spsEvent);
                } else if (eventId.contains(SUBSCRIBER_3)) {
                    subscriberEvents.get(SUBSCRIBER_3).add(spsEvent);
                }
            }

            @Override
            public String eventType() {
                return MULTI_EVENT_TYPE;
            }
        };

        Inlet inlet = InletService
                .newBuilder()
                .withSubId("multi-test")
                .withDatabase(DatabaseProvider.database())
                .withReceivers(List.of(subscriptionReceiver, multiReceiver))
                .build();

        // Register all subscriptions
        new AddSubscriptionsImpl().add(new Subscription(
                MULTI_EVENT_TYPE, "test-url", SUBSCRIBER_1, Map.of()));
        new AddSubscriptionsImpl().add(new Subscription(
                MULTI_EVENT_TYPE, "test-url", SUBSCRIBER_2, Map.of()));
        new AddSubscriptionsImpl().add(new Subscription(
                MULTI_EVENT_TYPE, "test-url", SUBSCRIBER_3, Map.of()));

        publish = PublishService.newBuilder()
                .withDatabase(DatabaseProvider.database())
                .withClient(new SameJVMClient(new JvmLocalPostImpl(List.of(inlet))))
                .withRetryPolicies(new RetryPolicies(
                        List.of(),
                        List.of(RetryPolicies.RetryPolicy.newBuilder()
                                .withForRetryAttempt(0, 5)
                                .withWaitInMs(10)
                                .build())))
                .build();
    }

    @Test
    @DisplayName("Same event delivered to multiple subscribers")
    void sameEventToMultipleSubscribers() {
        // Clear previous events
        subscriberEvents.values().forEach(list -> list.clear());

        BasicSpsEvents.BasicSpsEvent event = new BasicSpsEvents.BasicSpsEvent(
                MULTI_EVENT_TYPE,
                "multi-event-" + System.currentTimeMillis(),
                Map.of("message", "broadcast"));

        publish.publish(List.of(event));

        // Wait for delivery to at least 2 subscribers (subscriber 2 may fail initially)
        await().atMost(Duration.ofSeconds(10))
                .until(() -> {
                    int total = subscriberEvents.get(SUBSCRIBER_1).size() +
                            subscriberEvents.get(SUBSCRIBER_3).size();
                    return total >= 2;
                });

        // Verify at least subscriber 1 and 3 received the event
        assertThat(subscriberEvents.get(SUBSCRIBER_1)).isNotEmpty();
        assertThat(subscriberEvents.get(SUBSCRIBER_3)).isNotEmpty();
    }

    @Test
    @DisplayName("Subscriber failure is isolated - doesn't affect others")
    void subscriberFailureIsolated() {
        // Clear previous events
        subscriberEvents.values().forEach(list -> list.clear());
        failingSubscriberCount.set(0);

        BasicSpsEvents.BasicSpsEvent event = new BasicSpsEvents.BasicSpsEvent(
                MULTI_EVENT_TYPE,
                "isolation-test-" + System.currentTimeMillis(),
                Map.of("test", "isolation"));

        publish.publish(List.of(event));

        // Subscriber 1 and 3 should receive even if 2 fails
        await().atMost(Duration.ofSeconds(5))
                .until(() -> !subscriberEvents.get(SUBSCRIBER_1).isEmpty() &&
                        !subscriberEvents.get(SUBSCRIBER_3).isEmpty());

        // Verify successful subscribers received the event
        assertThat(subscriberEvents.get(SUBSCRIBER_1)).hasSize(1);
        assertThat(subscriberEvents.get(SUBSCRIBER_3)).hasSize(1);
    }
}

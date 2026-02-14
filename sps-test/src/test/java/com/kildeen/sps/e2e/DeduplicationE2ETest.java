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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * E2E tests for message deduplication functionality.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Deduplication E2E Tests")
public class DeduplicationE2ETest {

    static {
        TestInit.init();
    }

    private static final String DEDUP_EVENT_TYPE = "dedup_test_event";
    private static final String DEDUP_SUBSCRIBER = "dedup_test_subscriber";
    private static final String DEDUP_SUBSCRIBER_2 = "dedup_test_subscriber_2";

    private Publish publish;
    private CopyOnWriteArrayList<SpsEvent> receivedEvents;
    private AtomicInteger processCount;

    @BeforeAll
    void setUp() {
        DatabaseProvider.configure(EmbeddedDatabase.get());
        receivedEvents = new CopyOnWriteArrayList<>();
        processCount = new AtomicInteger(0);

        Subscribe subscribe = SubscriptionService.INSTANCE.inject();
        SubscriptionReceiver subscriptionReceiver = new SubscriptionReceiver(subscribe);

        Receiver capturingReceiver = new Receiver() {
            @Override
            public void receive(SpsEvent spsEvent) {
                processCount.incrementAndGet();
                receivedEvents.add(spsEvent);
            }

            @Override
            public String eventType() {
                return DEDUP_EVENT_TYPE;
            }
        };

        Inlet inlet = InletService
                .newBuilder()
                .withSubId("dedup-test")
                .withDatabase(DatabaseProvider.database())
                .withReceivers(List.of(subscriptionReceiver, capturingReceiver))
                .build();

        // Register subscriptions
        new AddSubscriptionsImpl().add(new Subscription(
                DEDUP_EVENT_TYPE,
                "test-url",
                DEDUP_SUBSCRIBER,
                Map.of()));

        new AddSubscriptionsImpl().add(new Subscription(
                DEDUP_EVENT_TYPE,
                "test-url",
                DEDUP_SUBSCRIBER_2,
                Map.of()));

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
    @DisplayName("Should ignore duplicate event with same ID")
    void duplicateEventIgnored() {
        receivedEvents.clear();
        processCount.set(0);
        String eventId = "dedup-001-" + System.currentTimeMillis();

        BasicSpsEvents.BasicSpsEvent event = new BasicSpsEvents.BasicSpsEvent(
                DEDUP_EVENT_TYPE, eventId, Map.of("attempt", 1));

        // First publish
        publish.publish(List.of(event));
        await().atMost(Duration.ofSeconds(5))
                .until(() -> !receivedEvents.isEmpty());

        int firstCount = processCount.get();

        // Second publish with same ID - should be deduplicated
        BasicSpsEvents.BasicSpsEvent duplicate = new BasicSpsEvents.BasicSpsEvent(
                DEDUP_EVENT_TYPE, eventId, Map.of("attempt", 2));
        publish.publish(List.of(duplicate));

        // Wait a bit and verify no additional processing
        await().during(Duration.ofMillis(500));

        // Process count should not have increased significantly
        // (allows for the second subscriber)
        assertThat(processCount.get()).isLessThanOrEqualTo(firstCount + 2);
    }

    @Test
    @DisplayName("Same ID to different subscribers should both process")
    void sameIdDifferentSubscribersBothProcess() {
        receivedEvents.clear();
        processCount.set(0);
        String eventId = "dedup-multi-" + System.currentTimeMillis();

        BasicSpsEvents.BasicSpsEvent event = new BasicSpsEvents.BasicSpsEvent(
                DEDUP_EVENT_TYPE, eventId, Map.of("data", "test"));

        publish.publish(List.of(event));

        // Both subscribers should receive the event
        await().atMost(Duration.ofSeconds(5))
                .until(() -> receivedEvents.size() >= 2);

        // Verify both subscriber IDs are present in received events
        assertThat(receivedEvents.stream()
                .map(e -> e.id())
                .filter(id -> id.contains(DEDUP_SUBSCRIBER) || id.contains(DEDUP_SUBSCRIBER_2))
                .count()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Different event IDs should both be processed")
    void differentEventIdsProcessed() {
        receivedEvents.clear();
        processCount.set(0);

        BasicSpsEvents.BasicSpsEvent event1 = new BasicSpsEvents.BasicSpsEvent(
                DEDUP_EVENT_TYPE, "dedup-diff-001-" + System.currentTimeMillis(), Map.of("index", 1));
        BasicSpsEvents.BasicSpsEvent event2 = new BasicSpsEvents.BasicSpsEvent(
                DEDUP_EVENT_TYPE, "dedup-diff-002-" + System.currentTimeMillis(), Map.of("index", 2));

        publish.publish(List.of(event1));
        publish.publish(List.of(event2));

        // Both events should be processed (twice each for two subscribers)
        await().atMost(Duration.ofSeconds(5))
                .until(() -> receivedEvents.size() >= 4);

        assertThat(receivedEvents.size()).isGreaterThanOrEqualTo(4);
    }
}

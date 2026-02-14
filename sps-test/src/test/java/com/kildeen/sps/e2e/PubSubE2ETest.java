package com.kildeen.sps.e2e;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.BasicSpsEvents;
import com.kildeen.sps.JvmLocalPostImpl;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEventType;
import com.kildeen.sps.SpsSubscriberType;
import com.kildeen.sps.TestInit;
import com.kildeen.sps.inlet.Inlet;
import com.kildeen.sps.inlet.InletService;
import com.kildeen.sps.inlet.Receiver;
import com.kildeen.sps.persistence.DatabaseProvider;
import com.kildeen.sps.publish.Publish;
import com.kildeen.sps.publish.PublishService;
import com.kildeen.sps.publish.RetryPolicies;
import com.kildeen.sps.publish.SameJVMClient;
import com.kildeen.sps.subscribe.AddSubscriberSpsEvent;
import com.kildeen.sps.subscribe.AddSubscriptionsImpl;
import com.kildeen.sps.subscribe.Subscribe;
import com.kildeen.sps.subscribe.SubscriptionService;
import com.kildeen.sps.subscribe.Subscription;
import com.kildeen.sps.subscribe.SubscriptionReceiver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Comprehensive E2E integration tests for the SPS pub/sub system.
 * Tests the full flow from publishing to receiving events.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("SPS Pub/Sub E2E Tests")
public class PubSubE2ETest {

    static {
        TestInit.init();
    }

    private static final String TEST_EVENT_TYPE = "e2e_test_event";
    private static final String TEST_SUBSCRIBER = "e2e_test_subscriber";
    private static final String FAILING_EVENT_TYPE = "e2e_failing_event";
    private static final String FAILING_SUBSCRIBER = "e2e_failing_subscriber";

    private Inlet inlet;
    private Publish publish;
    private CopyOnWriteArrayList<SpsEvent> receivedEvents;
    private AtomicInteger failingReceiverCallCount;

    @BeforeAll
    void setUp() {
        DatabaseProvider.configure(EmbeddedDatabase.get());
        receivedEvents = new CopyOnWriteArrayList<>();
        failingReceiverCallCount = new AtomicInteger(0);

        Subscribe subscribe = SubscriptionService.INSTANCE.inject();
        SubscriptionReceiver subscriptionReceiver = new SubscriptionReceiver(subscribe);

        // Receiver that captures events
        Receiver capturingReceiver = new Receiver() {
            @Override
            public void receive(SpsEvent spsEvent) {
                receivedEvents.add(spsEvent);
            }

            @Override
            public String eventType() {
                return TEST_EVENT_TYPE;
            }
        };

        // Receiver that fails a configurable number of times
        Receiver failingReceiver = new Receiver() {
            @Override
            public void receive(SpsEvent spsEvent) {
                int count = failingReceiverCallCount.incrementAndGet();
                Integer desiredFailCount = (Integer) spsEvent.data().get("failCount");
                if (desiredFailCount != null && count <= desiredFailCount) {
                    throw new RuntimeException("Simulated failure #" + count);
                }
            }

            @Override
            public String eventType() {
                return FAILING_EVENT_TYPE;
            }
        };

        inlet = InletService
                .newBuilder()
                .withSubId("e2e-test")
                .withDatabase(DatabaseProvider.database())
                .withReceivers(List.of(subscriptionReceiver, capturingReceiver, failingReceiver))
                .build();

        // Register subscriptions
        new AddSubscriptionsImpl().add(new Subscription(
                SpsEventType.add_subscriber_01.toString(),
                "test-url",
                SpsSubscriberType.add_subscriber.toString(),
                Map.of()));

        new AddSubscriptionsImpl().add(new Subscription(
                TEST_EVENT_TYPE,
                "test-url",
                TEST_SUBSCRIBER,
                Map.of()));

        new AddSubscriptionsImpl().add(new Subscription(
                FAILING_EVENT_TYPE,
                "test-url",
                FAILING_SUBSCRIBER,
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

    @Nested
    @DisplayName("Basic Pub/Sub Flow")
    class BasicPubSubFlow {

        @Test
        @DisplayName("Should successfully publish and receive single event")
        void publishSingleEvent() {
            receivedEvents.clear();

            BasicSpsEvents.BasicSpsEvent event = new BasicSpsEvents.BasicSpsEvent(
                    TEST_EVENT_TYPE,
                    "e2e-single-001",
                    Map.of("message", "Hello E2E"));

            publish.publish(List.of(event));

            await().atMost(Duration.ofSeconds(5))
                    .until(() -> !receivedEvents.isEmpty());

            assertThat(receivedEvents).hasSize(1);
            // Event ID is forked as "{originalId}_{subscriberId}" by EventFork
            assertThat(receivedEvents.get(0).id()).startsWith("e2e-single-001");
            assertThat(receivedEvents.get(0).id()).endsWith(TEST_SUBSCRIBER);
            assertThat(receivedEvents.get(0).data()).containsEntry("message", "Hello E2E");
        }

        @Test
        @DisplayName("Should successfully publish and receive multiple events")
        void publishMultipleEvents() {
            receivedEvents.clear();

            publish.publish(List.of(
                    new BasicSpsEvents.BasicSpsEvent(TEST_EVENT_TYPE, "e2e-multi-001", Map.of("index", 1))));
            publish.publish(List.of(
                    new BasicSpsEvents.BasicSpsEvent(TEST_EVENT_TYPE, "e2e-multi-002", Map.of("index", 2))));
            publish.publish(List.of(
                    new BasicSpsEvents.BasicSpsEvent(TEST_EVENT_TYPE, "e2e-multi-003", Map.of("index", 3))));

            await().atMost(Duration.ofSeconds(5))
                    .until(() -> receivedEvents.size() >= 3);

            assertThat(receivedEvents).hasSize(3);
            // Event IDs are forked as "{originalId}_{subscriberId}" by EventFork
            // Extract the original IDs by removing the subscriber suffix
            assertThat(receivedEvents.stream()
                    .map(e -> e.id().replace("_" + TEST_SUBSCRIBER, "")))
                    .containsExactlyInAnyOrder("e2e-multi-001", "e2e-multi-002", "e2e-multi-003");
        }

        @Test
        @DisplayName("Should acknowledge successful event processing")
        void eventIsAcknowledged() {
            BasicSpsEvents.BasicSpsEvent event = new BasicSpsEvents.BasicSpsEvent(
                    TEST_EVENT_TYPE,
                    "e2e-ack-001",
                    Map.of("data", "test"));

            publish.publish(List.of(event));

            await().atMost(Duration.ofSeconds(5))
                    .until(() -> DatabaseProvider.database().isAck("e2e-ack-001", TEST_SUBSCRIBER));
        }
    }

    @Nested
    @DisplayName("Dynamic Subscription")
    class DynamicSubscription {

        @Test
        @DisplayName("Should add new subscriber dynamically")
        void addSubscriberDynamically() {
            String newEventType = "dynamic_event_" + System.currentTimeMillis();

            AddSubscriberSpsEvent addSubscriberEvent = new AddSubscriberSpsEvent(
                    newEventType,
                    Map.of(),
                    "dynamic-url",
                    "dynamic_subscriber_001");

            publish.publish(List.of(addSubscriberEvent));

            await().atMost(Duration.ofSeconds(5))
                    .until(() -> DatabaseProvider.database().isAck(
                            addSubscriberEvent.id(),
                            SpsSubscriberType.add_subscriber.toString()));

            // Subscription was added - event was acknowledged
        }
    }

    @Nested
    @DisplayName("Retry Behavior")
    class RetryBehavior {

        @Test
        @DisplayName("Should retry failed events and eventually succeed")
        void retryAndSucceed() {
            failingReceiverCallCount.set(0);

            // Event configured to fail 2 times then succeed
            BasicSpsEvents.BasicSpsEvent event = new BasicSpsEvents.BasicSpsEvent(
                    FAILING_EVENT_TYPE,
                    "e2e-retry-001",
                    Map.of("failCount", 2));

            publish.publish(List.of(event));

            await().atMost(Duration.ofSeconds(10))
                    .until(() -> DatabaseProvider.database().isAck("e2e-retry-001", FAILING_SUBSCRIBER));

            // Verify it was called 3 times (2 failures + 1 success)
            assertThat(failingReceiverCallCount.get()).isEqualTo(3);
            assertThat(DatabaseProvider.database().nackCount("e2e-retry-001_" + FAILING_SUBSCRIBER)).isEqualTo(2);
        }

        @Test
        @DisplayName("Should track nack count correctly")
        void trackNackCount() {
            failingReceiverCallCount.set(0);

            BasicSpsEvents.BasicSpsEvent event = new BasicSpsEvents.BasicSpsEvent(
                    FAILING_EVENT_TYPE,
                    "e2e-nack-001",
                    Map.of("failCount", 1));

            publish.publish(List.of(event));

            await().atMost(Duration.ofSeconds(5))
                    .until(() -> DatabaseProvider.database().isAck("e2e-nack-001", FAILING_SUBSCRIBER));

            assertThat(DatabaseProvider.database().nackCount("e2e-nack-001_" + FAILING_SUBSCRIBER)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Event Data Integrity")
    class EventDataIntegrity {

        @Test
        @DisplayName("Should preserve complex event data through pub/sub flow")
        void preserveComplexData() {
            receivedEvents.clear();

            Map<String, Object> complexData = Map.of(
                    "string", "test value",
                    "number", 42
            );

            BasicSpsEvents.BasicSpsEvent event = new BasicSpsEvents.BasicSpsEvent(
                    TEST_EVENT_TYPE,
                    "e2e-complex-001",
                    complexData);

            publish.publish(List.of(event));

            await().atMost(Duration.ofSeconds(5))
                    .until(() -> !receivedEvents.isEmpty());

            SpsEvent received = receivedEvents.get(0);
            assertThat(received.data()).containsEntry("string", "test value");
            assertThat(received.data()).containsEntry("number", 42);
        }

        @Test
        @DisplayName("Should preserve event type through pub/sub flow")
        void preserveEventType() {
            receivedEvents.clear();

            BasicSpsEvents.BasicSpsEvent event = new BasicSpsEvents.BasicSpsEvent(
                    TEST_EVENT_TYPE,
                    "e2e-type-001",
                    Map.of("data", "test"));

            publish.publish(List.of(event));

            await().atMost(Duration.ofSeconds(5))
                    .until(() -> !receivedEvents.isEmpty());

            assertThat(receivedEvents.get(0).type()).isEqualTo(TEST_EVENT_TYPE);
        }
    }

    @Nested
    @DisplayName("Concurrent Publishing")
    class ConcurrentPublishing {

        @Test
        @DisplayName("Should handle concurrent event publishing")
        void handleConcurrentPublishing() throws InterruptedException {
            receivedEvents.clear();
            int eventCount = 10;
            CountDownLatch latch = new CountDownLatch(eventCount);

            // Publish events concurrently
            Thread[] threads = new Thread[eventCount];
            for (int i = 0; i < eventCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        BasicSpsEvents.BasicSpsEvent event = new BasicSpsEvents.BasicSpsEvent(
                                TEST_EVENT_TYPE,
                                "e2e-concurrent-" + index,
                                Map.of("index", index));
                        publish.publish(List.of(event));
                    } finally {
                        latch.countDown();
                    }
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            latch.await(10, TimeUnit.SECONDS);

            // Wait for all events to be received
            await().atMost(Duration.ofSeconds(10))
                    .until(() -> receivedEvents.size() >= eventCount);

            assertThat(receivedEvents).hasSize(eventCount);
        }
    }
}

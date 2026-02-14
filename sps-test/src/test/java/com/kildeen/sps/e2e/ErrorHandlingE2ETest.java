package com.kildeen.sps.e2e;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.BasicSpsEvents;
import com.kildeen.sps.JvmLocalPostImpl;
import com.kildeen.sps.Receipt;
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
import org.junit.jupiter.api.BeforeEach;
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
 * E2E tests for error handling scenarios.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Error Handling E2E Tests")
public class ErrorHandlingE2ETest {

    static {
        TestInit.init();
    }

    private static final String ERROR_EVENT_TYPE = "error_test_event";
    private static final String ERROR_SUBSCRIBER = "error_test_subscriber";
    private static final String TIMEOUT_EVENT_TYPE = "timeout_test_event";
    private static final String TIMEOUT_SUBSCRIBER = "timeout_test_subscriber";

    private Inlet inlet;
    private Publish publish;
    private CopyOnWriteArrayList<SpsEvent> receivedEvents;
    private AtomicInteger errorCount;
    private AtomicInteger timeoutCount;

    @BeforeAll
    void setUp() {
        DatabaseProvider.configure(EmbeddedDatabase.get());
        receivedEvents = new CopyOnWriteArrayList<>();
        errorCount = new AtomicInteger(0);
        timeoutCount = new AtomicInteger(0);

        Subscribe subscribe = SubscriptionService.INSTANCE.inject();
        SubscriptionReceiver subscriptionReceiver = new SubscriptionReceiver(subscribe);

        // Receiver that throws specific exceptions
        Receiver errorReceiver = new Receiver() {
            @Override
            public void receive(SpsEvent spsEvent) {
                errorCount.incrementAndGet();
                String errorType = (String) spsEvent.data().get("errorType");
                if ("nullPointer".equals(errorType)) {
                    throw new NullPointerException("Simulated NPE");
                } else if ("illegalState".equals(errorType)) {
                    throw new IllegalStateException("Simulated illegal state");
                } else if ("runtime".equals(errorType)) {
                    throw new RuntimeException("Simulated runtime error");
                }
                receivedEvents.add(spsEvent);
            }

            @Override
            public String eventType() {
                return ERROR_EVENT_TYPE;
            }
        };

        // Receiver that simulates timeout (slow processing)
        Receiver timeoutReceiver = new Receiver() {
            @Override
            public void receive(SpsEvent spsEvent) {
                timeoutCount.incrementAndGet();
                Integer delayMs = (Integer) spsEvent.data().get("delayMs");
                if (delayMs != null && delayMs > 0) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                receivedEvents.add(spsEvent);
            }

            @Override
            public String eventType() {
                return TIMEOUT_EVENT_TYPE;
            }
        };

        inlet = InletService
                .newBuilder()
                .withSubId("error-test")
                .withDatabase(DatabaseProvider.database())
                .withReceivers(List.of(subscriptionReceiver, errorReceiver, timeoutReceiver))
                .build();

        // Register subscriptions
        new AddSubscriptionsImpl().add(new Subscription(
                ERROR_EVENT_TYPE, "test-url", ERROR_SUBSCRIBER, Map.of()));
        new AddSubscriptionsImpl().add(new Subscription(
                TIMEOUT_EVENT_TYPE, "test-url", TIMEOUT_SUBSCRIBER, Map.of()));

        publish = PublishService.newBuilder()
                .withDatabase(DatabaseProvider.database())
                .withClient(new SameJVMClient(new JvmLocalPostImpl(List.of(inlet))))
                .withRetryPolicies(new RetryPolicies(
                        List.of(),
                        List.of(RetryPolicies.RetryPolicy.newBuilder()
                                .withForRetryAttempt(0, 3)
                                .withWaitInMs(10)
                                .build())))
                .build();
    }

    @BeforeEach
    void clear() {
        receivedEvents.clear();
        errorCount.set(0);
        timeoutCount.set(0);
    }

    @Test
    @DisplayName("Should handle NullPointerException gracefully")
    void handleNullPointerException() {
        BasicSpsEvents.BasicSpsEvent event = new BasicSpsEvents.BasicSpsEvent(
                ERROR_EVENT_TYPE,
                "npe-test-" + System.currentTimeMillis(),
                Map.of("errorType", "nullPointer"));

        publish.publish(List.of(event));

        // Wait for retries to complete
        await().atMost(Duration.ofSeconds(5))
                .until(() -> errorCount.get() >= 3);

        // Verify retries were attempted
        assertThat(errorCount.get()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should handle IllegalStateException gracefully")
    void handleIllegalStateException() {
        BasicSpsEvents.BasicSpsEvent event = new BasicSpsEvents.BasicSpsEvent(
                ERROR_EVENT_TYPE,
                "ise-test-" + System.currentTimeMillis(),
                Map.of("errorType", "illegalState"));

        publish.publish(List.of(event));

        // Wait for retries
        await().atMost(Duration.ofSeconds(5))
                .until(() -> errorCount.get() >= 3);

        assertThat(errorCount.get()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should process events that succeed after handling")
    void successfulEventAfterError() {
        BasicSpsEvents.BasicSpsEvent event = new BasicSpsEvents.BasicSpsEvent(
                ERROR_EVENT_TYPE,
                "success-test-" + System.currentTimeMillis(),
                Map.of("noError", true));

        publish.publish(List.of(event));

        await().atMost(Duration.ofSeconds(5))
                .until(() -> !receivedEvents.isEmpty());

        assertThat(receivedEvents).hasSize(1);
    }

    @Test
    @DisplayName("Should handle slow processing without blocking other events")
    void handleSlowProcessing() {
        // Send a slow event and a fast event
        BasicSpsEvents.BasicSpsEvent slowEvent = new BasicSpsEvents.BasicSpsEvent(
                TIMEOUT_EVENT_TYPE,
                "slow-test-" + System.currentTimeMillis(),
                Map.of("delayMs", 100));

        BasicSpsEvents.BasicSpsEvent fastEvent = new BasicSpsEvents.BasicSpsEvent(
                TIMEOUT_EVENT_TYPE,
                "fast-test-" + System.currentTimeMillis(),
                Map.of("delayMs", 0));

        publish.publish(List.of(slowEvent));
        publish.publish(List.of(fastEvent));

        // Both should eventually complete
        await().atMost(Duration.ofSeconds(5))
                .until(() -> receivedEvents.size() >= 2);

        assertThat(receivedEvents.size()).isGreaterThanOrEqualTo(2);
    }
}

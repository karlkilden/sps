package com.kildeen.sps.e2e;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEvents;
import com.kildeen.sps.TestInit;
import com.kildeen.sps.inlet.InletService;
import com.kildeen.sps.inlet.Receiver;
import com.kildeen.sps.persistence.Database;
import com.kildeen.sps.persistence.DatabaseProvider;
import com.kildeen.sps.persistence.TransportQueueEntry;
import com.kildeen.sps.publish.DatabaseClient;
import com.kildeen.sps.publish.DeliveryType;
import com.kildeen.sps.publish.PublishPolicy;
import com.kildeen.sps.publish.Subscriptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for transport fallback mechanism.
 * Tests that when HTTP delivery fails, events fall back to database-based delivery.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Transport Fallback E2E Tests")
class TransportFallbackE2ETest {

    private static final String EVENT_TYPE = "fallback_test_01";
    private static final String SUBSCRIBER_ID = "fallback-test-subscriber";

    static {
        TestInit.init();
    }

    private Database database;
    private CopyOnWriteArrayList<SpsEvent> receivedEvents;

    @BeforeAll
    void setUpAll() {
        DatabaseProvider.configure(EmbeddedDatabase.get());
        database = DatabaseProvider.database();
    }

    @BeforeEach
    void setUp() {
        EmbeddedDatabase.get().clear();
        receivedEvents = new CopyOnWriteArrayList<>();
    }

    @Test
    @DisplayName("Should insert event into transport queue via DatabaseClient")
    void shouldInsertEventIntoTransportQueue() {
        // Given: A database client for fallback delivery
        DatabaseClient databaseClient = new DatabaseClient(database);
        
        // And: A subscription with database delivery
        var subscriber = new Subscriptions.Subscription.Subscriber(
                SUBSCRIBER_ID, 
                "http://localhost:9999", // Non-existent URL
                new PublishPolicy(List.of(DeliveryType.DATABASE))
        );
        var subscription = new Subscriptions.Subscription(subscriber, EVENT_TYPE, Map.of());
        
        // And: An event to publish
        SpsEvent event = createEvent("test-event-1", EVENT_TYPE, Map.of("message", "Hello"));
        var spsEvents = new SpsEvents(EVENT_TYPE, List.of(event));
        
        // When: We post the event via database client
        var result = databaseClient.post(subscription, spsEvents).join();
        
        // Then: The event should be ACKed
        assertThat(result.allEvents()).isEqualTo(com.kildeen.sps.Receipt.ACK);
        
        // And: The event should be in the transport queue
        List<TransportQueueEntry> entries = database.pollTransportQueue(SUBSCRIBER_ID, 10);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).eventId()).isEqualTo("test-event-1");
        assertThat(entries.get(0).eventType()).isEqualTo(EVENT_TYPE);
        assertThat(entries.get(0).subscriberId()).isEqualTo(SUBSCRIBER_ID);
    }

    @Test
    @DisplayName("Should deliver event via transport queue polling")
    void shouldDeliverEventViaPolling() {
        // Given: A receiver that captures events
        Receiver receiver = new Receiver() {
            @Override
            public void receive(SpsEvent event) {
                receivedEvents.add(event);
            }

            @Override
            public String eventType() {
                return EVENT_TYPE;
            }
        };

        // And: An inlet service with transport polling explicitly enabled
        InletService inletService = InletService.newBuilder()
                .withDatabase(database)
                .withSubId(SUBSCRIBER_ID)
                .withReceivers(List.of(receiver))
                .withTransportPolling(true)
                .build();

        // And: An event in the transport queue
        String payload = "{\"eventType\":\"" + EVENT_TYPE + "\",\"spsEvents\":[{\"type\":\"" + EVENT_TYPE + "\",\"id\":\"polled-event-1\",\"data\":{\"message\":\"Hello from queue\"}}]}";
        database.insertTransportEvent("polled-event-1", EVENT_TYPE, SUBSCRIBER_ID, payload);

        // When/Then: The event should be received via polling
        await().atMost(Duration.ofSeconds(5))
                .until(() -> !receivedEvents.isEmpty());

        assertThat(receivedEvents).hasSize(1);
        assertThat(receivedEvents.get(0).id()).isEqualTo("polled-event-1");

        // Cleanup
        inletService.stop();
    }

    @Test
    @DisplayName("Should mark event as processed after delivery")
    void shouldMarkEventAsProcessedAfterDelivery() {
        // Given: A receiver
        Receiver receiver = new Receiver() {
            @Override
            public void receive(SpsEvent event) {
                receivedEvents.add(event);
            }

            @Override
            public String eventType() {
                return EVENT_TYPE;
            }
        };

        // And: An inlet service with transport polling explicitly enabled
        InletService inletService = InletService.newBuilder()
                .withDatabase(database)
                .withSubId(SUBSCRIBER_ID)
                .withReceivers(List.of(receiver))
                .withTransportPolling(true)
                .build();

        // And: An event in the queue
        String payload = "{\"eventType\":\"" + EVENT_TYPE + "\",\"spsEvents\":[{\"type\":\"" + EVENT_TYPE + "\",\"id\":\"processed-event-1\",\"data\":{}}]}";
        database.insertTransportEvent("processed-event-1", EVENT_TYPE, SUBSCRIBER_ID, payload);

        // When: We wait for processing
        await().atMost(Duration.ofSeconds(5))
                .until(() -> !receivedEvents.isEmpty());

        // Allow time for marking as processed
        await().atMost(Duration.ofSeconds(2))
                .until(() -> database.pollTransportQueue(SUBSCRIBER_ID, 10).isEmpty());

        // Then: The queue should be empty (event marked as processed)
        List<TransportQueueEntry> remaining = database.pollTransportQueue(SUBSCRIBER_ID, 10);
        assertThat(remaining).isEmpty();

        // Cleanup
        inletService.stop();
    }

    private SpsEvent createEvent(String id, String type, Map<String, Object> data) {
        return new SpsEvent() {
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
        };
    }
}

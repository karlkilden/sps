package com.kildeen.sps.persistence.postgres;

import com.kildeen.sps.Receipt;
import com.kildeen.sps.Schemas;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.persistence.Config;
import com.kildeen.sps.publish.Subscriptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("PostgresDatabase Tests")
class PostgresDatabaseTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("sps_test")
            .withUsername("sps")
            .withPassword("sps");

    private PostgresDatabaseFactory factory;
    private PostgresDatabase database;

    @BeforeAll
    void setUp() {
        postgres.start();
        factory = PostgresDatabaseFactory.newBuilder()
                .withJdbcUrl(postgres.getJdbcUrl())
                .withUsername(postgres.getUsername())
                .withPassword(postgres.getPassword())
                .withMaxPoolSize(5)
                .build();
        database = factory.create();
    }

    @AfterAll
    void tearDown() {
        if (factory != null) {
            factory.shutdown();
        }
    }

    @BeforeEach
    void cleanTables() {
        factory.getJdbi().useHandle(handle -> {
            handle.execute("TRUNCATE sps_subscriptions, sps_schemas, sps_receipts, sps_circuit_breakers, sps_leader CASCADE");
        });
    }

    @Nested
    @DisplayName("Subscription Operations")
    class SubscriptionOperations {

        @Test
        @DisplayName("Should add and retrieve subscription")
        void addAndRetrieveSubscription() {
            Subscriptions.Subscription.Subscriber subscriber =
                    new Subscriptions.Subscription.Subscriber("test-sub-1", "http://localhost:8080");

            Subscriptions.Subscription subscription = new Subscriptions.Subscription(
                    subscriber,
                    "test.event",
                    Map.of("field1", "mappedField1"));

            database.addSubscription(subscription);

            Subscriptions result = database.subscriptions(Set.of("test.event"));

            assertThat(result.subscriptions()).hasSize(1);
            assertThat(result.subscriptions().get(0).eventType()).isEqualTo("test.event");
            assertThat(result.subscriptions().get(0).subscriber().subId()).isEqualTo("test-sub-1");
        }

        @Test
        @DisplayName("Should update existing subscription")
        void updateExistingSubscription() {
            Subscriptions.Subscription.Subscriber subscriber1 =
                    new Subscriptions.Subscription.Subscriber("test-sub-1", "http://old-url");
            Subscriptions.Subscription subscription1 = new Subscriptions.Subscription(
                    subscriber1, "test.event", Map.of());

            Subscriptions.Subscription.Subscriber subscriber2 =
                    new Subscriptions.Subscription.Subscriber("test-sub-1", "http://new-url");
            Subscriptions.Subscription subscription2 = new Subscriptions.Subscription(
                    subscriber2, "test.event", Map.of());

            database.addSubscription(subscription1);
            database.addSubscription(subscription2);

            Subscriptions result = database.subscriptions(Set.of("test.event"));

            assertThat(result.subscriptions()).hasSize(1);
            // URL should be updated to new value
            assertThat(result.subscriptions().get(0).subscriber().resolveUrl()).isEqualTo("http://new-url");
        }

        @Test
        @DisplayName("Should filter subscriptions by event type")
        void filterByEventType() {
            database.addSubscription(new Subscriptions.Subscription(
                    new Subscriptions.Subscription.Subscriber("sub-a", "http://a"),
                    "type.a", Map.of()));
            database.addSubscription(new Subscriptions.Subscription(
                    new Subscriptions.Subscription.Subscriber("sub-b", "http://b"),
                    "type.b", Map.of()));
            database.addSubscription(new Subscriptions.Subscription(
                    new Subscriptions.Subscription.Subscriber("sub-c", "http://c"),
                    "type.c", Map.of()));

            Subscriptions result = database.subscriptions(Set.of("type.a", "type.c"));

            assertThat(result.subscriptions()).hasSize(2);
            assertThat(result.subscriptions().stream().map(Subscriptions.Subscription::eventType))
                    .containsExactlyInAnyOrder("type.a", "type.c");
        }
    }

    @Nested
    @DisplayName("Schema Operations")
    class SchemaOperations {

        @Test
        @DisplayName("Should add and retrieve schema")
        void addAndRetrieveSchema() {
            Schemas.Schema schema = new Schemas.Schema(
                    "test.event",
                    "Test event documentation",
                    Map.of("field1", "Field 1 doc", "field2", "Field 2 doc"),
                    Set.of("tag1", "tag2"),
                    1);

            database.addSchema(schema);

            Schemas.Schema result = database.schema("test.event");

            assertThat(result).isNotNull();
            assertThat(result.eventType()).isEqualTo("test.event");
            assertThat(result.eventDocumentation()).isEqualTo("Test event documentation");
            assertThat(result.fieldDocumentation()).containsEntry("field1", "Field 1 doc");
            assertThat(result.tags()).containsExactlyInAnyOrder("tag1", "tag2");
            assertThat(result.version()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should list all schemas")
        void listAllSchemas() {
            database.addSchema(new Schemas.Schema("event.a", "Doc A", Map.of(), Set.of(), 1));
            database.addSchema(new Schemas.Schema("event.b", "Doc B", Map.of(), Set.of(), 1));

            Schemas result = database.schemas();

            assertThat(result.schemas()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Receipt Operations")
    class ReceiptOperations {

        @Test
        @DisplayName("Should track ACK receipt")
        void trackAck() {
            SpsEvent event = testEvent("ack-event-1");

            database.ackOrNack(event, Receipt.ACK);

            assertThat(database.isAck("ack-event-1")).isTrue();
            assertThat(database.isNack("ack-event-1")).isFalse();
        }

        @Test
        @DisplayName("Should track NACK receipt")
        void trackNack() {
            SpsEvent event = testEvent("nack-event-1");

            database.ackOrNack(event, Receipt.NACK);

            assertThat(database.isNack("nack-event-1")).isTrue();
            assertThat(database.isAck("nack-event-1")).isFalse();
        }

        @Test
        @DisplayName("Should count NACK occurrences")
        void countNacks() {
            SpsEvent event = testEvent("multi-nack-1");

            database.ackOrNack(event, Receipt.NACK);
            database.ackOrNack(event, Receipt.NACK);
            database.ackOrNack(event, Receipt.NACK);
            database.ackOrNack(event, Receipt.ACK);

            assertThat(database.nackCount("multi-nack-1")).isEqualTo(3);
            assertThat(database.isAck("multi-nack-1")).isTrue();
        }

        @Test
        @DisplayName("Should track ABANDONED receipt")
        void trackAbandoned() {
            SpsEvent event = testEvent("abandoned-1");

            database.ackOrNack(event, Receipt.ABANDONED);

            assertThat(database.isAbandoned("abandoned-1")).isTrue();
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Operations")
    class CircuitBreakerOperations {

        @Test
        @DisplayName("Should trip and reset circuit")
        void tripAndResetCircuit() {
            database.tripCircuit("sub-1", "event.type");

            assertThat(database.isTripped("sub-1", "event.type")).isTrue();

            database.resetCircuit("sub-1", "event.type");

            assertThat(database.isTripped("sub-1", "event.type")).isFalse();
        }

        @Test
        @DisplayName("Should list all tripped circuits")
        void listTrippedCircuits() {
            database.tripCircuit("sub-1", "event.a");
            database.tripCircuit("sub-1", "event.b");
            database.tripCircuit("sub-2", "event.a");

            Map<String, Set<String>> tripped = database.trippedCircuits();

            assertThat(tripped).containsKey("sub-1");
            assertThat(tripped).containsKey("sub-2");
            assertThat(tripped.get("sub-1")).containsExactlyInAnyOrder("event.a", "event.b");
            assertThat(tripped.get("sub-2")).containsExactly("event.a");
        }
    }

    @Nested
    @DisplayName("Leader Election")
    class LeaderElection {

        @Test
        @DisplayName("Should acquire leadership")
        void acquireLeadership() {
            UUID leaderId = UUID.randomUUID();

            boolean acquired = database.takeLeader(leaderId);

            assertThat(acquired).isTrue();
        }

        @Test
        @DisplayName("Should extend own leadership")
        void extendLeadership() {
            UUID leaderId = UUID.randomUUID();

            database.takeLeader(leaderId);
            boolean extended = database.takeLeader(leaderId);

            assertThat(extended).isTrue();
        }

        @Test
        @DisplayName("Should deny leadership to others while active")
        void denyOthersWhileActive() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            database.takeLeader(leader1);
            boolean denied = database.takeLeader(leader2);

            assertThat(denied).isFalse();
        }
    }

    @Nested
    @DisplayName("Config Operations")
    class ConfigOperations {

        @Test
        @DisplayName("Should fetch config")
        void fetchConfig() {
            Config config = database.fetchConfig();

            assertThat(config).isNotNull();
            assertThat(config.gen()).isNotNull();
            assertThat(config.gen().url()).isEqualTo("http://localhost:7201");
        }
    }

    private SpsEvent testEvent(String id) {
        return new SpsEvent() {
            @Override
            public String type() {
                return "test.event";
            }

            @Override
            public String id() {
                return id;
            }

            @Override
            public Map<String, Object> data() {
                return Map.of("test", "data");
            }
        };
    }
}

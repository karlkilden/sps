package com.kildeen.sps.publish;

import com.kildeen.sps.Client;
import com.kildeen.sps.IdWithReceipts;
import com.kildeen.sps.IdWithReceiptsResult;
import com.kildeen.sps.Receipt;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEvents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Sender Tests")
class SenderTest {

    @Test
    @DisplayName("Should send via first delivery type when successful")
    void shouldSendViaFirstDeliveryType() {
        AtomicInteger httpCalls = new AtomicInteger(0);
        Client httpClient = createMockClient(DeliveryType.HTTP, Receipt.ACK, httpCalls);

        Sender sender = new Sender(List.of(httpClient));
        PublishableEvent event = createEvent(List.of(DeliveryType.HTTP));

        IdWithReceiptsResult result = sender.send(event).join();

        assertThat(result.allEvents()).isEqualTo(Receipt.ACK);
        assertThat(httpCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should fallback to second delivery type on connection failure")
    void shouldFallbackOnConnectionFailure() {
        AtomicInteger httpCalls = new AtomicInteger(0);
        AtomicInteger dbCalls = new AtomicInteger(0);

        Client failingHttpClient = createFailingClient(DeliveryType.HTTP, httpCalls);
        Client dbClient = createMockClient(DeliveryType.DATABASE, Receipt.ACK, dbCalls);

        Sender sender = new Sender(List.of(failingHttpClient, dbClient));
        PublishableEvent event = createEvent(List.of(DeliveryType.HTTP, DeliveryType.DATABASE));

        IdWithReceiptsResult result = sender.send(event).join();

        assertThat(result.allEvents()).isEqualTo(Receipt.ACK);
        assertThat(httpCalls.get()).isEqualTo(1);
        assertThat(dbCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should NOT fallback on NACK - NACK goes to retry system")
    void shouldNotFallbackOnNack() {
        AtomicInteger httpCalls = new AtomicInteger(0);
        AtomicInteger dbCalls = new AtomicInteger(0);

        Client nackingHttpClient = createMockClient(DeliveryType.HTTP, Receipt.NACK, httpCalls);
        Client dbClient = createMockClient(DeliveryType.DATABASE, Receipt.ACK, dbCalls);

        Sender sender = new Sender(List.of(nackingHttpClient, dbClient));
        PublishableEvent event = createEvent(List.of(DeliveryType.HTTP, DeliveryType.DATABASE));

        IdWithReceiptsResult result = sender.send(event).join();

        // NACK should be returned, not fallback to DATABASE
        assertThat(result.allEvents()).isEqualTo(Receipt.NACK);
        assertThat(httpCalls.get()).isEqualTo(1);
        assertThat(dbCalls.get()).isEqualTo(0); // Database should NOT be called
    }

    @Test
    @DisplayName("Should fail when all delivery methods exhausted")
    void shouldFailWhenAllDeliveryMethodsExhausted() {
        AtomicInteger httpCalls = new AtomicInteger(0);
        AtomicInteger dbCalls = new AtomicInteger(0);

        Client failingHttpClient = createFailingClient(DeliveryType.HTTP, httpCalls);
        Client failingDbClient = createFailingClient(DeliveryType.DATABASE, dbCalls);

        Sender sender = new Sender(List.of(failingHttpClient, failingDbClient));
        PublishableEvent event = createEvent(List.of(DeliveryType.HTTP, DeliveryType.DATABASE));

        assertThatThrownBy(() -> sender.send(event).join())
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("All delivery methods failed");

        assertThat(httpCalls.get()).isEqualTo(1);
        assertThat(dbCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should skip missing client and try next delivery type")
    void shouldSkipMissingClientAndTryNext() {
        AtomicInteger dbCalls = new AtomicInteger(0);
        Client dbClient = createMockClient(DeliveryType.DATABASE, Receipt.ACK, dbCalls);

        // Only DB client, but event requests HTTP first
        Sender sender = new Sender(List.of(dbClient));
        PublishableEvent event = createEvent(List.of(DeliveryType.HTTP, DeliveryType.DATABASE));

        IdWithReceiptsResult result = sender.send(event).join();

        assertThat(result.allEvents()).isEqualTo(Receipt.ACK);
        assertThat(dbCalls.get()).isEqualTo(1);
    }

    private Client createMockClient(DeliveryType type, Receipt response, AtomicInteger callCounter) {
        return new Client() {
            @Override
            public CompletableFuture<IdWithReceiptsResult> post(Subscriptions.Subscription subscription, SpsEvents spsEvents) {
                callCounter.incrementAndGet();
                return CompletableFuture.completedFuture(new IdWithReceiptsResult() {
                    @Override
                    public Receipt allEvents() {
                        return response;
                    }

                    @Override
                    public List<IdWithReceipts.IdWithReceipt> idWithReceipts() {
                        return List.of();
                    }
                });
            }

            @Override
            public EnumSet<DeliveryType> supports() {
                return EnumSet.of(type);
            }
        };
    }

    private Client createFailingClient(DeliveryType type, AtomicInteger callCounter) {
        return new Client() {
            @Override
            public CompletableFuture<IdWithReceiptsResult> post(Subscriptions.Subscription subscription, SpsEvents spsEvents) {
                callCounter.incrementAndGet();
                return CompletableFuture.failedFuture(new RuntimeException("Connection refused"));
            }

            @Override
            public EnumSet<DeliveryType> supports() {
                return EnumSet.of(type);
            }
        };
    }

    private PublishableEvent createEvent(List<DeliveryType> deliveryTypes) {
        SpsEvent event = new EventFork.ForkedEvents.Fork.ForkSpsEvent("test_event", "event-1", Map.of("key", "value"));
        Subscriptions.Subscription.Subscriber subscriber = new Subscriptions.Subscription.Subscriber(
                "test-sub", "http://localhost:8080", new PublishPolicy(deliveryTypes));
        Subscriptions.Subscription subscription = new Subscriptions.Subscription(subscriber, "test_event", Map.of());

        return new EventFork.ForkedEvents.Fork(subscription, List.of(event), deliveryTypes, Instant.now());
    }
}

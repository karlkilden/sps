package com.kildeen.sps.inlet;

import com.kildeen.sps.CircuitBreakers;
import com.kildeen.sps.IdWithReceipts;
import com.kildeen.sps.SpsEventType;
import com.kildeen.sps.SpsEvents;
import com.kildeen.sps.persistence.Database;
import com.kildeen.sps.publish.RetryPolicies;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class InletDI implements Inlet {

    private final ReceiveEvent receiveEvent;

    private InletDI(Builder builder) {
        receiveEvent = builder.receiveEvent;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public IdWithReceipts receive(SpsEvents events) {
        List<IdWithReceipts.IdWithReceipt> result = new ArrayList<>();
        events.spsEvents().forEach(e -> result.add(new IdWithReceipts.IdWithReceipt(e.id(), e.type(),
                receiveEvent.receive(e),
                Instant.now())));
        return new IdWithReceipts(result);
    }

    public static final class Builder {
        private Collection<Receiver> spsReceivers;
        private Database database;
        private ReceiveEvent receiveEvent;

        private CircuitBreakers circuitBreakers;

        private String subId;
        private CircuitBreakerExecutor circuitBreakerExecutor;

        private Builder() {
        }

        public Builder withDatabase(Database database) {
            this.database = database;
            return this;
        }

        public Builder withCircuitBreakers(CircuitBreakers circuitBreakers) {
            this.circuitBreakers = circuitBreakers;
            return this;
        }

        public Builder withReceivers(Collection<Receiver> receivers) {
            this.spsReceivers = receivers;
            return this;
        }

        public Builder withSubId(String subId) {
            this.subId = subId;
            return this;
        }

        public InletDI build() {
            if (database == null) {
                throw new NullPointerException("No database configured");
            }
            if (subId == null) {
                throw new NullPointerException("No subId configured");

            }
            Receiver healthcheckReceiver = new HealthCheckReceiver();

            Map<String, Receiver> mapped = Receiver.map(spsReceivers.stream()
                    .map(r -> new CircuitBreakEnabledReceiver(subId, r, findBreaker(r, circuitBreakers))));
            mapped.put(SpsEventType.healthcheck_01.toString(), healthcheckReceiver);
            this.circuitBreakerExecutor = new CircuitBreakerExecutor(mapped, database);
            receiveEvent = new ReceiveEvent(mapped,
                    new AckOrNackEvent(new RetryQueue(), new AckOrNackEventsImpl(database)));

            return new InletDI(this);

        }

        private CircuitBreakers.CircuitBreaker findBreaker(Receiver r, CircuitBreakers circuitBreakers) {

            if (circuitBreakers == null) {
                return CircuitBreakers.CircuitBreaker.defaultForType(r.eventType());
            }

            return circuitBreakers.circuitBreakers().stream().filter(breaker -> r.eventType().equals(breaker.type()))
                    .findFirst().orElse(circuitBreakers.defaultBreaker());
        }
    }
}

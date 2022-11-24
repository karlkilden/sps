package com.kildeen.sps.inlet;

import com.kildeen.sps.IdWithReceipts;
import com.kildeen.sps.SpsEvents;
import com.kildeen.sps.persistence.Database;

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
        events.spsEvents().forEach(e -> result.add(new IdWithReceipts.IdWithReceipt(e.id(),
                receiveEvent.receive(e),
                Instant.now())));
        return new IdWithReceipts(result);
    }

    public static final class Builder {
        private Collection<Receiver> spsReceivers;
        private Database database;
        private ReceiveEvent receiveEvent;

        private Builder() {
        }

        public Builder withDatabase(Database database) {
            this.database = database;
            return this;
        }

        public Builder withReceivers(Collection<Receiver> receivers) {
            this.spsReceivers = receivers;
            return this;
        }

        public InletDI build() {
            if (database == null) {
                throw new NullPointerException("No database configured");
            }
            Map<String, Receiver> mapped = Receiver.map(spsReceivers);
            receiveEvent = new ReceiveEvent(mapped,
                    new AckOrNackEvent(new RetryQueue(), new AckOrNackEventsImpl(database)));

            return new InletDI(this);

        }
    }
}

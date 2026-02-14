package com.kildeen.sps.publish;

import com.kildeen.sps.BasicSpsEvents;
import com.kildeen.sps.Client;
import com.kildeen.sps.IdWithReceiptsResult;
import com.kildeen.sps.Receipt;
import com.kildeen.sps.IdWithReceipts;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEvents;
import com.kildeen.sps.json.JsonProvider;
import com.kildeen.sps.persistence.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Database-based transport client for fallback delivery.
 * Inserts events into the transport queue table when HTTP delivery fails.
 * Subscribers poll the queue to receive events.
 */
public class DatabaseClient implements Client {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseClient.class);

    private final Database database;

    public DatabaseClient(Database database) {
        this.database = database;
    }

    @Override
    public CompletableFuture<IdWithReceiptsResult> post(Subscriptions.Subscription subscription, SpsEvents spsEvents) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String subscriberId = subscription.subscriber().subId();
                String eventType = spsEvents.eventType();

                // Convert to BasicSpsEvents for proper serialization/deserialization
                List<BasicSpsEvents.BasicSpsEvent> basicEvents = spsEvents.spsEvents().stream()
                        .map(e -> new BasicSpsEvents.BasicSpsEvent(e.type(), e.id(), e.data()))
                        .toList();
                BasicSpsEvents basicSpsEvents = new BasicSpsEvents(eventType, basicEvents);
                String payload = JsonProvider.json().write(basicSpsEvents);

                // Insert each event into the transport queue
                for (var event : spsEvents.spsEvents()) {
                    LOG.info("Inserting event {} into transport queue for subscriber {}", 
                            event.id(), subscriberId);
                    database.insertTransportEvent(event.id(), eventType, subscriberId, payload);
                }

                // Return ACK - the event is now safely in the database
                return new IdWithReceiptsResult() {
                    @Override
                    public Receipt allEvents() {
                        return Receipt.ACK;
                    }

                    @Override
                    public List<IdWithReceipts.IdWithReceipt> idWithReceipts() {
                        throw new UnsupportedOperationException("Not implemented for database transport");
                    }
                };
            } catch (Exception e) {
                LOG.error("Failed to insert event into transport queue", e);
                return new IdWithReceiptsResult() {
                    @Override
                    public Receipt allEvents() {
                        return Receipt.NACK;
                    }

                    @Override
                    public List<IdWithReceipts.IdWithReceipt> idWithReceipts() {
                        throw new UnsupportedOperationException("Not implemented for database transport");
                    }
                };
            }
        });
    }

    @Override
    public EnumSet<DeliveryType> supports() {
        return EnumSet.of(DeliveryType.DATABASE);
    }
}

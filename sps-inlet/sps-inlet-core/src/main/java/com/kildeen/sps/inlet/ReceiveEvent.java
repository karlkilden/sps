package com.kildeen.sps.inlet;

import com.kildeen.sps.Receipt;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.persistence.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Processes incoming events with deduplication and proper error handling.
 */
public class ReceiveEvent {

    private static final Logger LOG = LoggerFactory.getLogger(ReceiveEvent.class);

    private final Map<String, Receiver> receivers;
    private final AckOrNackEvent ackOrNackEvent;
    private final Database database;

    ReceiveEvent(Map<String, Receiver> receivers, AckOrNackEvent ackOrNackEvent) {
        this(receivers, ackOrNackEvent, null);
    }

    ReceiveEvent(Map<String, Receiver> receivers, AckOrNackEvent ackOrNackEvent, Database database) {
        this.receivers = receivers;
        this.ackOrNackEvent = ackOrNackEvent;
        this.database = database;
    }

    Receipt receive(SpsEvent spsEvent) {
        // Deduplication check: skip if already processed
        if (database != null && isAlreadyProcessed(spsEvent)) {
            LOG.debug("Event {} already processed, returning ACK (deduplicated)", spsEvent.id());
            return Receipt.ACK;
        }

        Receipt receipt = Receipt.UNKNOWN;

        try {
            Receiver receiver = receivers.get(spsEvent.type());
            if (receiver == null) {
                LOG.error("No receiver registered for event type {}, event id {}", spsEvent.type(), spsEvent.id());
                receipt = Receipt.NACK;
                return receipt;
            }
            receiver.receive(spsEvent);
            try {
                ackOrNackEvent.ack(spsEvent);
                receipt = Receipt.ACK;
            } catch (Exception e) {
                LOG.error("Failed to acknowledge event {}: {}", spsEvent.id(), e.getMessage(), e);
                receipt = Receipt.ACK_FAILURE;
            }
        } catch (Exception e) {
            LOG.error("Event processing failed for {}: {}", spsEvent.id(), e.getMessage(), e);
            try {
                ackOrNackEvent.nack(spsEvent);
                receipt = Receipt.NACK;
            } catch (Exception ex) {
                LOG.error("Failed to NACK event {}: {}", spsEvent.id(), ex.getMessage(), ex);
                receipt = Receipt.NACK_FAILURE;
            }
        } finally {
            if (receipt == Receipt.UNKNOWN) {
                throw new RuntimeException("unknown system state caused by:" + spsEvent.id());
            }
            if (receipt == Receipt.ACK_FAILURE || receipt == Receipt.NACK_FAILURE) {
                ackOrNackEvent.retry(spsEvent.id(), receipt);
            }
        }
        return receipt;
    }

    /**
     * Check if event was already successfully processed (deduplication).
     */
    private boolean isAlreadyProcessed(SpsEvent spsEvent) {
        try {
            // Extract subscriber ID from forked event ID (format: {originalId}_{subscriberId})
            String eventId = spsEvent.id();
            String subscriberId = extractSubscriberId(eventId);
            return database.isAck(eventId, subscriberId);
        } catch (Exception e) {
            LOG.warn("Deduplication check failed for {}, proceeding with processing", spsEvent.id(), e);
            return false;
        }
    }

    private String extractSubscriberId(String forkedEventId) {
        int lastUnderscore = forkedEventId.lastIndexOf('_');
        if (lastUnderscore > 0) {
            return forkedEventId.substring(lastUnderscore + 1);
        }
        return "";
    }
}

package com.kildeen.sps.dlq;

import com.kildeen.sps.Contract;
import com.kildeen.sps.SpsEvent;

import java.util.List;

/**
 * Dead Letter Queue for events that have been abandoned after exhausting retries.
 * Provides inspection, replay, and purge capabilities for operational troubleshooting.
 */
@Contract
public interface DeadLetterQueue {

    /**
     * Sends an event to the dead letter queue.
     *
     * @param event the event that failed
     * @param reason description of why the event was abandoned
     * @param retryCount number of retry attempts made
     */
    void send(SpsEvent event, String reason, int retryCount);

    /**
     * Peeks at entries in the dead letter queue without removing them.
     *
     * @param limit maximum number of entries to return
     * @return list of dead letter entries
     */
    List<DeadLetterEntry> peek(int limit);

    /**
     * Replays an event from the dead letter queue.
     * The event is removed from the DLQ and resubmitted for processing.
     *
     * @param eventId the ID of the event to replay
     * @return true if event was found and replayed
     */
    boolean replay(String eventId);

    /**
     * Permanently removes an event from the dead letter queue.
     *
     * @param eventId the ID of the event to purge
     * @return true if event was found and purged
     */
    boolean purge(String eventId);

    /**
     * Returns the total count of events in the dead letter queue.
     */
    long count();
}

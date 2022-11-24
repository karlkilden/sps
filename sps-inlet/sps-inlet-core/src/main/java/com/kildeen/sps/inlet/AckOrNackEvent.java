package com.kildeen.sps.inlet;

import com.kildeen.sps.Receipt;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AckOrNackEvent {
    private static final ScheduledExecutorService RETRY_QUEUE_SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(1);
    private final RetryQueue retryQueue;
    private final AckOrNackEvents ackOrNackEvents;

    public AckOrNackEvent(RetryQueue retryQueue, AckOrNackEvents ackOrNackEvents) {
        this.retryQueue = retryQueue;
        this.ackOrNackEvents = ackOrNackEvents;
        RETRY_QUEUE_SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::retryFromQueue,
                200,
                2000,
                TimeUnit.MILLISECONDS);
    }

    public void ack(String id) {
        ackOrNackEvents.ack(id);
    }

    public void nack(String id) {
        ackOrNackEvents.nack(id);
    }

    public void retry(String id, Receipt receipt) {
        retryQueue.save(id, receipt);
    }

    private void retryFromQueue() {
        var retry = retryQueue.next();
        if (retry != null) {
            retry.retries().getAndIncrement();

            try {
                if (retry.receipt() == Receipt.ACK) {
                    ack(retry.id());
                } else {
                    nack(retry.id());
                }
            } catch (Exception e) {
                //TODO: trace
                retryQueue.save(retry);
            }
        }
    }
}

package com.kildeen.sps.inlet;

import com.kildeen.sps.Receipt;
import com.kildeen.sps.SpsEvent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AckOrNackEvent {
    private final RetryQueue retryQueue;
    private final AckOrNackEvents ackOrNackEvents;

    public AckOrNackEvent(RetryQueue retryQueue, AckOrNackEvents ackOrNackEvents) {
        this.retryQueue = retryQueue;
        this.ackOrNackEvents = ackOrNackEvents;

    }

    public void ack(SpsEvent event) {
        ackOrNackEvents.ack(event);
    }

    public void nack(SpsEvent event) {
        ackOrNackEvents.nack(event);
    }

    public void retry(String id, Receipt receipt) {
        retryQueue.save(id, receipt);
    }

}

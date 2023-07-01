package com.kildeen.sps.inlet;

import com.kildeen.sps.CircuitBreakers;
import com.kildeen.sps.SpsEvent;

public class CircuitBreakEnabledReceiver implements Receiver {
    private final String subId;
    private final Receiver receiver;
    private final CircuitBreakers.CircuitBreaker breaker;
    private TrippedCircuit trippedCircuit;


    public CircuitBreakEnabledReceiver(String subId, Receiver receiver,
                                       CircuitBreakers.CircuitBreaker breaker) {
        this.subId = subId;
        this.receiver = receiver;
        this.breaker = breaker;
    }

    @Override
    public void receive(SpsEvent spsEvent) {
        if (tripped()) {
            if (trippedCircuit.isAttemptingReset()) {
                try {
                    receiver.receive(spsEvent);
                    trippedCircuit.recordAckForCurrentTier();
                } catch (Exception e) {
                    e.printStackTrace();
                    trippedCircuit.restart();
                    throw e;
                }
            }
            throw new IllegalStateException("Cannot receive due to circuit breaker tripped");
        }
        receiver.receive(spsEvent);
    }

    @Override
    public String eventType() {
        return receiver.eventType();
    }

    public void trip() {
        if (trippedCircuit == null) {
            this.trippedCircuit = new TrippedCircuit(breaker);
        }
    }

    public CircuitBreakers.CircuitBreaker breaker() {
        return breaker;
    }

    public boolean tripped() {
        System.out.println(trippedCircuit);
        System.out.println("tripped " + (trippedCircuit != null && trippedCircuit.hasRemainingEvents()));
        return trippedCircuit != null && trippedCircuit.hasRemainingEvents();
    }

    public TrippedCircuit trippedCircuit() {
        return trippedCircuit;
    }

    public String subId() {
        return subId;
    }
}

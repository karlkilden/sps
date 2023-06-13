package com.kildeen.sps.inlet;

import com.kildeen.sps.CircuitBreakers;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TrippedCircuit {

    private final Set<Integer> remainingResetCircuitTiers = new ConcurrentSkipListSet<>();
    private final CircuitBreakers.CircuitBreaker circuitBreaker;
    AtomicInteger currentResetCircuitTier;
    AtomicInteger eventsLeftForTier;

    AtomicInteger totalTrippedCount = new AtomicInteger();

    private ScheduledExecutorService executorService;


    public TrippedCircuit(CircuitBreakers.CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
        executorService = Executors.newScheduledThreadPool(2);
        initState();
    }

    public void testResetOnCircuit() {

        if (currentResetCircuitTier != null) {
            return;
        }
        for (int tier : circuitBreaker.resetCircuitTiers()) {
            if (remainingResetCircuitTiers.contains(tier)) {
                remainingResetCircuitTiers.remove(tier);
                currentResetCircuitTier = new AtomicInteger(tier);
                eventsLeftForTier = new AtomicInteger(tier);
            }
        }
    }

    public Set<Integer> remainingResetCircuitTiers() {
        return remainingResetCircuitTiers;
    }

    public void recordAckForCurrentTier() {
        eventsLeftForTier.getAndDecrement();
        if (eventsLeftForTier.intValue() <= 0) {
            remainingResetCircuitTiers.remove(currentResetCircuitTier.intValue());
            testResetOnCircuit();
        }
    }

    public void restart() {
        totalTrippedCount.getAndIncrement();
        initState();
    }

    private void initState() {
        remainingResetCircuitTiers.addAll(circuitBreaker.resetCircuitTiers());
        currentResetCircuitTier = null;
        eventsLeftForTier = null;
        executorService.schedule(this::testResetOnCircuit,
                circuitBreaker.waitBetweenResetAttemptsInSeconds(), TimeUnit.SECONDS);
    }

    public boolean isAttemptingReset() {
        return currentResetCircuitTier != null;
    }
}

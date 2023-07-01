package com.kildeen.sps.inlet;

import com.kildeen.sps.CircuitBreakers;

import java.util.Set;
import java.util.StringJoiner;
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
        System.out.println("testResetOnCircuit");
        if (currentResetCircuitTier != null) {
            return;
        }
        System.out.println("testResetOnCircuit 2");


        for (int tier : circuitBreaker.resetCircuitTiers()) {
            if (remainingResetCircuitTiers.contains(tier)) {
                remainingResetCircuitTiers.remove(tier);
                currentResetCircuitTier = new AtomicInteger(tier);
                System.out.println("line 42");
                eventsLeftForTier = new AtomicInteger(tier);
                System.out.println("Events left for tier " + tier);
            }
        }
    }

    public void recordAckForCurrentTier() {
        System.out.println("Record ack " + eventsLeftForTier);
        eventsLeftForTier.getAndDecrement();
        if (eventsLeftForTier.intValue() <= 0) {
            System.out.println("eventsLeftForTier is <=0");
            remainingResetCircuitTiers.remove(currentResetCircuitTier.intValue());
            currentResetCircuitTier.set(0);
            System.out.println("line 56");
            System.out.println("currentResetCircuitTier0 " + currentResetCircuitTier);
            testResetOnCircuit();
            System.out.println("currentResetCircuitTier1 " + currentResetCircuitTier);

        }
    }

    public void restart() {
        System.out.println("restart");
        totalTrippedCount.getAndIncrement();
        initState();
    }

    private void initState() {
        remainingResetCircuitTiers.addAll(circuitBreaker.resetCircuitTiers());
        eventsLeftForTier = null;
        executorService.schedule(this::testResetOnCircuit,
                circuitBreaker.waitBetweenResetAttemptsInSeconds(), TimeUnit.SECONDS);
        testResetOnCircuit();
    }

    public boolean isAttemptingReset() {
        return currentResetCircuitTier != null;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TrippedCircuit.class.getSimpleName() + "[", "]")
                .add("remainingResetCircuitTiers=" + remainingResetCircuitTiers)
                .add("circuitBreaker=" + circuitBreaker)
                .add("currentResetCircuitTier=" + currentResetCircuitTier)
                .add("eventsLeftForTier=" + eventsLeftForTier)
                .add("totalTrippedCount=" + totalTrippedCount)
                .add("executorService=" + executorService)
                .toString();
    }

    public boolean hasRemainingEvents() {
        System.out.println("hasRemainingEvents");
        System.out.println(remainingResetCircuitTiers);
        System.out.println("currentResetCircuitTier3 " + currentResetCircuitTier);
        return remainingResetCircuitTiers.isEmpty() == false ||
                (currentResetCircuitTier == null || currentResetCircuitTier.intValue() > 1);
    }
}

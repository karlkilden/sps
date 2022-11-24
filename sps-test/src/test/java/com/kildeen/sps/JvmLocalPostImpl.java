package com.kildeen.sps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kildeen.sps.inlet.Inlet;
import com.kildeen.sps.publish.JvmLocalPost;
import com.kildeen.sps.publish.Subscriptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class JvmLocalPostImpl implements JvmLocalPost {
    static final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Inlet> inlets;

    public JvmLocalPostImpl(List<Inlet> inlets) {
        this.inlets = inlets;
    }

    @Override
    public CompletableFuture<IdWithReceiptsResult> take(Subscriptions.Subscription subscription, String json) {
        BasicSpsEvents events;
        try {
            events = objectMapper.readValue(json, BasicSpsEvents.class);
            List<IdWithReceipts> results = new ArrayList<>();
            inlets.forEach(i -> results.add(i.receive(new SpsEvents(events))));
            List<IdWithReceipts.IdWithReceipt> all =
                    results.stream()
                            .flatMap(result -> result.idWithReceipts().stream())
                            .toList();
            Receipt allEventsReceipt = results.stream().map(IdWithReceipts::idWithReceipts)
                    .distinct().count() == 1 ? results.get(0).idWithReceipts().get(0).receipt() : Receipt.UNKNOWN;

            return CompletableFuture.completedFuture(new IdWithReceiptsResult() {
                @Override
                public List<IdWithReceipts.IdWithReceipt> idWithReceipts() {
                    return all;
                }

                @Override
                public Receipt allEvents() {
                    return allEventsReceipt;
                }
            });
        } catch (JsonProcessingException e) {
            throw ExceptionUtils.rethrow(e);
        }
    }
}

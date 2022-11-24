package com.kildeen.sps;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record IdWithReceipts(List<IdWithReceipt> idWithReceipts) {

    public IdWithReceiptsResponse toResponse() {
        Set<Receipt> receipts = idWithReceipts.stream()
                .map(IdWithReceipt::receipt)
                .collect(Collectors.toSet());
        Receipt allEvents;
        if (receipts.size() == 1) {
            allEvents = receipts.iterator().next();
        }
        else {
            allEvents = Receipt.UNKNOWN;
        }
        return new IdWithReceiptsResponse(allEvents, receipts);
    }

    public record IdWithReceipt(String id, Receipt receipt, Instant instant) {
    }
}

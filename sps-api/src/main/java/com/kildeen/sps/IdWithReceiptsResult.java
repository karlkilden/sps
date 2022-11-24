package com.kildeen.sps;

import java.util.List;

public interface IdWithReceiptsResult {

    default Receipt allEvents() {
        return Receipt.UNKNOWN;
    }

    List<IdWithReceipts.IdWithReceipt> idWithReceipts();
}

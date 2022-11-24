package com.kildeen.sps.inlet;

import com.kildeen.sps.Receipt;
import com.kildeen.sps.persistence.Database;

public class AckOrNackEventsImpl implements AckOrNackEvents {

    private final Database database;

    public AckOrNackEventsImpl(Database database) {
        this.database = database;
    }

    @Override
    public void ack(String id) {
        database.ackOrNack(id, Receipt.ACK);
    }

    @Override
    public void nack(String id) {
        database.ackOrNack(id, Receipt.NACK);
    }
}

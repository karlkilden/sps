package com.kildeen.sps.inlet;

import com.kildeen.sps.Receipt;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.persistence.Database;

public class AckOrNackEventsImpl implements AckOrNackEvents {

    private final Database database;

    public AckOrNackEventsImpl(Database database) {
        this.database = database;
    }

    @Override
    public void ack(SpsEvent event) {
        database.ackOrNack(event, Receipt.ACK);
    }

    @Override
    public void nack(SpsEvent event) {
        database.ackOrNack(event, Receipt.NACK);
    }
}

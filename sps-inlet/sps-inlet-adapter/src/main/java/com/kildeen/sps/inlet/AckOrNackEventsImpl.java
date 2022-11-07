package com.kildeen.sps.inlet;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.Database;
import com.kildeen.sps.ReceiptTuple;

public class AckOrNackEventsImpl implements AckOrNackEvents {

    private final Database database;

    public AckOrNackEventsImpl(Database database) {
        this.database = database;
    }

    @Override
    public void ack(String id) {
        database.ackOrNack(id, ReceiptTuple.ACK);
    }

    @Override
    public void nack(String id) {
        database.ackOrNack(id, ReceiptTuple.NACK);
    }
}

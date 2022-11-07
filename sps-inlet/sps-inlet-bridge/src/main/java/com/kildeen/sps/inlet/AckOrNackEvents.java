package com.kildeen.sps.inlet;

public interface AckOrNackEvents {
    void ack(String id);

    void nack(String id);
}

package com.kildeen.sps.inlet;

import com.kildeen.sps.SpsEvent;

public interface AckOrNackEvents {
    void ack(SpsEvent event);

    void nack(SpsEvent event);
}

package com.kildeen.sps.inlet;

import com.kildeen.sps.Bridge;
import com.kildeen.sps.SpsEvent;

@Bridge
public interface AckOrNackEvents {
    void ack(SpsEvent event);

    void nack(SpsEvent event);
}

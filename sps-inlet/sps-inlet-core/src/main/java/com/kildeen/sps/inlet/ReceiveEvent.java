package com.kildeen.sps.inlet;


import com.kildeen.sps.Receipt;
import com.kildeen.sps.SpsEvent;

import java.util.Map;

public class ReceiveEvent {

    private final Map<String, Receiver> receivers;
    private final AckOrNackEvent ackOrNackEvent;

    ReceiveEvent(Map<String, Receiver> receivers, AckOrNackEvent ackOrNackEvent) {
        this.receivers = receivers;
        this.ackOrNackEvent = ackOrNackEvent;
    }

    Receipt receive(SpsEvent spsEvent) {
        Receipt receipt = Receipt.UNKNOWN;

        try {
            receivers.get(spsEvent.type()).receive(spsEvent);
            try {
                ackOrNackEvent.ack(spsEvent.id());
                receipt = Receipt.ACK;
            } catch (Exception e) {
                receipt = Receipt.ACK_FAILURE;
            }
        } catch (Exception e) {
            try {
                ackOrNackEvent.nack(spsEvent.id());
                receipt = Receipt.NACK;
            } catch (Exception ex) {
                receipt = Receipt.NACK_FAILURE;
            }
        } finally {
            if (receipt == Receipt.UNKNOWN) {
                throw new RuntimeException("unknown system state caused by:" + spsEvent.id());
            }
            if (receipt == Receipt.ACK_FAILURE || receipt == Receipt.NACK_FAILURE) {
                ackOrNackEvent.retry(spsEvent.id(), receipt);
            }
        }
        return receipt;

    }
}

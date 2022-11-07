package com.kildeen.sps.inlet;

import com.kildeen.sps.SpsEvent;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Contract
public interface Receiver {
    //TODO: try catch behaviour / try etc
    void receive(SpsEvent spsEvent);

    String eventType();

    static Map<String, Receiver> map(Receiver... receivers) {
        return Arrays.stream(receivers)
                .collect(Collectors.toMap(Receiver::eventType, Function.identity()));
    }
}

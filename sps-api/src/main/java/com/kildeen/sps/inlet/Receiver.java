package com.kildeen.sps.inlet;

import com.kildeen.sps.SpsEvent;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface Receiver {
    static Map<String, Receiver> map(Collection<Receiver> receivers) {
        return receivers.stream()
                .collect(Collectors.toMap(Receiver::eventType, Function.identity()));
    }

    void receive(SpsEvent spsEvent);

    String eventType();
}

package com.kildeen.sps.publish;

import com.kildeen.sps.SpsEvent;

import java.util.Collection;

public interface Publish {

    void publish(String type, Collection<SpsEvent> events);
}

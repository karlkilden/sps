package com.kildeen.sps.inlet;

import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEvents;

@Contract
public interface Inlet {

    void receive(SpsEvents events);


}

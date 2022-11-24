package com.kildeen.sps.inlet;

import com.kildeen.sps.IdWithReceipts;
import com.kildeen.sps.SpsEvents;

public interface Inlet {

    IdWithReceipts receive(SpsEvents events);

}

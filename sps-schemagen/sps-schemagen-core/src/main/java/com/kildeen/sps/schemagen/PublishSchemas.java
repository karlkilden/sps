package com.kildeen.sps.schemagen;

import com.kildeen.sps.Bridge;
import com.kildeen.sps.Schemas;
import com.kildeen.sps.SpsEvent;
@Bridge
public interface PublishSchemas {
    void publish(SpsEvent spsEvent, Schemas schemas);
}

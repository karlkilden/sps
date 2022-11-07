package com.kildeen.sps.schemagen;

import com.kildeen.sps.SpsEvent;

import java.util.List;

public interface PublishSchemas {
    void publish(SpsEvent spsEvent, List<Schema> schemas);
}

package com.kildeen.sps.publish;

import com.kildeen.sps.Bridge;
import com.kildeen.sps.Schemas;

@Bridge
public interface FetchSchemas {
    Schemas.Schema fetch(String eventType);
}

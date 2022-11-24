package com.kildeen.sps.publish;

import com.kildeen.sps.Schemas;

public interface FetchSchemas {
    Schemas.Schema fetch(String eventType);
}

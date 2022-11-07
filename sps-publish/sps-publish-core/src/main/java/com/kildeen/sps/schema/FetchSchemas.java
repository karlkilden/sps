package com.kildeen.sps.schema;

import java.util.List;

public interface FetchSchemas {
    List<PublishSchemaTuple> fetch(List<String> tags);
}

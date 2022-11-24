package com.kildeen.sps.schemagen;

import com.kildeen.sps.Schemas;

import java.util.Set;

public interface FetchSchemas {
    Schemas fetch(Set<String> tags);
}

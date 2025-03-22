package com.kildeen.sps.schemagen;

import com.kildeen.sps.Bridge;
import com.kildeen.sps.Schemas;

import java.util.Set;
@Bridge
public interface FetchSchemas {
    Schemas fetch(Set<String> tags);
}

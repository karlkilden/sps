package com.kildeen.sps.schemagen;


import com.kildeen.sps.Bridge;
import com.kildeen.sps.Schemas;
@Bridge
public interface AddSchemas {

    void add(Schemas.Schema schema);
}

package com.kildeen.sps;

import java.util.ArrayList;
import java.util.List;

public class LongTuples<T1, T2> extends ArrayList<LongTuple<T1, T2>> {

    public LongTuples(List<LongTuple<T1, T2>> items) {
        this.addAll(items);
    }

    public List<Long> ids() {
        return this.stream().map(LongTuple::getId).toList();
    }

    public LongTuple<T1, T2> byId(long id) {
        return stream().filter(tuple -> tuple.getId() == id).findFirst().orElseThrow();
    }
}

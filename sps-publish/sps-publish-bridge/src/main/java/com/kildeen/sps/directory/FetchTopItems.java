package com.kildeen.sps.directory;

import com.kildeen.sps.LongTuples;

import java.util.List;

public interface FetchTopItems {

    LongTuples<Long, TopItemTuple> fetch(List<Long> itemIds);
}

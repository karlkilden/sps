package com.kildeen.sps.directory;

import com.kildeen.sps.LongTuples;

public interface SearchTopItems {
    LongTuples<String, Integer> search();
}

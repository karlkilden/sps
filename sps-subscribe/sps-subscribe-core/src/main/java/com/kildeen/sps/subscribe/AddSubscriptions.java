package com.kildeen.sps.subscribe;

import com.kildeen.sps.Bridge;

@Bridge
public interface AddSubscriptions {
    void add(Subscription subscription);
}

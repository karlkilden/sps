package com.kildeen.sps;

import com.kildeen.sps.publish.DeliveryType;
import com.kildeen.sps.publish.Subscriptions;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

public interface Client {

    CompletableFuture<IdWithReceiptsResult> post(Subscriptions.Subscription subscription, SpsEvents spsEvents);

    EnumSet<DeliveryType> supports();

}

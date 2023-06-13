package com.kildeen.sps.publish;

import com.kildeen.sps.Client;
import com.kildeen.sps.IdWithReceiptsResult;
import com.kildeen.sps.SpsEvents;
import com.kildeen.sps.json.JsonProvider;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

public class SameJVMClient implements Client {
    private final JvmLocalPost jvmLocalPost;

    public SameJVMClient(JvmLocalPost jvmLocalPost) {
        this.jvmLocalPost = jvmLocalPost;
    }

    @Override
    public CompletableFuture<IdWithReceiptsResult> post(Subscriptions.Subscription subscription, SpsEvents spsEvents) {
        return jvmLocalPost.take(subscription, JsonProvider.json().write(spsEvents));
    }

    @Override
    public EnumSet<DeliveryType> supports() {
        return EnumSet.allOf(DeliveryType.class);
    }
}

package com.kildeen.sps.publish;

import com.kildeen.sps.IdWithReceiptsResult;

import java.util.concurrent.CompletableFuture;

public interface JvmLocalPost {
    CompletableFuture<IdWithReceiptsResult> take(Subscriptions.Subscription subscription, String json);
}

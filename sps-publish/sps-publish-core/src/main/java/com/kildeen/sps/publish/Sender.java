package com.kildeen.sps.publish;

import com.kildeen.sps.Client;
import com.kildeen.sps.IdWithReceiptsResult;
import com.kildeen.sps.SpsEvents;

import java.util.concurrent.CompletableFuture;

public class Sender {
    Client client;

    public Sender(Client client) {
        this.client = client;
    }

    public CompletableFuture<IdWithReceiptsResult> send(PublishableEvent fork) {
        SpsEvents spsEvents = new SpsEvents(fork.subscription().eventType(), fork.forkedEvents());

        CompletableFuture<IdWithReceiptsResult> post = client.post(fork.subscription(), spsEvents);
        return post;

    }
}

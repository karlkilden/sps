package com.kildeen.sps.publish;

import com.kildeen.sps.Client;
import com.kildeen.sps.IdWithReceiptsResult;
import com.kildeen.sps.SpsEvents;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class Sender {
    private final List<Client> clients;

    public Sender(List<Client> clients) {
        this.clients = clients;
    }

    public CompletableFuture<IdWithReceiptsResult> send(PublishableEvent fork) {

        for (DeliveryType deliveryType : fork.deliveryTypes()) {
            for (Client client : clients) {
                if (client.supports().contains(deliveryType)) {
                    SpsEvents spsEvents = new SpsEvents(fork.subscription().eventType(), fork.forkedEvents());
                    CompletableFuture<IdWithReceiptsResult> post =
                            client.post(fork.subscription(), spsEvents);
                    return post;
                }
            }
        }
        throw new RuntimeException("No client found that supports any of:" + fork.deliveryTypes());
    }
}

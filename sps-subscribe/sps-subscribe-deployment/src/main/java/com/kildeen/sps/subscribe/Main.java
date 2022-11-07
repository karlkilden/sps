package com.kildeen.sps.subscribe;

import com.kildeen.sps.DataBaseProvider;
import com.kildeen.sps.SpsEvents;
import com.kildeen.sps.inlet.Inlet;
import com.kildeen.sps.inlet.InletDI;
import com.kildeen.sps.inlet.Receiver;
import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) {

        ExecutorService executor = Executors.newFixedThreadPool(10);

        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.runAsync(Main::run);

        voidCompletableFuture.join();

    }

    private static void run() {
        Javalin app = Javalin.create();
        Subscribe subscribe = SubscribeDI.INSTANCE.inject();
        SubscriptionReceiver addSchemaReceiver = new SubscriptionReceiver(subscribe);
        Inlet inlet = InletDI.INSTANCE.inject(Receiver.map(addSchemaReceiver), DataBaseProvider.database());
        AddSubscriptionResource resource = new AddSubscriptionResource(inlet);

        app.start(7200);
        app.post(resource.receiveEndpoint(), ctx -> {
            Handler handler = context -> {
                SpsEvents events = context.bodyAsClass(SpsEvents.class);
                resource.handle(events);
                System.out.println(events);
                context.status(HttpStatus.NO_CONTENT);
            };
            handler.handle(ctx);
        });
    }

}

package com.kildeen.sps.schemagen;

import com.kildeen.sps.DataBaseProvider;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.inlet.Inlet;
import com.kildeen.sps.inlet.InletDI;
import com.kildeen.sps.inlet.Receiver;
import com.kildeen.sps.SpsEvents;
import com.kildeen.sps.publish.Publish;
import com.kildeen.sps.publish.PublishDI;
import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;

import java.util.Map;
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
        AddSchemaReceiver addSchemaReceiver = new AddSchemaReceiver(new AddSchema(new AddSchemasImpl()));
        Publish publish = PublishDI.INSTANCE.inject(null);

        PublishSchemaReceiver publishSchemaReceiver
                = new PublishSchemaReceiver(new PublishSchema(new FetchSchema(new FetchSchemasImpl()),
                new PublishSchemasImpl(publish)));
        Inlet inlet = InletDI.INSTANCE.inject(Receiver.map(addSchemaReceiver, publishSchemaReceiver),
                DataBaseProvider.database());

        SchemagenResource schemaGenResource = new SchemagenResource(inlet);

        app.start(7200);
        app.post(schemaGenResource.receiveEndpoint(), ctx -> {
            Handler handler = context -> {
                SpsEvents events = context.bodyAsClass(SpsEvents.class);
                schemaGenResource.handle(events);
                System.out.println(events);
                context.status(HttpStatus.NO_CONTENT);
            };
            handler.handle(ctx);
        });
    }

}

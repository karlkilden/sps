package com.kildeen.sps.schemagen;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.BasicSpsEvents;
import com.kildeen.sps.IdWithReceiptsResponse;
import com.kildeen.sps.SpsEvents;
import com.kildeen.sps.StandardLibs;
import com.kildeen.sps.inlet.Inlet;
import com.kildeen.sps.inlet.InletDI;
import com.kildeen.sps.persistence.DataBaseProvider;
import com.kildeen.sps.publish.Publish;
import com.kildeen.sps.publish.PublishDI;
import io.javalin.Javalin;
import io.javalin.http.Handler;

import java.util.List;
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
        StandardLibs.configure();
        Javalin app = Javalin.create();
        AddSchemaReceiver addSchemaReceiver =
                new AddSchemaReceiver(new AddSchema(new AddSchemasImpl()));
        Publish publish = PublishDI.newBuilder()
                .withDatabase(DataBaseProvider.database())
                .build();

        PublishSchemaReceiver publishSchemaReceiver
                = new PublishSchemaReceiver(new PublishSchema(new FetchSchema(new FetchSchemasImpl()),
                new PublishSchemasImpl(publish)));
        Inlet inlet = InletDI.newBuilder()
                .withDatabase(DataBaseProvider.database())
                .withReceivers(List.of(addSchemaReceiver, publishSchemaReceiver))
                .build();

        SchemagenResource schemaGenResource = new SchemagenResource(inlet);

        app.start(7201);
        app.post(schemaGenResource.receiveEndpoint(), ctx -> {
            Handler handler = context -> {
                SpsEvents events = context.bodyAsClass(BasicSpsEvents.class).get();
                IdWithReceiptsResponse result = schemaGenResource.handle(events);
                context.status(result.code());
                if (result.hasBody()) {
                    context.result(result.json());
                }
            };
            handler.handle(ctx);
        });
    }
}

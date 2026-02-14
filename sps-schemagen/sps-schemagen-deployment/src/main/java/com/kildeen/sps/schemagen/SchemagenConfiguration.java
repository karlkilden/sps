package com.kildeen.sps.schemagen;

import static java.lang.invoke.MethodHandles.lookup;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.inlet.Inlet;
import com.kildeen.sps.inlet.InletService;
import com.kildeen.sps.persistence.DatabaseProvider;
import com.kildeen.sps.persistence.Database;
import com.kildeen.sps.publish.Publish;
import com.kildeen.sps.publish.PublishService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * CDI configuration for the Schemagen service.
 * Provides beans for dependency injection.
 */
@ApplicationScoped
public class SchemagenConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    @ConfigProperty(name = "sps.schemagen.service-id", defaultValue = "sps-schemagen")
    String serviceId;

    @Produces
    @Singleton
    public Database database() {
        LOG.info("Initializing embedded database");
        Database db = EmbeddedDatabase.get();
        DatabaseProvider.configure(db);
        return db;
    }

    @Produces
    @Singleton
    public Publish publish(Database database) {
        LOG.info("Initializing Publish service");
        return PublishService.newBuilder()
                .withDatabase(database)
                .build();
    }

    @Produces
    @Singleton
    public Inlet inlet(Database database, Publish publish) {
        LOG.info("Initializing Inlet for service {}", serviceId);

        AddSchemaReceiver addSchemaReceiver =
                new AddSchemaReceiver(new AddSchema(new AddSchemasImpl()));

        PublishSchemaReceiver publishSchemaReceiver =
                new PublishSchemaReceiver(new PublishSchema(
                        new FetchSchema(new FetchSchemasImpl()),
                        new PublishSchemasImpl(publish)));

        return InletService
                .newBuilder()
                .withSubId(serviceId)
                .withDatabase(database)
                .withReceivers(List.of(addSchemaReceiver, publishSchemaReceiver))
                .build();
    }
}

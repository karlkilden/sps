package com.kildeen.sps.subscribe;

import com.kildeen.sps.inlet.Inlet;
import com.kildeen.sps.inlet.InletService;
import com.kildeen.sps.persistence.DatabaseProvider;
import com.kildeen.sps.persistence.Database;
import com.kildeen.embeddeddb.EmbeddedDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * CDI configuration for the Subscribe service.
 * Provides beans for dependency injection.
 */
@ApplicationScoped
public class SubscribeConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    @ConfigProperty(name = "sps.subscribe.service-id", defaultValue = "sps-subscribe")
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
    public Subscribe subscribe() {
        LOG.info("Initializing Subscribe service");
        return SubscriptionService.INSTANCE.inject();
    }

    @Produces
    @Singleton
    public Inlet inlet(Database database, Subscribe subscribe) {
        LOG.info("Initializing Inlet for service {}", serviceId);

        SubscriptionReceiver receiver = new SubscriptionReceiver(subscribe);

        return InletService
                .newBuilder()
                .withSubId(serviceId)
                .withDatabase(database)
                .withReceivers(List.of(receiver))
                .build();
    }
}

package com.kildeen.sps.demo;

import com.kildeen.sps.StandardLibs;
import com.kildeen.sps.inlet.Inlet;
import com.kildeen.sps.inlet.InletService;
import com.kildeen.sps.inlet.Receiver;
import com.kildeen.sps.persistence.Database;
import com.kildeen.sps.persistence.DatabaseProvider;
import com.kildeen.sps.publish.Publish;
import com.kildeen.sps.publish.PublishService;
import com.kildeen.sps.publish.Subscriptions;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CDI configuration for SPS services.
 * Wires together publisher and subscriber components.
 */
@ApplicationScoped
public class SpsConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SpsConfiguration.class);

    // Configure SPS standard libs (JSON + embedded database)
    static {
        StandardLibs.configure();
    }

    @ConfigProperty(name = "sps.subscriber.id", defaultValue = "demo-subscriber")
    String subscriberId;

    @ConfigProperty(name = "sps.event.types", defaultValue = "demo_event_01,greeting_01")
    List<String> eventTypes;

    @ConfigProperty(name = "sps.subscriber.url")
    Optional<String> subscriberUrl;

    @ConfigProperty(name = "sps.publisher.callback.url")
    Optional<String> publisherCallbackUrl;

    @Inject
    EventBroadcaster eventBroadcaster;

    @Produces
    @Singleton
    public Database database() {
        LOG.info("Providing SPS database from StandardLibs");
        return DatabaseProvider.database();
    }

    @Produces
    @Singleton
    @Startup
    public Publish publishService(Database database) {
        LOG.info("Initializing SPS PublishService");

        // Register subscriptions if subscriber URL is configured
        subscriberUrl.ifPresent(url -> {
            LOG.info("Registering subscriptions for subscriber {} at {}", subscriberId, url);
            for (String eventType : eventTypes) {
                var subscriber = new Subscriptions.Subscription.Subscriber(subscriberId, url);
                var subscription = new Subscriptions.Subscription(subscriber, eventType, Map.of());
                database.addSubscription(subscription);
                LOG.info("  Registered subscription: {} -> {}", eventType, subscription.url());
            }
        });

        return PublishService.newBuilder()
                .withDatabase(database)
                .build();
    }

    @Produces
    @Singleton
    public Inlet inletService(Database database) {
        LOG.info("Initializing SPS InletService with subscriber: {}", subscriberId);

        String callbackUrl = publisherCallbackUrl.orElse(null);
        if (callbackUrl != null) {
            LOG.info("Publisher callback URL configured: {}", callbackUrl);
        }

        // Create receivers for each configured event type with dashboard callback
        List<Receiver> receivers = eventTypes.stream()
                .map(type -> new DemoReceiver(type, eventBroadcaster::broadcast, callbackUrl))
                .map(r -> (Receiver) r)
                .toList();

        LOG.info("Registered receivers for event types: {}", eventTypes);

        return InletService.newBuilder()
                .withDatabase(database)
                .withSubId(subscriberId)
                .withReceivers(receivers)
                .withTransportPolling(true)  // Enable database transport fallback polling
                .build();
    }
}

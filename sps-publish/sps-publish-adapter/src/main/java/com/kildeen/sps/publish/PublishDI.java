package com.kildeen.sps.publish;

import com.kildeen.sps.Client;
import com.kildeen.sps.Schemas;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEventType;
import com.kildeen.sps.SpsSubscriberType;
import com.kildeen.sps.persistence.DataBaseProvider;
import com.kildeen.sps.persistence.Database;
import com.kildeen.sps.publish.Subscriptions.Subscription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PublishDI implements Publish {

    private final Publisher publisher;
    private final PublishSchema publishSchema;
    private final FetchSubscription fetchSubscription;

    private PublishDI(Builder builder) {
        publisher = builder.publisher;
        publishSchema = builder.publishSchema;
        fetchSubscription = builder.fetchSubscription;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public PublishResult publish(Collection<SpsEvent> events) {
        Set<String> types = SpsEvent.resolveTypes(events);
        Subscriptions subscriptions = fetchSubscription.fetch(types);

        if (subscriptions.isEmpty()) {
            generateSchema(types, events);
            return PublishResult.SCHEMA_GEN_PUBLISH;
        }

        publisher.publish(subscriptions, events);
        return PublishResult.PUBLISH;
    }

    private void generateSchema(Set<String> types, Collection<SpsEvent> events) {
        String url = fetchSubscription.fetchSchemaGenUrl();
        Subscription.Subscriber schemaGen =
                new Subscription.Subscriber(SpsSubscriberType.gen_schema.toString(), url);

        for (String type : types) {
            Subscriptions schemaGenWrapper =
                    new Subscriptions(List.of(new Subscription(schemaGen,
                            SpsEventType.add_schema_01.toString(),
                            Map.of())));

            publishSchema.publish(type, schemaGenWrapper, events);
        }
    }

    public static final class Builder {
        private Database database;
        private List<Client> clients = new ArrayList<>();
        private Schemas schemas;
        private Publisher publisher;
        private PublishSchema publishSchema;
        private FetchSubscription fetchSubscription;
        private RetryPolicies retryPolicies;


        private Builder() {
        }

        public Builder withDatabase(Database database) {
            this.database = database;
            return this;
        }

        public Builder withClient(Client client) {
            this.clients.add(client);
            return this;
        }

        public Builder withRetryPolicies(RetryPolicies retryPolicies) {
            this.retryPolicies = retryPolicies;
            return this;
        }

        /**
         * Each event schema is autogenerated. However, the client can update the docs by adding schemas
         * @param schemas
         * @return Builder instance
         */
        public Builder withClientSuppliedSchema(Schemas schemas) {
            this.schemas = schemas;
            return this;
        }

        public PublishDI build() {
            if (clients.isEmpty()) {
                clients.add(new HttpClient());
            }
            if (database == null) {
                throw new NullPointerException("No database configured");
            }
            if (schemas == null) {
                schemas = new Schemas(List.of());
            }
            if (retryPolicies == null) {
                retryPolicies = new RetryPolicies(List.of(), RetryPolicies.DEFAULT_RETRY_POLICIES);
            }
            this.publisher = new Publisher(new Sender(clients), new RetryQueue(), retryPolicies,
                    new CircuitBreakerState(database));
            fetchSubscription = new FetchSubscription(new FetchSubscriptionsImpl(database));
            FetchSchema fetchSchema = new FetchSchema(new FetchSchemasImpl(DataBaseProvider.database()));
            publishSchema = new PublishSchema(publisher, fetchSubscription, fetchSchema, schemas);
            return new PublishDI(this);

        }
    }
}

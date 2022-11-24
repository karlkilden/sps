package com.kildeen.sps.publish;

import com.kildeen.sps.Client;
import com.kildeen.sps.Schemas;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsSubscriberType;
import com.kildeen.sps.persistence.DataBaseProvider;
import com.kildeen.sps.persistence.Database;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PublishDI implements Publish {

    private final PublishEvent publishEvent;
    private final PublishSchema publishSchema;
    private final FetchSubscription fetchSubscription;

    private PublishDI(Builder builder) {
        publishEvent = builder.publishEvent;
        publishSchema = builder.publishSchema;
        fetchSubscription = builder.fetchSubscription;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public PublishResult publish(String type, Collection<SpsEvent> events) {
        Subscriptions subscriptions = fetchSubscription.fetch(type);

        if (subscriptions.isEmpty()) {
            generateSchema(type, events);
            return PublishResult.SCHEMA_GEN_PUBLISH;
        }

        publishEvent.publish(type, subscriptions, events);
        return PublishResult.PUBLISH;
    }

    private void generateSchema(String type, Collection<SpsEvent> events) {
        String url = fetchSubscription.fetchSchemaGenUrl();
        Subscriptions.Subscription.Subscriber schemaGen =
                new Subscriptions.Subscription.Subscriber(SpsSubscriberType.gen_schema.toString(), url);

        Subscriptions schemaGenWrapper =
                new Subscriptions(List.of(new Subscriptions.Subscription(schemaGen, type, Map.of())));

        publishSchema.publish(type, schemaGenWrapper, events);
    }

    public static final class Builder {
        private Database database;
        private Client client;
        private Schemas schemas;
        private PublishEvent publishEvent;
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
            this.client = client;
            return this;
        }

        public Builder withRetryPolicies(RetryPolicies retryPolicies) {
            this.retryPolicies = retryPolicies;
            return this;
        }

        public Builder withClientSuppliedSchema(Schemas schemas) {
            this.schemas = schemas;
            return this;
        }

        public PublishDI build() {
            if (client == null) {
                client = new HttpClient();
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
            Publisher publisher = new Publisher(new Sender(client), new RetryQueue(), retryPolicies);
            fetchSubscription = new FetchSubscription(new FetchSubscriptionsImpl(database));
            FetchSchema fetchSchema = new FetchSchema(new FetchSchemasImpl(DataBaseProvider.database()));
            publishEvent = new PublishEvent(publisher);
            publishSchema = new PublishSchema(publishEvent, fetchSubscription, fetchSchema, schemas);
            return new PublishDI(this);

        }
    }
}

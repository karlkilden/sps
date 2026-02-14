package com.kildeen.sps.publish;

import com.kildeen.sps.Schemas;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEventType;
import com.kildeen.sps.json.JsonProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles schema publishing and generation based on event data.
 * Automatically generates schema documentation from event fields.
 */
public class PublishSchema {

    private static final Logger LOG = LoggerFactory.getLogger(PublishSchema.class);

    private final Publisher publisher;
    private final FetchSchema fetchSchema;
    private final Schemas clientSuppliedSchemas;

    public PublishSchema(Publisher publisher,
                         FetchSchema fetchSchema,
                         Schemas clientSuppliedSchemas) {
        this.publisher = publisher;
        this.fetchSchema = fetchSchema;
        this.clientSuppliedSchemas = clientSuppliedSchemas;
    }

    public void publish(String type, Subscriptions subscriptions, Collection<SpsEvent> events) {
        LOG.debug("Publishing schema for event type: {}", type);
        Schemas.Schema stored = fetchSchema.fetch(type);
        Schemas.Schema suppliedSchema =
                clientSuppliedSchemas.schemas().stream().filter(schema -> schema.eventType().equals(type)).findAny()
                        .orElse(null);
        if (suppliedSchema != null) {
            if (stored != null && stored.version() > suppliedSchema.version()) {
                LOG.debug("Skipping schema publish for {}: stored version {} > supplied version {}",
                        type, stored.version(), suppliedSchema.version());
                return;
            }
            SpsEvent mostKeys = getSpsEventWithMostKeys(events);

            Map<String, String> fieldDocumentation = createFieldDocumentation(mostKeys,
                    suppliedSchema.fieldDocumentation());
            Schemas.Schema schema =
                    new Schemas.Schema(type,
                            suppliedSchema.eventDocumentation(),
                            fieldDocumentation,
                            suppliedSchema.tags(),
                            suppliedSchema.version());
            LOG.info("Publishing client-supplied schema for type {} (version {})", type, schema.version());
            doPublish(schema, subscriptions);
        } else {
            SpsEvent mostKeys = getSpsEventWithMostKeys(events);
            if (stored != null && stored.fieldDocumentation().size() == mostKeys.data().size()) {
                LOG.debug("Skipping auto-generated schema for {}: field count unchanged ({})",
                        type, mostKeys.data().size());
                return;
            }
            Map<String, String> fieldDocumentation = createFieldDocumentation(mostKeys,
                    stored == null ? Map.of() : stored.fieldDocumentation());
            int newVersion = stored == null ? 1 : stored.version() + 1;
            Schemas.Schema schema =
                    new Schemas.Schema(type,
                            "N/A",
                            fieldDocumentation,
                            Set.of("default"),
                            newVersion);
            LOG.info("Publishing auto-generated schema for type {} (version {}, {} fields)",
                    type, newVersion, fieldDocumentation.size());
            doPublish(schema, subscriptions);
        }
    }

    @NotNull
    private Map<String, String> createFieldDocumentation(SpsEvent mostKeys, Map<String, String> existing) {
        Map<String, String> fieldDocumentation = new HashMap<>(existing);

        mostKeys.data().keySet().stream()
                .filter(k -> !fieldDocumentation.containsKey(k))
                .forEach(k -> fieldDocumentation.put(k, "N/A"));
        return fieldDocumentation;
    }

    private SpsEvent getSpsEventWithMostKeys(Collection<SpsEvent> events) {
        List<SpsEvent> spsEvents = events.stream()
                .sorted(Comparator.comparing(e -> e.data().keySet().size())).toList();
        return spsEvents.get(spsEvents.size() - 1);
    }

    private void doPublish(Schemas.Schema schema, Subscriptions subscriptions) {
        SpsEvent event = new SpsEvent() {
            private String id = genId();

            @Override
            public String type() {
                return SpsEventType.add_schema_01.toString();
            }

            @Override
            public String id() {
                return id;
            }

            @Override
            public Map<String, Object> data() {
                return Map.of("json", JsonProvider.json().write(schema));
            }
        };
        publisher.publish(subscriptions, List.of(event));
    }
}
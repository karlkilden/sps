package com.kildeen.sps.schemagen;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.BasicSpsEvents;
import com.kildeen.sps.JvmLocalPostImpl;
import com.kildeen.sps.Schemas;
import com.kildeen.sps.TestInit;
import com.kildeen.sps.inlet.Inlet;
import com.kildeen.sps.inlet.InletDI;
import com.kildeen.sps.persistence.DataBaseProvider;
import com.kildeen.sps.publish.Publish;
import com.kildeen.sps.publish.PublishDI;
import com.kildeen.sps.publish.SameJVMClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class SchemaGenTest {
    private static Inlet inlet;
    private static Publish publish;

    static {
        TestInit.init();
    }

    @BeforeAll
    static void setUp() {
        inlet = InletDI.newBuilder()
                .withDatabase(DataBaseProvider.configure(EmbeddedDatabase.get()))
                .withReceivers(List.of(new AddSchemaReceiver(new AddSchema(new AddSchemasImpl()))))
                .build();

        publish = baseBuilder()
                .build();
    }

    private static PublishDI.Builder baseBuilder() {
        return PublishDI.newBuilder().withDatabase(DataBaseProvider.configure(EmbeddedDatabase.get()))
                .withClient(new SameJVMClient(new JvmLocalPostImpl(List.of(inlet))));
    }

    @Test
    void generates_schema() {
        publish.publish("not_existing",
                List.of(new BasicSpsEvents.BasicSpsEvent(
                        "not_existing", "1", Map.of("key", "value"))));


        Schemas.Schema schema = EmbeddedDatabase.get().schema("not_existing");
        assertThat(schema).isNotNull();
        assertThat(schema.eventType()).isEqualTo("not_existing");
        assertThat(schema.fieldDocumentation().keySet()).containsExactly("key");
    }

    @Test
    void updates_schema_when_event_has_more_keys() {
        publish.publish("not_existing_2",
                List.of(new BasicSpsEvents.BasicSpsEvent(
                        "not_existing_2", "1", Map.of("key", "value"))));

        publish.publish("not_existing_2",
                List.of(new BasicSpsEvents.BasicSpsEvent(
                        "not_existing_2", "1", Map.of("key", "value", "key2", "value2"))));

        Schemas.Schema schema = EmbeddedDatabase.get().schema("not_existing_2");
        assertThat(schema.fieldDocumentation().keySet()).containsExactlyInAnyOrder("key", "key2");
    }

    @Test
    void uses_field_documentation() {
        var publish = baseBuilder()
                .withClientSuppliedSchema(new Schemas(List.of(new Schemas.Schema("not_existing_3", null,
                        Map.of("some_field", "some_field_doc"), Set.of(), 1))))
                .build();

        publish.publish("not_existing_3",
                List.of(new BasicSpsEvents.BasicSpsEvent(
                        "not_existing_3", "1", Map.of("key", "value", "key2", "value2"))));

        Schemas.Schema schema = EmbeddedDatabase.get().schemas().schemas().get(0);
        assertThat(schema.fieldDocumentation().keySet()).containsExactlyInAnyOrder("key", "key2", "some_field");
        assertThat(schema.fieldDocumentation().get("some_field")).isEqualTo("some_field_doc");

    }

    @Test
    void uses() {
        var publish = baseBuilder()
                .withClientSuppliedSchema(new Schemas(List.of(new Schemas.Schema("not_existing_3", null,
                        Map.of("some_field", "some_field_doc"), Set.of(), 1))))
                .build();

        publish.publish("not_existing_3",
                List.of(new BasicSpsEvents.BasicSpsEvent(
                        "not_existing_3", "1", Map.of("key", "value", "key2", "value2"))));

        Schemas.Schema schema = EmbeddedDatabase.get().schemas().schemas().get(0);
        assertThat(schema.fieldDocumentation().keySet()).containsExactlyInAnyOrder("key", "key2", "some_field");

    }
}

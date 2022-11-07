package com.kildeen.sps;

import com.kildeen.sps.publish.Publish;
import com.kildeen.sps.publish.PublishDI;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class SchemaGenTest {

    @Test
    void name() {
    Publish publish = PublishDI.INSTANCE.inject(null);
    publish.publish("schemagen_test01", List.of(new SpsEvent() {
        @Override
        public String type() {
            return "schemagen_test01";
        }

        @Override
        public Map<String, Object> data() {
            return Map.of("other_value", 10);
        }
    }));
    }
}

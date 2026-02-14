package com.kildeen.sps.e2e;

import com.kildeen.embeddeddb.InMemoryDeadLetterQueue;
import com.kildeen.sps.BasicSpsEvents;
import com.kildeen.sps.dlq.DeadLetterEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Dead Letter Queue functionality.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Dead Letter Queue Tests")
public class DeadLetterQueueE2ETest {

    private static final String DLQ_EVENT_TYPE = "dlq_test_event";

    private InMemoryDeadLetterQueue dlq;

    @BeforeEach
    void setUp() {
        dlq = new InMemoryDeadLetterQueue();
    }

    @Test
    @DisplayName("DLQ interface works correctly for manual send")
    void dlqManualSendWorks() {
        // Test DLQ directly without retry mechanics
        BasicSpsEvents.BasicSpsEvent event = new BasicSpsEvents.BasicSpsEvent(
                DLQ_EVENT_TYPE, "dlq-direct-" + System.currentTimeMillis(), Map.of("test", "data"));

        dlq.send(event, "test_reason", 5);

        assertThat(dlq.count()).isEqualTo(1);
        List<DeadLetterEntry> entries = dlq.peek(10);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).reason()).isEqualTo("test_reason");
        assertThat(entries.get(0).retryCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should be able to purge events from DLQ")
    void canPurgeFromDlq() {
        BasicSpsEvents.BasicSpsEvent event = new BasicSpsEvents.BasicSpsEvent(
                DLQ_EVENT_TYPE, "dlq-purge-" + System.currentTimeMillis(), Map.of("test", "purge"));

        dlq.send(event, "purge_test", 3);

        assertThat(dlq.count()).isEqualTo(1);

        boolean purged = dlq.purge(event.id());
        assertThat(purged).isTrue();
        assertThat(dlq.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("DLQ prevents duplicate entries")
    void dlqPreventsDuplicates() {
        BasicSpsEvents.BasicSpsEvent event = new BasicSpsEvents.BasicSpsEvent(
                DLQ_EVENT_TYPE, "dlq-dup-" + System.currentTimeMillis(), Map.of("test", "dup"));

        dlq.send(event, "first", 1);
        dlq.send(event, "second", 2); // Same ID, should be ignored

        assertThat(dlq.count()).isEqualTo(1);
        List<DeadLetterEntry> entries = dlq.peek(10);
        assertThat(entries.get(0).reason()).isEqualTo("first"); // First entry kept
    }
}

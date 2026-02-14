package com.kildeen.sps.persistence.postgres;

import com.kildeen.sps.Receipt;
import com.kildeen.sps.Schemas;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.persistence.Config;
import com.kildeen.sps.persistence.Database;
import com.kildeen.sps.persistence.TransportQueueEntry;
import com.kildeen.sps.publish.Subscriptions;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * PostgreSQL implementation of the SPS Database interface.
 */
public class PostgresDatabase implements Database {
    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final Jdbi jdbi;

    public PostgresDatabase(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public void addSubscription(Subscriptions.Subscription subscription) {
        jdbi.useHandle(handle -> {
            handle.createUpdate("""
                    INSERT INTO sps_subscriptions (event_type, url, subscriber_id, sub_schema)
                    VALUES (:eventType, :url, :subscriberId, CAST(:subSchema AS jsonb))
                    ON CONFLICT (event_type, subscriber_id) DO UPDATE SET
                        url = EXCLUDED.url,
                        sub_schema = EXCLUDED.sub_schema,
                        updated_at = NOW()
                    """)
                    .bind("eventType", subscription.eventType())
                    .bind("url", subscription.subscriber().resolveUrl())
                    .bind("subscriberId", subscription.subscriber().subId())
                    .bind("subSchema", toJson(subscription.subSchema()))
                    .execute();
        });
    }

    @Override
    public Schemas schemas() {
        return jdbi.withHandle(handle -> {
            List<Schemas.Schema> schemaList = handle.createQuery("""
                            SELECT event_type, event_documentation, field_documentation, tags, version
                            FROM sps_schemas
                            """)
                    .map((rs, ctx) -> new Schemas.Schema(
                            rs.getString("event_type"),
                            rs.getString("event_documentation"),
                            parseStringMap(rs.getString("field_documentation")),
                            parseStringSet(rs.getArray("tags")),
                            rs.getInt("version")))
                    .list();
            return new Schemas(schemaList);
        });
    }

    @Override
    public Schemas.Schema schema(String eventType) {
        return jdbi.withHandle(handle ->
                handle.createQuery("""
                                SELECT event_type, event_documentation, field_documentation, tags, version
                                FROM sps_schemas WHERE event_type = :eventType
                                """)
                        .bind("eventType", eventType)
                        .map((rs, ctx) -> new Schemas.Schema(
                                rs.getString("event_type"),
                                rs.getString("event_documentation"),
                                parseStringMap(rs.getString("field_documentation")),
                                parseStringSet(rs.getArray("tags")),
                                rs.getInt("version")))
                        .findOne()
                        .orElse(null));
    }

    @Override
    public void addSchema(Schemas.Schema schema) {
        String tagsArray = "{" + String.join(",", schema.tags().stream().map(this::escapeJson).toList()) + "}";

        jdbi.useHandle(handle ->
                handle.createUpdate("""
                                INSERT INTO sps_schemas (event_type, event_documentation, field_documentation, tags, version)
                                VALUES (:eventType, :eventDoc, CAST(:fieldDoc AS jsonb), CAST(:tags AS text[]), :version)
                                ON CONFLICT (event_type) DO UPDATE SET
                                    event_documentation = EXCLUDED.event_documentation,
                                    field_documentation = EXCLUDED.field_documentation,
                                    tags = EXCLUDED.tags,
                                    version = EXCLUDED.version,
                                    updated_at = NOW()
                                """)
                        .bind("eventType", schema.eventType())
                        .bind("eventDoc", schema.eventDocumentation())
                        .bind("fieldDoc", toJson(schema.fieldDocumentation()))
                        .bind("tags", tagsArray)
                        .bind("version", schema.version())
                        .execute());
    }

    @Override
    public void ackOrNack(SpsEvent event, Receipt receipt) {
        jdbi.useHandle(handle ->
                handle.createUpdate("""
                                INSERT INTO sps_receipts (event_id, event_type, receipt_type)
                                VALUES (:eventId, :eventType, :receiptType)
                                """)
                        .bind("eventId", event.id())
                        .bind("eventType", event.type())
                        .bind("receiptType", receipt.name())
                        .execute());
    }

    @Override
    public Subscriptions subscriptions(Set<String> eventTypes) {
        if (eventTypes.isEmpty()) {
            return new Subscriptions(List.of());
        }

        return jdbi.withHandle(handle -> {
            List<Subscriptions.Subscription> subscriptions = handle.createQuery("""
                            SELECT event_type, url, subscriber_id, sub_schema
                            FROM sps_subscriptions
                            WHERE event_type = ANY(:eventTypes)
                            """)
                    .bind("eventTypes", eventTypes.toArray(new String[0]))
                    .map((rs, ctx) -> {
                        String eventType = rs.getString("event_type");
                        String url = rs.getString("url");
                        String subscriberId = rs.getString("subscriber_id");
                        Map<String, String> subSchema = parseStringMap(rs.getString("sub_schema"));

                        Subscriptions.Subscription.Subscriber subscriber =
                                new Subscriptions.Subscription.Subscriber(subscriberId, url);

                        return new Subscriptions.Subscription(subscriber, eventType, subSchema);
                    })
                    .list();

            return new Subscriptions(subscriptions);
        });
    }

    @Override
    public Config fetchConfig() {
        return jdbi.withHandle(handle -> {
            String schemagenUrl = handle.createQuery("""
                            SELECT value FROM sps_config WHERE key = 'schemagen.url'
                            """)
                    .mapTo(String.class)
                    .findOne()
                    .orElse("http://localhost:7201");

            return new Config(new Config.SchemaGen(schemagenUrl));
        });
    }

    @Override
    public boolean isAck(String id) {
        return hasReceipt(id, "ACK");
    }

    @Override
    public boolean isNack(String id) {
        return hasReceipt(id, "NACK");
    }

    @Override
    public boolean isAbandoned(String id) {
        return hasReceipt(id, "ABANDONED");
    }

    private boolean hasReceipt(String id, String receiptType) {
        return jdbi.withHandle(handle ->
                handle.createQuery("""
                                SELECT EXISTS(SELECT 1 FROM sps_receipts WHERE event_id = :eventId AND receipt_type = :receiptType)
                                """)
                        .bind("eventId", id)
                        .bind("receiptType", receiptType)
                        .mapTo(Boolean.class)
                        .one());
    }

    @Override
    public int nackCount(String id) {
        return jdbi.withHandle(handle ->
                handle.createQuery("""
                                SELECT COUNT(*) FROM sps_receipts WHERE event_id = :eventId AND receipt_type = 'NACK'
                                """)
                        .bind("eventId", id)
                        .mapTo(Integer.class)
                        .one());
    }

    @Override
    public long firstNackInterval(String id) {
        return jdbi.withHandle(handle -> {
            List<Instant> nackTimes = handle.createQuery("""
                            SELECT created_at FROM sps_receipts
                            WHERE event_id = :eventId AND receipt_type = 'NACK'
                            ORDER BY created_at ASC LIMIT 2
                            """)
                    .bind("eventId", id)
                    .mapTo(Instant.class)
                    .list();

            if (nackTimes.isEmpty()) {
                return -1L;
            }
            if (nackTimes.size() == 1) {
                return 0L;
            }
            return nackTimes.get(1).toEpochMilli() - nackTimes.get(0).toEpochMilli();
        });
    }

    @Override
    public long firstNackToAck(String id) {
        return jdbi.withHandle(handle -> {
            Instant firstNack = handle.createQuery("""
                            SELECT created_at FROM sps_receipts
                            WHERE event_id = :eventId AND receipt_type = 'NACK'
                            ORDER BY created_at ASC LIMIT 1
                            """)
                    .bind("eventId", id)
                    .mapTo(Instant.class)
                    .findOne()
                    .orElse(null);

            if (firstNack == null) {
                return -1L;
            }

            Instant ack = handle.createQuery("""
                            SELECT created_at FROM sps_receipts
                            WHERE event_id = :eventId AND receipt_type = 'ACK'
                            ORDER BY created_at ASC LIMIT 1
                            """)
                    .bind("eventId", id)
                    .mapTo(Instant.class)
                    .findOne()
                    .orElse(null);

            if (ack == null) {
                return -1L;
            }

            return ack.toEpochMilli() - firstNack.toEpochMilli();
        });
    }

    @Override
    public long nackCountByTypeSince(String eventType, Instant since) {
        return jdbi.withHandle(handle ->
                handle.createQuery("""
                                SELECT COUNT(*) FROM sps_receipts
                                WHERE event_type = :eventType AND receipt_type = 'NACK' AND created_at > :since
                                """)
                        .bind("eventType", eventType)
                        .bind("since", since)
                        .mapTo(Long.class)
                        .one());
    }

    @Override
    public void tripCircuit(String subId, String eventType) {
        jdbi.useHandle(handle ->
                handle.createUpdate("""
                                INSERT INTO sps_circuit_breakers (sub_id, event_type)
                                VALUES (:subId, :eventType)
                                ON CONFLICT (sub_id, event_type) DO NOTHING
                                """)
                        .bind("subId", subId)
                        .bind("eventType", eventType)
                        .execute());
    }

    @Override
    public void resetCircuit(String subId, String eventType) {
        jdbi.useHandle(handle ->
                handle.createUpdate("""
                                DELETE FROM sps_circuit_breakers WHERE sub_id = :subId AND event_type = :eventType
                                """)
                        .bind("subId", subId)
                        .bind("eventType", eventType)
                        .execute());
    }

    @Override
    public Map<String, Set<String>> trippedCircuits() {
        return jdbi.withHandle(handle -> {
            Map<String, Set<String>> result = new HashMap<>();
            handle.createQuery("""
                            SELECT sub_id, event_type FROM sps_circuit_breakers
                            """)
                    .map((rs, ctx) -> {
                        String subId = rs.getString("sub_id");
                        String eventType = rs.getString("event_type");
                        result.computeIfAbsent(subId, k -> new HashSet<>()).add(eventType);
                        return null;
                    })
                    .list();
            return result;
        });
    }

    @Override
    public boolean isTripped(String subId, String eventType) {
        return jdbi.withHandle(handle ->
                handle.createQuery("""
                                SELECT EXISTS(SELECT 1 FROM sps_circuit_breakers WHERE sub_id = :subId AND event_type = :eventType)
                                """)
                        .bind("subId", subId)
                        .bind("eventType", eventType)
                        .mapTo(Boolean.class)
                        .one());
    }

    @Override
    public boolean takeLeader(UUID id) {
        return jdbi.withHandle(handle -> {
            // Try to clean up expired leaders first
            handle.createUpdate("""
                            DELETE FROM sps_leader WHERE expires_at < NOW()
                            """)
                    .execute();

            // Try to insert as new leader
            int inserted = handle.createUpdate("""
                            INSERT INTO sps_leader (leader_id, acquired_at, expires_at)
                            SELECT :leaderId, NOW(), NOW() + INTERVAL '30 seconds'
                            WHERE NOT EXISTS (SELECT 1 FROM sps_leader WHERE expires_at > NOW())
                            """)
                    .bind("leaderId", id)
                    .execute();

            if (inserted > 0) {
                return true;
            }

            // Check if we are already the leader and extend
            int updated = handle.createUpdate("""
                            UPDATE sps_leader SET expires_at = NOW() + INTERVAL '30 seconds'
                            WHERE leader_id = :leaderId
                            """)
                    .bind("leaderId", id)
                    .execute();

            return updated > 0;
        });
    }

    // Transport queue methods

    @Override
    public void insertTransportEvent(String eventId, String eventType, String subscriberId, String payload) {
        jdbi.useHandle(handle ->
                handle.createUpdate("""
                        INSERT INTO sps_transport_queue (event_id, event_type, subscriber_id, payload, status)
                        VALUES (:eventId, :eventType, :subscriberId, CAST(:payload AS jsonb), 'PENDING')
                        ON CONFLICT (event_id, subscriber_id) DO NOTHING
                        """)
                        .bind("eventId", eventId)
                        .bind("eventType", eventType)
                        .bind("subscriberId", subscriberId)
                        .bind("payload", payload)
                        .execute());
    }

    @Override
    public List<TransportQueueEntry> pollTransportQueue(String subscriberId, int limit) {
        return jdbi.withHandle(handle ->
                handle.createQuery("""
                        SELECT id, event_id, event_type, subscriber_id, payload::text, status, created_at
                        FROM sps_transport_queue
                        WHERE subscriber_id = :subscriberId AND status = 'PENDING'
                        ORDER BY created_at ASC
                        LIMIT :limit
                        """)
                        .bind("subscriberId", subscriberId)
                        .bind("limit", limit)
                        .map((rs, ctx) -> new TransportQueueEntry(
                                rs.getLong("id"),
                                rs.getString("event_id"),
                                rs.getString("event_type"),
                                rs.getString("subscriber_id"),
                                rs.getString("payload"),
                                rs.getString("status"),
                                rs.getTimestamp("created_at").toInstant()))
                        .list());
    }

    @Override
    public void markTransportProcessed(String eventId, String subscriberId) {
        jdbi.useHandle(handle ->
                handle.createUpdate("""
                        UPDATE sps_transport_queue
                        SET status = 'PROCESSED', processed_at = NOW()
                        WHERE event_id = :eventId AND subscriber_id = :subscriberId
                        """)
                        .bind("eventId", eventId)
                        .bind("subscriberId", subscriberId)
                        .execute());
    }

    @Override
    public int cleanupTransportQueue(Instant olderThan) {
        return jdbi.withHandle(handle ->
                handle.createUpdate("""
                        DELETE FROM sps_transport_queue
                        WHERE status = 'PROCESSED' AND processed_at < :olderThan
                        """)
                        .bind("olderThan", olderThan)
                        .execute());
    }

    // JSON helper methods
    private String toJson(Map<String, ?> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":\"")
                    .append(escapeJson(String.valueOf(entry.getValue()))).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Map<String, String> parseStringMap(String json) {
        Map<String, String> result = new HashMap<>();
        if (json == null || json.isBlank() || json.equals("{}")) {
            return result;
        }
        String content = json.trim();
        if (content.startsWith("{") && content.endsWith("}")) {
            content = content.substring(1, content.length() - 1).trim();
            if (!content.isEmpty()) {
                String[] pairs = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                for (String pair : pairs) {
                    String[] kv = pair.split(":", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim().replaceAll("^\"|\"$", "");
                        String value = kv[1].trim().replaceAll("^\"|\"$", "");
                        result.put(key, value);
                    }
                }
            }
        }
        return result;
    }

    private Set<String> parseStringSet(java.sql.Array array) {
        if (array == null) {
            return Set.of();
        }
        try {
            String[] values = (String[]) array.getArray();
            return new HashSet<>(Arrays.asList(values));
        } catch (Exception e) {
            LOG.warn("Failed to parse string array", e);
            return Set.of();
        }
    }
}

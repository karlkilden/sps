-- Transport Queue for database-based event delivery fallback
-- When HTTP delivery fails, events are stored here for polling by subscribers

CREATE TABLE IF NOT EXISTS sps_transport_queue (
    id SERIAL PRIMARY KEY,
    event_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    subscriber_id VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(event_id, subscriber_id)
);

CREATE INDEX idx_transport_queue_subscriber ON sps_transport_queue(subscriber_id, status);
CREATE INDEX idx_transport_queue_created ON sps_transport_queue(created_at);
CREATE INDEX idx_transport_queue_status ON sps_transport_queue(status);

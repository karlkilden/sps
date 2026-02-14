-- SPS Initial Schema

-- Subscriptions table
CREATE TABLE IF NOT EXISTS sps_subscriptions (
    id SERIAL PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    subscriber_id VARCHAR(255) NOT NULL,
    sub_schema JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(event_type, subscriber_id)
);

CREATE INDEX idx_subscriptions_event_type ON sps_subscriptions(event_type);
CREATE INDEX idx_subscriptions_subscriber ON sps_subscriptions(subscriber_id);

-- Schemas table
CREATE TABLE IF NOT EXISTS sps_schemas (
    id SERIAL PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL UNIQUE,
    event_documentation TEXT DEFAULT '',
    field_documentation JSONB DEFAULT '{}',
    tags TEXT[] DEFAULT ARRAY[]::TEXT[],
    version INT DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_schemas_event_type ON sps_schemas(event_type);

-- Event receipts (ack/nack tracking)
CREATE TABLE IF NOT EXISTS sps_receipts (
    id SERIAL PRIMARY KEY,
    event_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    receipt_type VARCHAR(20) NOT NULL CHECK (receipt_type IN ('ACK', 'NACK', 'ABANDONED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_receipts_event_id ON sps_receipts(event_id);
CREATE INDEX idx_receipts_event_type ON sps_receipts(event_type);
CREATE INDEX idx_receipts_type_created ON sps_receipts(event_type, created_at);

-- Circuit breakers
CREATE TABLE IF NOT EXISTS sps_circuit_breakers (
    id SERIAL PRIMARY KEY,
    sub_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    tripped_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(sub_id, event_type)
);

CREATE INDEX idx_circuit_breakers_sub_id ON sps_circuit_breakers(sub_id);

-- Leader election
CREATE TABLE IF NOT EXISTS sps_leader (
    id SERIAL PRIMARY KEY,
    leader_id UUID NOT NULL,
    acquired_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() + INTERVAL '30 seconds'
);

-- Config table
CREATE TABLE IF NOT EXISTS sps_config (
    key VARCHAR(255) PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Insert default config
INSERT INTO sps_config (key, value) VALUES ('schemagen.url', 'http://localhost:7201')
ON CONFLICT (key) DO NOTHING;

CREATE TABLE processed_events (
    event_id VARCHAR(128) PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


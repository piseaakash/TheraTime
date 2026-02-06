-- 1. Concurrency: prevent double-booking per therapist slot (exact start_time).
--    One row per (therapist_id, start_time) per tenant schema.
CREATE UNIQUE INDEX idx_tenant_1_appointments_therapist_start
    ON tenant_1.appointments (therapist_id, start_time);
CREATE UNIQUE INDEX idx_tenant_2_appointments_therapist_start
    ON tenant_2.appointments (therapist_id, start_time);

-- 2. Transactional outbox: one table per tenant schema for reliable event publishing.
CREATE TABLE tenant_1.outbox (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at TIMESTAMP,
    attempt_count INT NOT NULL DEFAULT 0
);

CREATE TABLE tenant_2.outbox (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at TIMESTAMP,
    attempt_count INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_tenant_1_outbox_status_created ON tenant_1.outbox (status, created_at)
    WHERE status = 'PENDING';
CREATE INDEX idx_tenant_2_outbox_status_created ON tenant_2.outbox (status, created_at)
    WHERE status = 'PENDING';

-- Per-tenant notification config; optional per-therapist override.
-- Lookup: (tenant_id, therapist_id) first, then fallback to (tenant_id, null) for tenant default.
CREATE TABLE notification_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    therapist_id BIGINT,
    email_enabled BOOLEAN DEFAULT false,
    email_from VARCHAR(255),
    smtp_host VARCHAR(255),
    smtp_port INT,
    smtp_username VARCHAR(255),
    smtp_password_encrypted VARCHAR(512),
    whatsapp_enabled BOOLEAN DEFAULT false,
    whatsapp_phone_or_api_key VARCHAR(255),
    default_to_email VARCHAR(255),
    default_to_phone VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_notification_config_tenant_therapist ON notification_config (tenant_id, therapist_id)
    WHERE therapist_id IS NOT NULL;
CREATE UNIQUE INDEX idx_notification_config_tenant_default ON notification_config (tenant_id)
    WHERE therapist_id IS NULL;

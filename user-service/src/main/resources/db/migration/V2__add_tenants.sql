-- Tenant model: one tenant = one practice (one therapist + admins + clients).
-- Row-level multi-tenancy: tenant_id on users and downstream data.
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Default tenant for existing users (single practice at first).
INSERT INTO tenants (id, name) VALUES (1, 'Default');
SELECT setval('tenants_id_seq', 1);

ALTER TABLE users ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
UPDATE users SET tenant_id = 1;
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;

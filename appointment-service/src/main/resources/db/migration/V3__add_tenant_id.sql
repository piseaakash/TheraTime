-- Row-level multi-tenancy: scope appointments and calendar blocks by tenant (practice).
-- tenant_id is a logical reference to user-service tenants; no FK in this DB.
ALTER TABLE appointments ADD COLUMN tenant_id BIGINT;
UPDATE appointments SET tenant_id = 1;
ALTER TABLE appointments ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE calendar_blocks ADD COLUMN tenant_id BIGINT;
UPDATE calendar_blocks SET tenant_id = 1;
ALTER TABLE calendar_blocks ALTER COLUMN tenant_id SET NOT NULL;

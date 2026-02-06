# Tenant model: schema-per-tenant (multi-tenancy)

This document describes the **schema-per-tenant** multi-tenancy used in TheraTime: one PostgreSQL schema per practice (tenant) for strong isolation and a clear portfolio differentiator.

## What is implemented

- **Schema-per-tenant**: Each tenant has its own PostgreSQL schema (`tenant_1`, `tenant_2`, …). Tables `appointments` and `calendar_blocks` exist **inside** each schema; there is no `tenant_id` column—the schema is the boundary.
- **user-service**: `tenants` table, `users.tenant_id`, User API exposes `tenantId`. Source of truth for which tenant a user belongs to.
- **appointment-service**:
  - **V4 migration**: Creates schemas `tenant_1` and `tenant_2` and the same table DDL in each (for demo/portfolio).
  - **TenantAwareDataSource**: Wraps the JDBC DataSource; on each `getConnection()` it runs `SET search_path TO tenant_<id>` so all queries in that request hit the correct schema.
  - **TenantContextFilter**: After JWT validation, resolves the current user’s `tenantId` from user-service and sets it in `TenantContext` (ThreadLocal). The datasource uses this to set the schema.
  - **Validation**: Booking and calendar operations still validate that the therapist (and client) belong to the **current user’s tenant** (same-tenant rule) so the API cannot be used to cross tenants.

## Why schema-per-tenant for this project

- **Portfolio / MVP**: Shows you can implement a “proper” multi-tenant design (schema isolation, routing, migrations) rather than a simple `tenant_id` column—stronger signal for Fiverr/Upwork and technical reviewers.
- **Isolation**: Data is isolated by schema; no risk of forgetting a `WHERE tenant_id = ?` and leaking data.
- **Extensibility**: Per-tenant backup/restore, moving a tenant to another DB, or compliance requirements are easier to explain and implement later.

## When to use which approach

| Use case | Prefer |
|----------|--------|
| Portfolio, MVP, client wants “real” multi-tenancy, compliance / isolation matters | **Schema-per-tenant** (this repo) |
| Very high number of tenants (e.g. 10k+), single schema, simpler ops | Row-level (`tenant_id` on tables) |
| Need to add tenants dynamically without new migrations | Schema-per-tenant with “create schema + run DDL” on tenant signup |

Documenting both in the README or in this doc (as we do here) shows you understand the tradeoffs—good for proposals and interviews.

## Tradeoffs

### Schema-per-tenant (current)

- **Pros**: Strong isolation, clear story for clients, per-tenant backup/restore possible, no `tenant_id` in every query.
- **Cons**: Schema routing (filter + datasource) required; migrations must be run per new schema (or scripted when creating a tenant); slightly more moving parts.

### Row-level (alternative)

- **Pros**: One schema, one set of migrations, simpler to add tenants (just insert and use `tenant_id`).
- **Cons**: Every query must include `tenant_id`; one missed filter can cause cross-tenant data leak.

## Security notes

- Tenant is resolved from the **current authenticated user** (JWT → user-service → `tenantId`) and set in `TenantContext` before any appointment-service DB access.
- All appointment/calendar reads and writes run in that tenant’s schema, so isolation is enforced by the DB path. Same-tenant checks in the API ensure therapist/client belong to the current user’s tenant.
- Unauthenticated requests (e.g. health checks) use default schema `tenant_1` so the app can start and health checks can run.

## Optional next steps

1. **Tenant in JWT**: Put `tenantId` in the access token to avoid an extra user-service call per request.
2. **Tenant admin API**: Create tenants and assign users (e.g. admin-only).
3. **Dynamic tenant creation**: On tenant signup, run `CREATE SCHEMA tenant_<id>` and apply the same DDL (e.g. from a template or Flyway callback).

-- Schema-per-tenant: each practice gets its own PostgreSQL schema for strong isolation.
-- Tables have the same structure; no tenant_id column (schema is the boundary).
-- Demo: tenant_1 and tenant_2 for portfolio / multi-tenant showcase.

CREATE SCHEMA IF NOT EXISTS tenant_1;
CREATE TABLE tenant_1.appointments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    therapist_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    google_meet_link VARCHAR(255),
    version BIGINT DEFAULT 0 NOT NULL
);
CREATE TABLE tenant_1.calendar_blocks (
    id BIGSERIAL PRIMARY KEY,
    therapist_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    reason VARCHAR(255)
);

CREATE SCHEMA IF NOT EXISTS tenant_2;
CREATE TABLE tenant_2.appointments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    therapist_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    google_meet_link VARCHAR(255),
    version BIGINT DEFAULT 0 NOT NULL
);
CREATE TABLE tenant_2.calendar_blocks (
    id BIGSERIAL PRIMARY KEY,
    therapist_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    reason VARCHAR(255)
);

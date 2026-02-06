CREATE TABLE credentials (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL, -- references User MS (logical, not DB-level FK)
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    refresh_token VARCHAR(255),
    refresh_token_expiry TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

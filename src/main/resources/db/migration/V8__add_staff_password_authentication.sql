-- Staff credentials are independent of Flutter/Firebase identities.
CREATE TABLE staff_accounts (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
    auth_version BIGINT NOT NULL DEFAULT 0 CHECK (auth_version >= 0),
    failed_attempts INTEGER NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0),
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (username = lower(username)),
    CHECK (username ~ '^[a-z0-9][a-z0-9._-]{2,63}$')
);

-- A metadata lock serializes opt-in bootstrap across application instances.
-- No credentials or staff users are inserted by this migration.
CREATE TABLE staff_bootstrap_state (
    id SMALLINT PRIMARY KEY CHECK (id = 1)
);
INSERT INTO staff_bootstrap_state(id) VALUES (1);

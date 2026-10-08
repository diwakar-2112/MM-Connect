CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Identity & Auth
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100) UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id) WHERE NOT revoked;

-- 2. Master Data / Admin-managed Dropdowns
CREATE TABLE lookup_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    parent_code VARCHAR(50),
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT uq_category_code UNIQUE (category, code)
);

CREATE INDEX idx_lookup_category ON lookup_options(category) WHERE is_active;

-- 3. Profiles & Photos
CREATE TABLE profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    profile_created_for VARCHAR(20) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    date_of_birth DATE NOT NULL,
    height_cm INT,
    marital_status VARCHAR(20) NOT NULL,
    religion VARCHAR(50),
    community VARCHAR(50),
    show_community BOOLEAN NOT NULL DEFAULT TRUE,
    highest_qualification VARCHAR(100),
    field_of_study VARCHAR(100),
    profession_domain VARCHAR(100),
    employment_type VARCHAR(50),
    annual_income BIGINT,
    current_city VARCHAR(100) NOT NULL,
    native_place VARCHAR(100),
    family_type VARCHAR(20),
    father_occupation VARCHAR(100),
    food_preference VARCHAR(30),
    smoking VARCHAR(30),
    alcohol VARCHAR(30),
    interests TEXT[],
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE profile_photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    storage_key VARCHAR(255) NOT NULL,
    thumbnail_key VARCHAR(255),
    blurred_key VARCHAR(255),
    visibility VARCHAR(30) NOT NULL DEFAULT 'ALL_VERIFIED',
    moderation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_photos_user ON profile_photos(user_id);

-- 4. Partner Preferences
CREATE TABLE partner_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    pref_age_min INT NOT NULL DEFAULT 21,
    pref_age_max INT NOT NULL DEFAULT 60,
    pref_locations TEXT[],
    pref_education TEXT[],
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Reciprocal Matching (Flat denormalized table for fast discovery)
CREATE TABLE match_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    gender VARCHAR(20) NOT NULL,
    birth_date DATE NOT NULL,
    age INT NOT NULL,
    marital_status VARCHAR(20) NOT NULL,
    religion VARCHAR(50),
    community VARCHAR(50),
    current_city VARCHAR(100) NOT NULL,
    pref_age_min INT NOT NULL,
    pref_age_max INT NOT NULL,
    pref_locations TEXT[],
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_match_discovery ON match_profiles(gender, is_active, is_verified, age);

-- 6. Connections (Request state machine with optimistic locking)
CREATE TABLE connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    note VARCHAR(250),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_sender_receiver UNIQUE (sender_id, receiver_id),
    CONSTRAINT chk_no_self_connection CHECK (sender_id <> receiver_id)
);

CREATE INDEX idx_connections_receiver ON connections(receiver_id, status);
CREATE INDEX idx_connections_sender ON connections(sender_id, status);

-- 7. Interactions (Anti-join table: Passed, Shortlisted, Blocked)
CREATE TABLE user_interactions (
    actor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (actor_id, target_id, action)
);

-- 8. Platform Settings & Feature Flags
CREATE TABLE platform_settings (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value JSONB NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO platform_settings (config_key, config_value) VALUES
('monetization', '{"enabled": false, "freeInterestsPerMonth": null}'),
('contact_reveal_policy', '{"activePolicy": "ON_ACCEPT"}'),
('verification', '{"mode": "SELF_DECLARED"}');
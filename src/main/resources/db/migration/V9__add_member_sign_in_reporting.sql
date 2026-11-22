-- Reporting fields are additive. Historical login timestamps are unknown.
ALTER TABLE users ADD COLUMN sign_in_name VARCHAR(100);
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMPTZ;

CREATE INDEX users_member_report_created ON users(created_at DESC, id DESC)
    WHERE role = 'ROLE_USER' AND deleted_at IS NULL
      AND (firebase_uid IS NOT NULL OR last_login_at IS NOT NULL);

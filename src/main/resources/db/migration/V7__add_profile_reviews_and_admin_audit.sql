-- Additive foundation only. No accounts are promoted or automatically approved.
ALTER TABLE users ADD COLUMN first_approved_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN firebase_uid VARCHAR(128) UNIQUE;

CREATE TABLE profile_reviews (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    submitted_payload TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'CHANGES_REQUIRED', 'SUPERSEDED')),
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMPTZ,
    reviewer_id UUID REFERENCES users(id),
    member_feedback VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0,
    CHECK ((status IN ('APPROVED', 'CHANGES_REQUIRED')) =
           (reviewed_at IS NOT NULL AND reviewer_id IS NOT NULL))
);
CREATE UNIQUE INDEX profile_reviews_one_pending_per_user
    ON profile_reviews(user_id) WHERE status = 'PENDING';
CREATE INDEX profile_reviews_queue ON profile_reviews(status, submitted_at, id);
CREATE INDEX profile_reviews_member_latest ON profile_reviews(user_id, submitted_at DESC, id DESC);

CREATE TABLE admin_audit_events (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL REFERENCES users(id),
    target_user_id UUID NOT NULL REFERENCES users(id),
    review_id UUID REFERENCES profile_reviews(id),
    action VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX admin_audit_events_target ON admin_audit_events(target_user_id, created_at DESC);

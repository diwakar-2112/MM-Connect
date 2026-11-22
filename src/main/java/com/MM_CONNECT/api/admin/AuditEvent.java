package com.MM_CONNECT.api.admin;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Append-only application record. Do not put profile payloads or tokens here. */
@Entity
@Table(name = "admin_audit_events")
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;
    @Column(name = "target_user_id", nullable = false, updatable = false)
    private UUID targetUserId;
    @Column(name = "review_id", updatable = false)
    private UUID reviewId;
    @Column(nullable = false, length = 40, updatable = false)
    private String action;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AuditEvent() {}

    public AuditEvent(UUID actorId, UUID targetUserId, UUID reviewId, String action) {
        this.actorId = actorId;
        this.targetUserId = targetUserId;
        this.reviewId = reviewId;
        this.action = action;
    }
}

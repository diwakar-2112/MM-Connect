package com.MM_CONNECT.api.matching;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_interactions")
@IdClass(UserInteraction.InteractionKey.class)
public class UserInteraction {

    @Id
    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Id
    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Id
    @Column(name = "action", length = 20, nullable = false)
    private String action; // PASSED, SHORTLISTED, BLOCKED

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public UserInteraction() {}

    public UserInteraction(UUID actorId, UUID targetId, String action) {
        this.actorId = actorId;
        this.targetId = targetId;
        this.action = action;
        this.createdAt = Instant.now();
    }

    public UUID getActorId() { return actorId; }
    public UUID getTargetId() { return targetId; }
    public String getAction() { return action; }
    public Instant getCreatedAt() { return createdAt; }

    // Composite Key Class
    public static class InteractionKey implements Serializable {
        private UUID actorId;
        private UUID targetId;
        private String action;

        public InteractionKey() {}

        public InteractionKey(UUID actorId, UUID targetId, String action) {
            this.actorId = actorId;
            this.targetId = targetId;
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof InteractionKey that)) return false;
            return Objects.equals(actorId, that.actorId)
                    && Objects.equals(targetId, that.targetId)
                    && Objects.equals(action, that.action);
        }

        @Override
        public int hashCode() {
            return Objects.hash(actorId, targetId, action);
        }
    }
}
package com.MM_CONNECT.api.admin.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "staff_accounts")
public class StaffAccount {
    @Id @Column(name = "user_id") private UUID userId;
    @Column(nullable = false, unique = true, length = 64) private String username;
    @Column(name = "password_hash", nullable = false, length = 255) private String passwordHash;
    @Column(name = "password_change_required", nullable = false) private boolean passwordChangeRequired = true;
    @Column(name = "auth_version", nullable = false) private long authVersion;
    @Column(name = "failed_attempts", nullable = false) private int failedAttempts;
    @Column(name = "locked_until") private Instant lockedUntil;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected StaffAccount() {}
    public StaffAccount(UUID userId, String username, String passwordHash, Instant now) {
        this.userId = userId; this.username = username; this.passwordHash = passwordHash;
        this.createdAt = now; this.updatedAt = now;
    }
    public UUID getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isPasswordChangeRequired() { return passwordChangeRequired; }
    public long getAuthVersion() { return authVersion; }
    public int getFailedAttempts() { return failedAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getCreatedAt() { return createdAt; }
    public void setPasswordHash(String value) { passwordHash = value; }
    public void setPasswordChangeRequired(boolean value) { passwordChangeRequired = value; }
    public void revokeTokens() { authVersion = Math.incrementExact(authVersion); }
    public void setFailedAttempts(int value) { failedAttempts = value; }
    public void setLockedUntil(Instant value) { lockedUntil = value; }
    public void touch(Instant now) { updatedAt = now; }
}

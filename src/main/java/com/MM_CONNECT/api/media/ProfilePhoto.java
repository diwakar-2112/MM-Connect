package com.MM_CONNECT.api.media;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profile_photos")
public class ProfilePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "visibility", nullable = false)
    private String visibility = "ALL_VERIFIED"; // ALL_VERIFIED, ACCEPTED_ONLY

    @Column(name = "moderation_status", nullable = false)
    private String moderationStatus = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public ProfilePhoto() {}

    public ProfilePhoto(UUID userId, String storageKey, boolean isPrimary) {
        this.userId = userId;
        this.storageKey = storageKey;
        this.isPrimary = isPrimary;
    }

    // --- Getters and Setters ---
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getStorageKey() { return storageKey; }
    public String getVisibility() { return visibility; }
    public String getModerationStatus() { return moderationStatus; }
    public boolean isPrimary() { return isPrimary; }
}
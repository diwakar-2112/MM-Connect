package com.MM_CONNECT.api.verification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** An immutable submitted payload; decisions change status, never its contents. */
@Entity
@Table(name = "profile_reviews")
public class ProfileReview {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(name = "submitted_payload", nullable = false, columnDefinition = "text", updatable = false)
    private String submittedPayload;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private ReviewStatus status = ReviewStatus.PENDING;
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt = Instant.now();
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @Column(name = "reviewer_id")
    private UUID reviewerId;
    @Column(name = "member_feedback", length = 1000)
    private String memberFeedback;
    @Version
    private long version;

    protected ProfileReview() {}

    public ProfileReview(UUID userId, String submittedPayload) {
        this.userId = userId;
        this.submittedPayload = submittedPayload;
    }

    public void supersede() { status = ReviewStatus.SUPERSEDED; }

    public void decide(ReviewStatus decision, UUID reviewerId, String feedback) {
        status = decision;
        this.reviewerId = reviewerId;
        memberFeedback = feedback;
        reviewedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getSubmittedPayload() { return submittedPayload; }
    public ReviewStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public UUID getReviewerId() { return reviewerId; }
    public String getMemberFeedback() { return memberFeedback; }
    public long getVersion() { return version; }
}

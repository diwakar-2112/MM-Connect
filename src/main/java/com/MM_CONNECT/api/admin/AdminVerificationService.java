package com.MM_CONNECT.api.admin;

import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import com.MM_CONNECT.api.media.PhotoUploadService;
import com.MM_CONNECT.api.media.ProfilePhotoRepository;
import com.MM_CONNECT.api.profile.ProfileWriter;
import com.MM_CONNECT.api.verification.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.UUID;

@Service
public class AdminVerificationService {
    private final StaffAccessService access;
    private final UserRepository users;
    private final ProfileReviewRepository reviews;
    private final ReviewPayloadCodec codec;
    private final ProfileWriter writer;
    private final AuditEventRepository audit;
    private final ProfilePhotoRepository photos;
    private final PhotoUploadService storage;

    public AdminVerificationService(StaffAccessService access, UserRepository users,
            ProfileReviewRepository reviews, ReviewPayloadCodec codec, ProfileWriter writer,
            AuditEventRepository audit, ProfilePhotoRepository photos, PhotoUploadService storage) {
        this.access = access;
        this.users = users;
        this.reviews = reviews;
        this.codec = codec;
        this.writer = writer;
        this.audit = audit;
        this.photos = photos;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public Page<VerificationDtos.QueueItem> queue(UUID actorId, ReviewStatus status, int page, int size) {
        access.requireReviewer(actorId);
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be nonnegative and size must be 1–100.");
        }
        return reviews.findByStatus(status, PageRequest.of(page, size,
                Sort.by("submittedAt").ascending().and(Sort.by("id").ascending())))
                .map(this::item);
    }

    @Transactional
    public VerificationDtos.ReviewDetail detail(UUID actorId, UUID reviewId) {
        access.requireReviewer(actorId);
        ProfileReview review = reviews.findById(reviewId).orElseThrow(this::notFound);
        var photoViews = photos.findProfilePhotos(review.getUserId()).stream()
                .map(p -> new VerificationDtos.ReviewPhoto(p.getId(),
                        storage.generatePresignedGetUrl(p.getStorageKey()), p.getModerationStatus(), p.isPrimary()))
                .toList();
        audit.save(new AuditEvent(actorId, review.getUserId(), reviewId, "REVIEW_VIEWED"));
        return new VerificationDtos.ReviewDetail(item(review), codec.decode(review.getSubmittedPayload()),
                review.getReviewerId(), review.getReviewedAt(), review.getMemberFeedback(), photoViews);
    }

    @Transactional
    public VerificationDtos.QueueItem decide(UUID actorId, UUID reviewId, long expectedVersion,
                                             ReviewStatus decision, String feedback) {
        access.requireReviewer(actorId);
        if (decision != ReviewStatus.APPROVED && decision != ReviewStatus.CHANGES_REQUIRED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported review decision.");
        }
        if (decision == ReviewStatus.CHANGES_REQUIRED
                && (feedback == null || feedback.isBlank() || feedback.length() > 1000)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide correction feedback (1–1000 characters).");
        }
        // Fetch only the scalar first, avoiding a stale managed review before acquiring the account lock.
        UUID userId = reviews.findReviewUserId(reviewId).orElseThrow(this::notFound);
        if (actorId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot review your own profile.");
        }
        User member = users.findByIdForUpdate(userId)
                .filter(u -> u.isActive() && u.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Member account is not active."));
        ProfileReview review = reviews.findById(reviewId).orElseThrow(this::notFound);
        if (review.getStatus() != ReviewStatus.PENDING || review.getVersion() != expectedVersion) {
            var error = new ResponseStatusException(HttpStatus.CONFLICT, "Review changed. Reload before deciding.");
            error.getBody().setProperty("code", "STALE_REVIEW");
            throw error;
        }
        if (decision == ReviewStatus.APPROVED) {
            writer.write(userId, codec.decode(review.getSubmittedPayload()), true);
            member.setVerified(true);
            if (member.getFirstApprovedAt() == null) member.setFirstApprovedAt(Instant.now());
            member.setUpdatedAt(Instant.now());
            users.save(member);
        }
        // Requesting corrections never revokes an established first approval.
        review.decide(decision, actorId, decision == ReviewStatus.CHANGES_REQUIRED ? feedback.trim() : null);
        reviews.saveAndFlush(review);
        audit.save(new AuditEvent(actorId, userId, reviewId, "PROFILE_" + decision.name()));
        return item(review);
    }

    private VerificationDtos.QueueItem item(ProfileReview review) {
        return new VerificationDtos.QueueItem(review.getId(), review.getUserId(), review.getStatus(),
                review.getSubmittedAt(), review.getVersion());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found.");
    }
}

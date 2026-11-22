package com.MM_CONNECT.api.verification;

import com.MM_CONNECT.api.profile.dto.OnboardingRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class VerificationDtos {
    private VerificationDtos() {}

    public record MemberStatus(boolean onboardingComplete, boolean verified,
                               boolean profileLocked, String status, Instant firstApprovedAt,
                               UUID reviewId, Long version, String feedback,
                               OnboardingRequest pendingSubmission) {}
    public record QueueItem(UUID reviewId, UUID userId, ReviewStatus status,
                            Instant submittedAt, long version) {}
    public record ReviewDetail(QueueItem review, OnboardingRequest submission,
                               UUID reviewerId, Instant reviewedAt, String feedback,
                               List<ReviewPhoto> photos) {}
    public record ReviewPhoto(UUID photoId, String photoUrl, String moderationStatus, boolean isPrimary) {}
    public record Approve(@NotNull @PositiveOrZero Long expectedVersion) {}
    public record RequestChanges(@NotNull @PositiveOrZero Long expectedVersion,
                                 @NotBlank @Size(max = 1000) String feedback) {}
}

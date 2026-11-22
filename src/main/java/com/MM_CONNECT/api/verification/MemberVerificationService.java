package com.MM_CONNECT.api.verification;

import com.MM_CONNECT.api.admin.AuditEvent;
import com.MM_CONNECT.api.admin.AuditEventRepository;
import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import com.MM_CONNECT.api.profile.ProfileRepository;
import com.MM_CONNECT.api.profile.ProfileWriter;
import com.MM_CONNECT.api.profile.dto.OnboardingRequest;
import com.MM_CONNECT.api.profile.dto.OnboardingResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class MemberVerificationService {
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final ProfileWriter writer;
    private final ProfileReviewRepository reviews;
    private final ReviewPayloadCodec codec;
    private final AuditEventRepository audit;

    public MemberVerificationService(UserRepository users, ProfileRepository profiles, ProfileWriter writer,
                                     ProfileReviewRepository reviews, ReviewPayloadCodec codec,
                                     AuditEventRepository audit) {
        this.users = users;
        this.profiles = profiles;
        this.writer = writer;
        this.reviews = reviews;
        this.codec = codec;
        this.audit = audit;
    }

    @Transactional
    public OnboardingResponse submit(UUID userId, OnboardingRequest request, boolean initial) {
        User user = users.findByIdForUpdate(userId)
                .filter(u -> u.isActive() && u.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active."));
        boolean exists = profiles.existsById(userId);
        if (initial && exists) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Profile already exists. Use PUT /v1/profiles/me to resubmit.");
        if (!initial && !exists) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Profile not found. Complete onboarding first.");

        reviews.findByUserIdAndStatus(userId, ReviewStatus.PENDING).ifPresent(previous -> {
            previous.supersede();
            reviews.saveAndFlush(previous); // Release the unique pending slot before insertion.
        });
        ProfileReview review = reviews.saveAndFlush(new ProfileReview(userId, codec.encode(request)));
        // Initial submissions are private; approved canonical data changes only on approval.
        if (!user.isVerified()) writer.write(userId, request, false);
        audit.save(new AuditEvent(userId, userId, review.getId(), "PROFILE_SUBMITTED"));
        return new OnboardingResponse("Profile submitted for review.", userId, "UNDER_REVIEW", user.isVerified());
    }

    @Transactional(readOnly = true)
    public VerificationDtos.MemberStatus status(UUID userId) {
        User user = users.findById(userId).filter(u -> u.isActive() && u.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active."));
        boolean onboarded = profiles.existsById(userId);
        ProfileReview latest = reviews.findFirstByUserIdOrderBySubmittedAtDescIdDesc(userId).orElse(null);
        String status = latest == null ? (user.isVerified() ? "APPROVED" : "NOT_SUBMITTED")
                : latest.getStatus().name();
        return new VerificationDtos.MemberStatus(onboarded, user.isVerified(),
                !onboarded || !user.isVerified(), status, user.getFirstApprovedAt(),
                latest == null ? null : latest.getId(), latest == null ? null : latest.getVersion(),
                latest == null ? null : latest.getMemberFeedback(),
                latest != null && latest.getStatus() != ReviewStatus.APPROVED
                        ? codec.decode(latest.getSubmittedPayload()) : null);
    }
}

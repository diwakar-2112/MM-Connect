package com.MM_CONNECT.api.admin;

import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import com.MM_CONNECT.api.media.PhotoUploadService;
import com.MM_CONNECT.api.media.ProfilePhotoRepository;
import com.MM_CONNECT.api.profile.ProfileWriter;
import com.MM_CONNECT.api.verification.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminVerificationServiceTests {
    private final UUID actorId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private final UUID reviewId = UUID.randomUUID();
    private UserRepository users;
    private ProfileReviewRepository reviews;
    private ProfileWriter writer;
    private AuditEventRepository audit;
    private AdminVerificationService service;
    private StaffAccessService access;
    private User actor;
    private User member;
    private ProfileReview review;
    private final ReviewPayloadCodec codec = new ReviewPayloadCodec();

    @BeforeEach
    void setup() {
        users = mock(UserRepository.class);
        reviews = mock(ProfileReviewRepository.class);
        writer = mock(ProfileWriter.class);
        audit = mock(AuditEventRepository.class);
        access = new StaffAccessService(users);
        actor = new User(); actor.setId(actorId); actor.setRole("ROLE_ADMIN");
        member = new User(); member.setId(memberId);
        review = new ProfileReview(memberId, codec.encode(MemberVerificationServiceTests.payload()));
        ReflectionTestUtils.setField(review, "id", reviewId);
        when(users.findById(actorId)).thenReturn(Optional.of(actor));
        when(users.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
        when(reviews.findReviewUserId(reviewId)).thenReturn(Optional.of(memberId));
        when(reviews.findById(reviewId)).thenReturn(Optional.of(review));
        service = new AdminVerificationService(access, users, reviews, codec, writer, audit,
                mock(ProfilePhotoRepository.class), mock(PhotoUploadService.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_SUPERADMIN"})
    void staffCanApproveTheExactSubmissionAndPublishAtomically(String role) {
        actor.setRole(role);
        var result = service.decide(actorId, reviewId, 0, ReviewStatus.APPROVED, null);
        assertTrue(member.isVerified());
        assertNotNull(member.getFirstApprovedAt());
        assertEquals(ReviewStatus.APPROVED, result.status());
        verify(writer).write(memberId, MemberVerificationServiceTests.payload(), true);
        verify(audit).save(any(AuditEvent.class));
        var order = inOrder(users, reviews);
        order.verify(users).findById(actorId);
        order.verify(reviews).findReviewUserId(reviewId);
        order.verify(users).findByIdForUpdate(memberId);
        order.verify(reviews).findById(reviewId);
    }

    @Test
    void askingForCorrectionsToLaterEditsDoesNotRelockOrOverwriteApprovedData() {
        member.setVerified(true);
        Instant first = Instant.parse("2026-01-01T00:00:00Z");
        member.setFirstApprovedAt(first);
        service.decide(actorId, reviewId, 0, ReviewStatus.CHANGES_REQUIRED, "Please correct your city.");
        assertTrue(member.isVerified());
        assertEquals(first, member.getFirstApprovedAt());
        assertEquals("Please correct your city.", review.getMemberFeedback());
        verifyNoInteractions(writer);
    }

    @Test
    void staleVersionOrSupersededReviewCannotPublishOrAuditSuccess() {
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.decide(actorId, reviewId, 7, ReviewStatus.APPROVED, null)).getStatusCode());
        review.supersede();
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.decide(actorId, reviewId, 0, ReviewStatus.APPROVED, null)).getStatusCode());
        assertFalse(member.isVerified());
        verifyNoInteractions(writer, audit);
    }

    @Test
    void memberCannotUseReviewApisAndAdminCannotUseSuperadminPermission() {
        assertThrows(ResponseStatusException.class, () -> access.requireSuperadmin(actorId));
        actor.setRole("ROLE_USER");
        assertThrows(ResponseStatusException.class,
                () -> service.decide(actorId, reviewId, 0, ReviewStatus.APPROVED, null));
        verifyNoInteractions(reviews, writer, audit);
    }

    @Test
    void demotedOrSuspendedStaffAndSelfReviewAreDenied() {
        actor.setActive(false);
        assertThrows(ResponseStatusException.class, () -> service.queue(actorId, ReviewStatus.PENDING, 0, 20));
        actor.setActive(true);
        when(reviews.findReviewUserId(reviewId)).thenReturn(Optional.of(actorId));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> service.decide(actorId, reviewId, 0, ReviewStatus.APPROVED, null)).getStatusCode());
        verifyNoInteractions(writer, audit);
    }

    @Test
    void rejectedInitialSubmissionRemainsUnverifiedAndAllowsAReason() {
        service.decide(actorId, reviewId, 0, ReviewStatus.CHANGES_REQUIRED, "Please correct the details.");
        assertFalse(member.isVerified());
        assertNull(member.getFirstApprovedAt());
        assertEquals(ReviewStatus.CHANGES_REQUIRED, review.getStatus());
    }
}

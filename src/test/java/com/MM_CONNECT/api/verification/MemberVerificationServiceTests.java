package com.MM_CONNECT.api.verification;

import com.MM_CONNECT.api.admin.AuditEventRepository;
import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import com.MM_CONNECT.api.profile.ProfileRepository;
import com.MM_CONNECT.api.profile.ProfileWriter;
import com.MM_CONNECT.api.profile.dto.OnboardingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MemberVerificationServiceTests {
    private final UUID userId = UUID.randomUUID();
    private User user;
    private UserRepository users;
    private ProfileRepository profiles;
    private ProfileWriter writer;
    private ProfileReviewRepository reviews;
    private MemberVerificationService service;
    private final ReviewPayloadCodec codec = new ReviewPayloadCodec();

    @BeforeEach
    void setup() {
        users = mock(UserRepository.class);
        profiles = mock(ProfileRepository.class);
        writer = mock(ProfileWriter.class);
        reviews = mock(ProfileReviewRepository.class);
        user = new User();
        user.setId(userId);
        when(users.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(reviews.saveAndFlush(any(ProfileReview.class))).thenAnswer(invocation -> {
            ProfileReview review = invocation.getArgument(0);
            if (review.getId() == null) ReflectionTestUtils.setField(review, "id", UUID.randomUUID());
            return review;
        });
        service = new MemberVerificationService(users, profiles, writer, reviews, codec, mock(AuditEventRepository.class));
    }

    @Test
    void initialSubmissionStaysUnpublishedAndLocked() {
        var payload = payload();
        var response = service.submit(userId, payload, true);
        assertEquals("UNDER_REVIEW", response.status());
        assertFalse(response.discoveryFeedUnlocked());
        assertFalse(user.isVerified());
        verify(writer).write(userId, payload, false);
        verify(users).findByIdForUpdate(userId);
    }

    @Test
    void approvedMembersEditsDoNotOverwriteCanonicalProfileOrRemoveApproval() {
        user.setVerified(true);
        when(profiles.existsById(userId)).thenReturn(true);
        var response = service.submit(userId, payload(), false);
        assertTrue(response.discoveryFeedUnlocked());
        assertTrue(user.isVerified());
        verifyNoInteractions(writer);
        verify(reviews).saveAndFlush(any(ProfileReview.class));
    }

    @Test
    void newerSubmissionSupersedesPendingRevisionBeforeInserting() {
        ProfileReview previous = new ProfileReview(userId, codec.encode(payload()));
        when(profiles.existsById(userId)).thenReturn(true);
        when(reviews.findByUserIdAndStatus(userId, ReviewStatus.PENDING)).thenReturn(Optional.of(previous));
        service.submit(userId, payload(), false);
        assertEquals(ReviewStatus.SUPERSEDED, previous.getStatus());
        var order = inOrder(reviews);
        order.verify(reviews).findByUserIdAndStatus(userId, ReviewStatus.PENDING);
        order.verify(reviews).saveAndFlush(previous);
        order.verify(reviews).saveAndFlush(argThat(r -> r != previous && r.getStatus() == ReviewStatus.PENDING));
    }

    @Test
    void pendingEditStatusKeepsApprovedMemberUnlockedAndIncludesTheirDraft() {
        user.setVerified(true);
        when(profiles.existsById(userId)).thenReturn(true);
        var pending = new ProfileReview(userId, codec.encode(payload()));
        when(reviews.findFirstByUserIdOrderBySubmittedAtDescIdDesc(userId)).thenReturn(Optional.of(pending));
        var status = service.status(userId);
        assertFalse(status.profileLocked());
        assertEquals("PENDING", status.status());
        assertEquals(payload(), status.pendingSubmission());
    }

    @Test
    void duplicateOnboardingAndEditingMissingProfileDoNotSave() {
        when(profiles.existsById(userId)).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> service.submit(userId, payload(), true));
        when(profiles.existsById(userId)).thenReturn(false);
        assertThrows(ResponseStatusException.class, () -> service.submit(userId, payload(), false));
        verifyNoInteractions(reviews, writer);
    }

    @Test
    void suspendedAccountCannotSubmitEvenAfterItHasAnAccessToken() {
        user.setActive(false);
        assertThrows(ResponseStatusException.class, () -> service.submit(userId, payload(), true));
        verifyNoInteractions(profiles, reviews, writer);
    }

    public static OnboardingRequest payload() {
        return new OnboardingRequest("MYSELF",
                new OnboardingRequest.BasicDetails("Example Member", "WOMAN", LocalDate.of(1995, 1, 1), 165, "NEVER_MARRIED", "HINDI"),
                new OnboardingRequest.Background("HINDU", "BAIRAGI", true, "RAMANANDI", "AGASTYA", null, null),
                new OnboardingRequest.EducationAndCareer("BACHELORS", "SCIENCE", "SOFTWARE", "EMPLOYED", 1000000L, "Pune"),
                new OnboardingRequest.LocationAndFamily("Pune", "Jaipur", "NUCLEAR", null, null, null, "Maharashtra", null, null),
                new OnboardingRequest.Lifestyle("VEGETARIAN", "NEVER", "NEVER", List.of("READING"), null),
                new OnboardingRequest.PartnerPreferencesDto(24, 35, List.of(), List.of(), List.of(), false, false, List.of()));
    }
}

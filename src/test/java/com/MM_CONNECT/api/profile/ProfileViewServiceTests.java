package com.MM_CONNECT.api.profile;

import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import com.MM_CONNECT.api.connection.ConnectionRequestRepository;
import com.MM_CONNECT.api.matching.UserInteractionRepository;
import com.MM_CONNECT.api.media.PhotoUploadService;
import com.MM_CONNECT.api.media.ProfilePhoto;
import com.MM_CONNECT.api.media.ProfilePhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileViewServiceTests {

    private final UUID viewerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();
    private UserRepository users;
    private ProfileRepository profiles;
    private UserInteractionRepository interactions;
    private ConnectionRequestRepository connections;
    private ProfilePhotoRepository photos;
    private PhotoUploadService photoUrls;
    private ProfileViewService service;
    private User viewer;
    private User target;
    private Profile profile;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        profiles = mock(ProfileRepository.class);
        interactions = mock(UserInteractionRepository.class);
        connections = mock(ConnectionRequestRepository.class);
        photos = mock(ProfilePhotoRepository.class);
        photoUrls = mock(PhotoUploadService.class);
        service = new ProfileViewService(users, profiles, interactions, connections, photos, photoUrls);

        viewer = new User(null, "viewer@example.test");
        viewer.setId(viewerId);
        target = new User("1234567890", "private@example.test");
        target.setId(targetId);
        target.setVerified(true);
        profile = new Profile();
        profile.setUserId(targetId);
        profile.setFullName("Example Partner");
        profile.setDateOfBirth(LocalDate.now().minusYears(28));
        profile.setCurrentCity("Pune");
        profile.setCurrentState("Maharashtra");
        profile.setNativePlace("Sikar");
        profile.setNativeDistrict("Sikar");
        profile.setWorkCity("Mumbai");
        profile.setRelocationPreference("DEPENDS");
        profile.setCommunity("PRIVATE_COMMUNITY");
        profile.setSubCommunity("PRIVATE_BRANCH");
        profile.setAnnualIncome(1200000L);
        when(users.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(users.findById(targetId)).thenReturn(Optional.of(target));
        when(profiles.existsById(viewerId)).thenReturn(true);
        when(profiles.findById(targetId)).thenReturn(Optional.of(profile));
        when(photos.findProfilePhotos(targetId)).thenReturn(List.of());
    }

    @Test
    void returnsCurrentAgeAndNewLocationFieldsWhileHidingCommunityOnRequest() {
        profile.setShowCommunity(false);

        var details = service.getProfile(viewerId, targetId).profileDetails();

        assertEquals(targetId, details.userId());
        assertEquals(28, details.age());
        assertTrue(details.verified());
        assertEquals("Maharashtra", details.currentState());
        assertEquals("Sikar", details.nativeDistrict());
        assertEquals("Mumbai", details.workCity());
        assertEquals("DEPENDS", details.relocationPreference());
        assertFalse(details.showCommunity());
        assertNull(details.community());
        assertNull(details.subCommunity());
    }

    @Test
    void visibleCommunityIsReturnedWhenOwnerAllowsIt() {
        var details = service.getProfile(viewerId, targetId).profileDetails();

        assertEquals("PRIVATE_COMMUNITY", details.community());
        assertEquals("PRIVATE_BRANCH", details.subCommunity());
    }

    @Test
    void onlySignsApprovedPhotosPermittedForTheViewer() {
        viewer.setVerified(true);
        ProfilePhoto pending = photo("pending.jpg", "PENDING", "ALL_VERIFIED");
        ProfilePhoto rejected = photo("rejected.jpg", "REJECTED", "ALL_VERIFIED");
        ProfilePhoto unknown = photo("unknown.jpg", "APPROVED", "UNKNOWN");
        ProfilePhoto visible = photo("visible.jpg", "APPROVED", "ALL_VERIFIED");
        when(photos.findProfilePhotos(targetId)).thenReturn(List.of(pending, rejected, unknown, visible));
        when(photoUrls.generatePresignedGetUrl("visible.jpg")).thenReturn("https://example.test/signed");

        var response = service.getProfile(viewerId, targetId);

        assertEquals(1, response.photos().size());
        assertEquals(visible.getId(), response.photos().get(0).photoId());
        assertEquals("https://example.test/signed", response.photos().get(0).photoUrl());
        verify(photoUrls).generatePresignedGetUrl("visible.jpg");
        verifyNoMoreInteractions(photoUrls);
        verifyNoInteractions(connections);
    }

    @Test
    void unverifiedViewerCannotReadAllVerifiedPhotos() {
        when(photos.findProfilePhotos(targetId))
                .thenReturn(List.of(photo("verified.jpg", "APPROVED", "ALL_VERIFIED")));

        assertTrue(service.getProfile(viewerId, targetId).photos().isEmpty());
        verifyNoInteractions(photoUrls);
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true"})
    void acceptedOnlyPhotosUnlockForAcceptedConnectionInEitherDirection(boolean forward, boolean reverse) {
        when(photos.findProfilePhotos(targetId))
                .thenReturn(List.of(photo("accepted.jpg", "APPROVED", "ACCEPTED_ONLY")));
        when(connections.existsBySenderIdAndReceiverIdAndStatus(viewerId, targetId, "ACCEPTED"))
                .thenReturn(forward);
        when(connections.existsBySenderIdAndReceiverIdAndStatus(targetId, viewerId, "ACCEPTED"))
                .thenReturn(reverse);
        when(photoUrls.generatePresignedGetUrl("accepted.jpg")).thenReturn("https://example.test/accepted");

        assertEquals(1, service.getProfile(viewerId, targetId).photos().size());
    }

    @Test
    void verificationAloneDoesNotUnlockAcceptedOnlyPhotos() {
        viewer.setVerified(true);
        when(photos.findProfilePhotos(targetId))
                .thenReturn(List.of(photo("accepted.jpg", "APPROVED", "ACCEPTED_ONLY")));

        assertTrue(service.getProfile(viewerId, targetId).photos().isEmpty());
        verifyNoInteractions(photoUrls);
    }

    @Test
    void ownerCanReadTheirPendingPhotos() {
        profile.setUserId(viewerId);
        when(profiles.findById(viewerId)).thenReturn(Optional.of(profile));
        when(photos.findProfilePhotos(viewerId))
                .thenReturn(List.of(new ProfilePhoto(viewerId, "own-pending.jpg", true)));
        when(photoUrls.generatePresignedGetUrl("own-pending.jpg")).thenReturn("https://example.test/own");

        assertEquals(1, service.getProfile(viewerId, viewerId).photos().size());
        verifyNoInteractions(interactions, connections);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void blocksInEitherDirectionHideProfile(boolean viewerBlockedTarget) {
        UUID actor = viewerBlockedTarget ? viewerId : targetId;
        UUID blocked = viewerBlockedTarget ? targetId : viewerId;
        when(interactions.existsByActorIdAndTargetIdAndAction(actor, blocked, "BLOCKED")).thenReturn(true);

        var error = assertThrows(ResponseStatusException.class, () -> service.getProfile(viewerId, targetId));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertEquals("Profile not found", error.getReason());
        verifyNoInteractions(photos, photoUrls);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING", "INACTIVE", "DELETED", "NO_PROFILE"})
    void unavailableTargetReturnsNotFound(String condition) {
        switch (condition) {
            case "MISSING" -> when(users.findById(targetId)).thenReturn(Optional.empty());
            case "INACTIVE" -> target.setActive(false);
            case "DELETED" -> target.setDeletedAt(Instant.now());
            case "NO_PROFILE" -> when(profiles.findById(targetId)).thenReturn(Optional.empty());
        }

        var error = assertThrows(ResponseStatusException.class, () -> service.getProfile(viewerId, targetId));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(photos, photoUrls);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING", "INACTIVE", "DELETED"})
    void unavailableViewerCannotReadProfiles(String condition) {
        switch (condition) {
            case "MISSING" -> when(users.findById(viewerId)).thenReturn(Optional.empty());
            case "INACTIVE" -> viewer.setActive(false);
            case "DELETED" -> viewer.setDeletedAt(Instant.now());
        }

        var error = assertThrows(ResponseStatusException.class, () -> service.getProfile(viewerId, targetId));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(photos, photoUrls);
    }

    @Test
    void viewerMustCompleteOnboarding() {
        when(profiles.existsById(viewerId)).thenReturn(false);

        var error = assertThrows(ResponseStatusException.class, () -> service.getProfile(viewerId, targetId));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        assertEquals("Please finish onboarding to view this profile.", error.getReason());
        assertEquals("ONBOARDING_REQUIRED", error.getBody().getProperties().get("code"));
        verify(users, never()).findById(targetId);
        verify(profiles, never()).findById(targetId);
        verifyNoInteractions(photos, photoUrls);
    }

    private ProfilePhoto photo(String key, String moderation, String visibility) {
        ProfilePhoto photo = new ProfilePhoto(targetId, key, false);
        ReflectionTestUtils.setField(photo, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(photo, "moderationStatus", moderation);
        ReflectionTestUtils.setField(photo, "visibility", visibility);
        return photo;
    }
}

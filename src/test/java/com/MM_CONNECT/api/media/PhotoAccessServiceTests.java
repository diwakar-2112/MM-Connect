package com.MM_CONNECT.api.media;

import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import com.MM_CONNECT.api.connection.ConnectionRequestRepository;
import com.MM_CONNECT.api.matching.UserInteractionRepository;
import com.MM_CONNECT.api.profile.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PhotoAccessServiceTests {
    private final UUID viewerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();
    private User viewer;
    private User target;
    private ProfilePhoto photo;
    private PhotoAccessService service;
    private UserInteractionRepository interactions;
    private ConnectionRequestRepository connections;

    @BeforeEach
    void setup() {
        var users = mock(UserRepository.class);
        var profiles = mock(ProfileRepository.class);
        interactions = mock(UserInteractionRepository.class);
        connections = mock(ConnectionRequestRepository.class);
        viewer = new User(); viewer.setId(viewerId); viewer.setVerified(true);
        target = new User(); target.setId(targetId); target.setVerified(true);
        when(users.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(users.findById(targetId)).thenReturn(Optional.of(target));
        when(profiles.existsById(any())).thenReturn(true);
        photo = new ProfilePhoto(targetId, "key.jpg", true);
        service = new PhotoAccessService(users, profiles, mock(ProfilePhotoRepository.class), interactions, connections);
    }

    @Test
    void previewsAndPendingPhotosDoNotBypassVerification() {
        assertFalse(service.canView(viewerId, photo));
        ReflectionTestUtils.setField(photo, "moderationStatus", "APPROVED");
        assertTrue(service.canView(viewerId, photo));
        viewer.setVerified(false);
        assertFalse(service.canView(viewerId, photo));
        viewer.setVerified(true);
        target.setVerified(false);
        assertFalse(service.canView(viewerId, photo));
    }

    @Test
    void bilateralBlockAndAcceptedOnlyPolicyAreRespected() {
        ReflectionTestUtils.setField(photo, "moderationStatus", "APPROVED");
        ReflectionTestUtils.setField(photo, "visibility", "ACCEPTED_ONLY");
        assertFalse(service.canView(viewerId, photo));
        when(connections.existsBySenderIdAndReceiverIdAndStatus(targetId, viewerId, "ACCEPTED")).thenReturn(true);
        assertTrue(service.canView(viewerId, photo));
        when(interactions.existsByActorIdAndTargetIdAndAction(targetId, viewerId, "BLOCKED")).thenReturn(true);
        assertFalse(service.canView(viewerId, photo));
    }

    @Test
    void unverifiedOwnerCanSeeTheirOwnPendingPhotos() {
        target.setVerified(false);
        assertTrue(service.canView(targetId, photo));
        target.setActive(false);
        assertFalse(service.canView(targetId, photo));
    }
}

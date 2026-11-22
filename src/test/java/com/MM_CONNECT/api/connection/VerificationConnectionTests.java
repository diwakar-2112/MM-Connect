package com.MM_CONNECT.api.connection;

import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import com.MM_CONNECT.api.matching.UserInteractionRepository;
import com.MM_CONNECT.api.profile.ProfileRepository;
import com.MM_CONNECT.api.verification.MemberAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VerificationConnectionTests {
    @Test
    void initiallyUnverifiedMemberCannotUseAnyConnectionRouteEvenWithExistingProfile() {
        var users = mock(UserRepository.class);
        var profiles = mock(ProfileRepository.class);
        var requests = mock(ConnectionRequestRepository.class);
        UUID id = UUID.randomUUID(), other = UUID.randomUUID();
        User user = new User(); user.setId(id);
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(profiles.existsById(id)).thenReturn(true);
        var controller = new ConnectionController(requests, profiles, users,
                new MemberAccessService(users, profiles), mock(UserInteractionRepository.class));
        assertThrows(ResponseStatusException.class, () -> controller.sendRequest(id, other, null));
        assertThrows(ResponseStatusException.class, () -> controller.getSentRequests(id));
        assertThrows(ResponseStatusException.class, () -> controller.getReceivedRequests(id));
        assertThrows(ResponseStatusException.class, () -> controller.acceptRequest(id, other));
        assertThrows(ResponseStatusException.class, () -> controller.declineRequest(id, other));
        assertThrows(ResponseStatusException.class, () -> controller.withdrawRequest(id, other));
        var error = assertThrows(ResponseStatusException.class, () -> controller.getPartnerContact(id, other));
        assertEquals("VERIFICATION_REQUIRED", error.getBody().getProperties().get("code"));
        verifyNoInteractions(requests);
    }
}

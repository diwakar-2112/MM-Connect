package com.MM_CONNECT.api.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthIdentityTests {
    @Test
    void unverifiedEmailCannotClaimAnExistingAccount() throws Exception {
        var users = mock(UserRepository.class);
        var jwt = mock(JwtService.class);
        var firebase = mock(FirebaseAuth.class);
        var token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("incoming-uid");
        when(token.getClaims()).thenReturn(Map.of("email", "member@example.test", "email_verified", false));
        when(firebase.verifyIdToken("test-token")).thenReturn(token);
        try (var firebaseStatic = mockStatic(FirebaseAuth.class)) {
            firebaseStatic.when(FirebaseAuth::getInstance).thenReturn(firebase);
            assertThrows(ResponseStatusException.class,
                    () -> new AppAuthService(users, jwt).processFirebaseLogin("test-token"));
        }
        verify(users, never()).findByEmail(any());
        verifyNoInteractions(jwt);
    }

    @Test
    void staffIdentityMustAlreadyBeBoundToTheTrustedFirebaseUid() throws Exception {
        var users = mock(UserRepository.class);
        var jwt = mock(JwtService.class);
        var firebase = mock(FirebaseAuth.class);
        var token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("incoming-uid");
        when(token.getClaims()).thenReturn(Map.of("email", "staff@example.test", "email_verified", true));
        when(firebase.verifyIdToken("test-token")).thenReturn(token);
        User staff = new User(); staff.setId(UUID.randomUUID()); staff.setRole("ROLE_ADMIN");
        when(users.findByEmail("staff@example.test")).thenReturn(Optional.of(staff));
        try (var firebaseStatic = mockStatic(FirebaseAuth.class)) {
            firebaseStatic.when(FirebaseAuth::getInstance).thenReturn(firebase);
            var service = new AppAuthService(users, jwt);
            assertThrows(ResponseStatusException.class, () -> service.processFirebaseLogin("test-token"));
            staff.setFirebaseUid("other-uid");
            assertThrows(ResponseStatusException.class, () -> service.processFirebaseLogin("test-token"));
            staff.setFirebaseUid("incoming-uid");
            service.processFirebaseLogin("test-token");
        }
        verify(jwt, times(1)).generateAccessToken(staff);
    }

    @Test
    void verifiedLegacyMemberBindsOnceAndSubsequentLoginUsesStableUid() throws Exception {
        var users = mock(UserRepository.class);
        var jwt = mock(JwtService.class);
        var firebase = mock(FirebaseAuth.class);
        var token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("member-uid");
        when(token.getClaims()).thenReturn(Map.of("email", "member@example.test", "email_verified", true));
        when(firebase.verifyIdToken("test-token")).thenReturn(token);
        User member = new User(); member.setId(UUID.randomUUID());
        when(users.findByEmail("member@example.test")).thenReturn(Optional.of(member));
        try (var firebaseStatic = mockStatic(FirebaseAuth.class)) {
            firebaseStatic.when(FirebaseAuth::getInstance).thenReturn(firebase);
            var service = new AppAuthService(users, jwt);
            service.processFirebaseLogin("test-token");
            assertEquals("member-uid", member.getFirebaseUid());
            when(users.findByFirebaseUid("member-uid")).thenReturn(Optional.of(member));
            when(token.getClaims()).thenReturn(Map.of("email", "changed@example.test", "email_verified", true));
            service.processFirebaseLogin("test-token");
            verify(users, never()).findByEmail("changed@example.test");
        }
    }
}

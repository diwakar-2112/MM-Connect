package com.MM_CONNECT.api.admin.auth;

import com.MM_CONNECT.api.admin.*;
import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StaffAuthServiceTests {
    private static final String CURRENT = "TEST_ONLY_current_password";
    private static final String NEXT = "TEST_ONLY_next_password";
    private StaffAccountRepository accounts;
    private UserRepository users;
    private StaffBootstrapRepository bootstrap;
    private AuditEventRepository audit;
    private PasswordEncoder encoder;
    private StaffTokenService tokens;
    private StaffAuthService service;
    private User user;
    private StaffAccount staff;
    private Clock clock;
    private Instant now;

    @BeforeEach void setup() {
        accounts = mock(StaffAccountRepository.class); users = mock(UserRepository.class);
        bootstrap = mock(StaffBootstrapRepository.class); audit = mock(AuditEventRepository.class);
        encoder = mock(PasswordEncoder.class); clock = mock(Clock.class);
        now = Instant.now(); when(clock.instant()).thenReturn(now);
        when(encoder.encode(anyString())).thenReturn("TEST_ONLY_encoded_hash");
        when(encoder.matches(CURRENT, "TEST_ONLY_existing_hash")).thenReturn(true);
        tokens = new StaffTokenService("TEST_ONLY_staff_signing_key", 15, clock);
        user = new User(); user.setId(UUID.randomUUID()); user.setRole("ROLE_ADMIN");
        staff = new StaffAccount(user.getId(), "test.admin", "TEST_ONLY_existing_hash", now);
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(accounts.findById(user.getId())).thenReturn(Optional.of(staff));
        when(accounts.lockByUserId(user.getId())).thenReturn(Optional.of(staff));
        when(accounts.lockByUsername("test.admin")).thenReturn(Optional.of(staff));
        when(bootstrap.findById((short) 1)).thenReturn(Optional.of(new StaffBootstrapState()));
        when(users.saveAndFlush(any())).thenAnswer(invocation -> {
            User created = invocation.getArgument(0); created.setId(UUID.randomUUID()); return created;
        });
        service = new StaffAuthService(accounts, users, new StaffAccessService(users), audit, bootstrap, encoder, tokens, clock);
        clearInvocations(encoder);
    }
    private void superadmin() { user.setRole("ROLE_SUPERADMIN"); staff.setPasswordChangeRequired(false); }
    private StaffAuthDtos.Login login(String password) { return new StaffAuthDtos.Login(" TEST.ADMIN ", password); }
    private void status(int status, Runnable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action::run).getStatusCode().value());
    }

    @Test void initialSessionIsRestrictedAndUsernameIsCanonicalized() {
        var result = service.login(login(CURRENT));
        assertTrue(result.passwordChangeRequired()); assertTrue(result.staff().permissions().isEmpty());
        assertEquals("test.admin", result.staff().username()); assertEquals(900, result.expiresIn());
        assertEquals("staff_access", tokens.verify(result.accessToken()).getClaim("token_type").asString());
        verify(accounts).lockByUsername("test.admin");
    }
    @Test void wrongPasswordKeepsCredentialsAndCountsTheAttempt() {
        status(401, () -> service.login(login("wrong")));
        assertEquals(1, staff.getFailedAttempts()); assertEquals("TEST_ONLY_existing_hash", staff.getPasswordHash());
        verifyNoInteractions(audit); verify(encoder, never()).encode(anyString());
    }
    @Test void nonexistentUsernameStillPaysThePasswordVerificationCost() {
        status(401, () -> service.login(new StaffAuthDtos.Login("missing.admin", "wrong")));
        verify(encoder).matches("wrong", "TEST_ONLY_encoded_hash");
        verifyNoInteractions(audit);
    }
    @Test void fiveFailuresBlockEvenTheCorrectPasswordUntilTheLockExpires() {
        for (int i = 0; i < 5; i++) status(401, () -> service.login(login("wrong")));
        assertEquals(now.plusSeconds(900), staff.getLockedUntil());
        status(401, () -> service.login(login(CURRENT)));
        assertEquals(5, staff.getFailedAttempts());
        when(clock.instant()).thenReturn(now.plusSeconds(901));
        assertNotNull(service.login(login(CURRENT)).accessToken());
        assertEquals(0, staff.getFailedAttempts()); assertNull(staff.getLockedUntil());
    }
    @Test void inactiveDeletedAndNonstaffIdentitiesCannotSignIn() {
        user.setActive(false); status(401, () -> service.login(login(CURRENT)));
        user.setActive(true); user.setDeletedAt(now); status(401, () -> service.login(login(CURRENT)));
        user.setDeletedAt(null); user.setRole("ROLE_USER"); status(401, () -> service.login(login(CURRENT)));
    }
    @Test void ownPasswordChangeRemovesRestrictionAndRotatesTheTokenVersion() {
        var result = service.changePassword(user.getId(), new StaffAuthDtos.ChangePassword(CURRENT, NEXT));
        assertFalse(staff.isPasswordChangeRequired()); assertFalse(result.passwordChangeRequired());
        assertEquals(1, staff.getAuthVersion()); assertEquals("TEST_ONLY_encoded_hash", staff.getPasswordHash());
        assertNotEquals(NEXT, staff.getPasswordHash());
        assertEquals(1L, tokens.verify(result.accessToken()).getClaim("auth_version").asLong());
        assertTrue(result.staff().permissions().contains("VERIFICATION_DECIDE"));
        verify(audit).save(any(AuditEvent.class));
    }
    @Test void incorrectCurrentPasswordCannotChangeCredentials() {
        status(401, () -> service.changePassword(user.getId(), new StaffAuthDtos.ChangePassword("wrong", NEXT)));
        assertEquals(0, staff.getAuthVersion()); assertTrue(staff.isPasswordChangeRequired());
        verify(encoder, never()).encode(anyString());
    }
    @Test void shortBlankAndUnchangedNewPasswordsAreRejected() {
        for (String value : new String[] {"short", "                ", CURRENT}) {
            status(400, () -> service.changePassword(user.getId(), new StaffAuthDtos.ChangePassword(CURRENT, value)));
        }
        assertEquals(0, staff.getAuthVersion()); verifyNoInteractions(audit);
    }
    @Test void logoutRevokesAllExistingStaffTokenVersions() {
        service.logout(user.getId()); assertEquals(1, staff.getAuthVersion());
        verify(audit).save(any(AuditEvent.class));
    }
    @Test void superadminCreatesOnlyAnAdminWithIndependentIdentityAndHashedTemporaryPassword() {
        superadmin();
        var result = service.createAdmin(user.getId(), new StaffAuthDtos.CreateAdmin("NEW.Admin", NEXT));
        var created = ArgumentCaptor.forClass(User.class); verify(users).saveAndFlush(created.capture());
        assertEquals("ROLE_ADMIN", created.getValue().getRole());
        assertNull(created.getValue().getFirebaseUid()); assertNull(created.getValue().getPhoneNumber());
        assertNull(created.getValue().getEmail()); assertEquals("new.admin", result.username());
        var credential = ArgumentCaptor.forClass(StaffAccount.class); verify(accounts).saveAndFlush(credential.capture());
        assertTrue(credential.getValue().isPasswordChangeRequired());
        assertEquals("TEST_ONLY_encoded_hash", credential.getValue().getPasswordHash());
    }
    @Test void anAdminCannotCreateListOrResetOtherStaffEvenThroughTheService() {
        status(403, () -> service.createAdmin(user.getId(), new StaffAuthDtos.CreateAdmin("new.admin", NEXT)));
        status(403, () -> service.listStaff(user.getId(), 0, 20));
        status(403, () -> service.resetPassword(user.getId(), UUID.randomUUID(), new StaffAuthDtos.ResetPassword(NEXT)));
        verify(users, never()).saveAndFlush(any());
    }
    @Test void restrictedSuperadminCannotProvisionAccountsBeforeChangingPassword() {
        user.setRole("ROLE_SUPERADMIN");
        status(403, () -> service.createAdmin(user.getId(), new StaffAuthDtos.CreateAdmin("new.admin", NEXT)));
        verify(users, never()).saveAndFlush(any());
    }
    @Test void duplicateUsernamesNeverCreateAnExtraUser() {
        superadmin(); when(accounts.existsByUsername("new.admin")).thenReturn(true);
        status(409, () -> service.createAdmin(user.getId(), new StaffAuthDtos.CreateAdmin("new.admin", NEXT)));
        verify(users, never()).saveAndFlush(any());
    }
    @Test void resetRotatesAdminCredentialsRevokesSessionsAndRequiresANewPassword() {
        superadmin(); User target = new User(); target.setId(UUID.randomUUID()); target.setRole("ROLE_ADMIN");
        StaffAccount targetStaff = new StaffAccount(target.getId(), "other.admin", "TEST_ONLY_other_hash", now);
        targetStaff.setPasswordChangeRequired(false); targetStaff.setFailedAttempts(4);
        when(users.findById(target.getId())).thenReturn(Optional.of(target));
        when(accounts.lockByUserId(target.getId())).thenReturn(Optional.of(targetStaff));
        var result = service.resetPassword(user.getId(), target.getId(), new StaffAuthDtos.ResetPassword(NEXT));
        assertTrue(result.passwordChangeRequired()); assertEquals(1, targetStaff.getAuthVersion());
        assertEquals(0, targetStaff.getFailedAttempts()); assertEquals("TEST_ONLY_encoded_hash", targetStaff.getPasswordHash());
    }
    @Test void superadminCannotUseTheAdminResetEndpointOnSelfOrAnotherSuperadmin() {
        superadmin();
        status(403, () -> service.resetPassword(user.getId(), user.getId(), new StaffAuthDtos.ResetPassword(NEXT)));
        User other = new User(); other.setId(UUID.randomUUID()); other.setRole("ROLE_SUPERADMIN");
        when(users.findById(other.getId())).thenReturn(Optional.of(other));
        when(accounts.lockByUserId(other.getId())).thenReturn(Optional.of(
                new StaffAccount(other.getId(), "other.superadmin", "TEST_ONLY_hash", now)));
        status(403, () -> service.resetPassword(user.getId(), other.getId(), new StaffAuthDtos.ResetPassword(NEXT)));
        assertEquals(0, staff.getAuthVersion()); verifyNoInteractions(audit);
    }
    @Test void existingSuperadminMakesBootstrapIdempotentEvenWithoutRetainingSeedCredentials() {
        when(accounts.countSuperadmins()).thenReturn(1L);
        service.bootstrapSuperadmin("", "");
        verify(bootstrap).findById((short) 1); verify(users, never()).saveAndFlush(any());
        verifyNoInteractions(encoder, audit);
    }
    @Test void initialBootstrapCreatesARestrictedIndependentSuperadmin() {
        service.bootstrapSuperadmin("FIRST.Superadmin", NEXT);
        var created = ArgumentCaptor.forClass(User.class); verify(users).saveAndFlush(created.capture());
        assertEquals("ROLE_SUPERADMIN", created.getValue().getRole()); assertNull(created.getValue().getFirebaseUid());
        var credential = ArgumentCaptor.forClass(StaffAccount.class); verify(accounts).saveAndFlush(credential.capture());
        assertTrue(credential.getValue().isPasswordChangeRequired()); assertEquals("first.superadmin", credential.getValue().getUsername());
    }
    @Test void bootstrapFailsWithoutMigrationMetadataInsteadOfSeedingWithoutALock() {
        when(bootstrap.findById((short) 1)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> service.bootstrapSuperadmin("first.admin", NEXT));
        verify(users, never()).saveAndFlush(any());
    }
    @Test void credentialRequestAndResponseToStringNeverIncludeSecrets() {
        assertFalse(new StaffAuthDtos.Login("admin", CURRENT).toString().contains(CURRENT));
        assertFalse(new StaffAuthDtos.ChangePassword(CURRENT, NEXT).toString().contains(NEXT));
        assertFalse(new StaffAuthDtos.CreateAdmin("admin", NEXT).toString().contains(NEXT));
        assertFalse(new StaffAuthDtos.ResetPassword(NEXT).toString().contains(NEXT));
        assertFalse(service.login(login(CURRENT)).toString().contains("eyJ"));
    }
}

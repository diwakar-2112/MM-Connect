package com.MM_CONNECT.api.auth;

import com.MM_CONNECT.api.admin.AdminVerificationController;
import com.MM_CONNECT.api.admin.AdminVerificationService;
import com.MM_CONNECT.api.admin.StaffAccessService;
import com.MM_CONNECT.api.admin.auth.*;
import com.MM_CONNECT.api.auth.dto.TokenResponse;
import com.MM_CONNECT.api.common.GlobalExceptionHandler;
import com.MM_CONNECT.api.config.SecurityRuleConfig;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.Instant;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class AdminSecurityTests {
    private AnnotationConfigWebApplicationContext context;
    private User user;
    private JwtService jwt;
    private MockMvc mvc;
    private StaffAccount staff;

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    @Import({SecurityRuleConfig.class, StaffSecurityConfig.class, GlobalExceptionHandler.class, StaffBootstrapRunner.class})
    static class TestConfig {
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean StaffAccountRepository staffAccounts() { return mock(StaffAccountRepository.class); }
        @Bean StaffAuthService staffAuth() { return mock(StaffAuthService.class); }
        @Bean StaffAuthController staffController(StaffAuthService auth) { return new StaffAuthController(auth); }
        @Bean AppAuthService appAuth() { return mock(AppAuthService.class); }
        @Bean AuthController authController(AppAuthService auth) { return new AuthController(auth); }
        @Bean MemberProbe probe() { return new MemberProbe(); }
        @Bean JwtService jwt() {
            var jwt = new JwtService();
            ReflectionTestUtils.setField(jwt, "secretKey", "TEST_ONLY_signing_key_for_isolated_security_tests");
            ReflectionTestUtils.setField(jwt, "accessTokenExpiration", 15L);
            ReflectionTestUtils.setField(jwt, "refreshTokenExpirationDays", 30L);
            return jwt;
        }
        @Bean JwtAuthFilter filter(JwtService jwt, UserRepository users) { return new JwtAuthFilter(jwt, users); }
        @Bean StaffAccessService staff(UserRepository users) { return new StaffAccessService(users); }
        @Bean AdminVerificationController controller(StaffAccessService staff) {
            return new AdminVerificationController(mock(AdminVerificationService.class), staff);
        }
    }

    @org.springframework.web.bind.annotation.RestController
    static class MemberProbe {
        @org.springframework.web.bind.annotation.GetMapping("/v1/test/member")
        String read() { return "member"; }
    }

    @BeforeEach
    void setup() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                "security.jwt.secret-key=TEST_ONLY_signing_key_for_isolated_security_tests",
                "security.jwt.access-token-expiration-minutes=15",
                "security.jwt.refresh-token-expiration-days=30");
        context.register(TestConfig.class);
        context.refresh();
        jwt = context.getBean(JwtService.class);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setFirebaseUid("test-firebase-uid");
        when(context.getBean(UserRepository.class).findById(user.getId())).thenReturn(Optional.of(user));
        staff = new StaffAccount(user.getId(), "test.admin", "TEST_ONLY_unused_hash", Instant.now());
        staff.setPasswordChangeRequired(false);
        when(context.getBean(StaffAccountRepository.class).findById(user.getId())).thenReturn(Optional.of(staff));
        when(context.getBean(StaffAuthService.class).me(user.getId())).thenAnswer(invocation ->
                new StaffAuthDtos.Me(user.getId(), staff.getUsername(), user.getRole(),
                        staff.isPasswordChangeRequired(), List.of()));
        mvc = webAppContextSetup(context).addFilters((Filter) context.getBean("springSecurityFilterChain")).build();
    }

    @AfterEach
    void close() { if (context != null) context.close(); }

    @Test
    void missingTokenAndMemberTokensCannotAuthenticateStaff() throws Exception {
        mvc.perform(get("/v1/admin/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v1/admin/me").header("Authorization", "Bearer " + jwt.generateAccessToken(user)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminAndSuperadminCanReadStaffSessionWithoutMemberOnboarding() throws Exception {
        for (String role : new String[] {"ROLE_ADMIN", "ROLE_SUPERADMIN"}) {
            user.setRole(role);
            mvc.perform(get("/v1/admin/me").header("Authorization", staffAuthorization()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.role").value(role))
                    .andExpect(header().string("Cache-Control", "no-store"));
        }
    }

    @Test
    void demotionAndSuspensionTakeEffectWithAnAlreadyIssuedToken() throws Exception {
        user.setRole("ROLE_ADMIN");
        String token = staffAuthorization();
        user.setRole("ROLE_USER");
        mvc.perform(get("/v1/admin/me").header("Authorization", token))
                .andExpect(status().isUnauthorized());
        user.setRole("ROLE_ADMIN");
        user.setActive(false);
        mvc.perform(get("/v1/admin/me").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void firebaseTokensNeverBecomeStaffSessionsAndStaffNeedNoFirebaseIdentity() throws Exception {
        String memberToken = jwt.generateAccessToken(user);
        user.setRole("ROLE_ADMIN");
        mvc.perform(get("/v1/admin/me").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isUnauthorized());
        user.setFirebaseUid(null);
        mvc.perform(get("/v1/admin/me").header("Authorization", "Bearer " + jwt.generateAccessToken(user)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/v1/admin/me").header("Authorization", staffAuthorization()))
                .andExpect(status().isOk());
    }

    @Test
    void adminDecisionRequiresAnInspectedNonnegativeVersionAndCorrectionFeedback() throws Exception {
        user.setRole("ROLE_ADMIN");
        String authorization = staffAuthorization();
        String prefix = "/v1/admin/verifications/" + UUID.randomUUID();
        mvc.perform(post(prefix + "/approve").header("Authorization", authorization)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(prefix + "/approve").header("Authorization", authorization)
                        .contentType("application/json").content("{\"expectedVersion\":-1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(prefix + "/request-changes").header("Authorization", authorization)
                        .contentType("application/json").content("{\"expectedVersion\":0,\"feedback\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshTokensAreNeverAcceptedAsAccessTokens() throws Exception {
        user.setRole("ROLE_SUPERADMIN");
        mvc.perform(get("/v1/admin/me").header("Authorization", "Bearer " + jwt.generateRefreshToken(user)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffTokensCannotAuthenticateMemberApis() throws Exception {
        user.setRole("ROLE_ADMIN");
        mvc.perform(get("/v1/test/member").header("Authorization", staffAuthorization()))
                .andExpect(status().isUnauthorized());
        user.setRole("ROLE_USER");
        mvc.perform(get("/v1/test/member").header("Authorization", "Bearer " + jwt.generateAccessToken(user)))
                .andExpect(status().isOk());
    }

    @Test
    void flutterSessionKeepsTheOriginalEndpointAndResponse() throws Exception {
        when(context.getBean(AppAuthService.class).processFirebaseLogin("TEST_ONLY_firebase_id_token"))
                .thenReturn(new TokenResponse("TEST_ONLY_member_access", "TEST_ONLY_member_refresh", true));
        mvc.perform(post("/v1/auth/session").contentType("application/json")
                        .content("{\"firebaseToken\":\"TEST_ONLY_firebase_id_token\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isNewUser").value(true))
                .andExpect(jsonPath("$.accessToken").value("TEST_ONLY_member_access"))
                .andExpect(jsonPath("$.refreshToken").value("TEST_ONLY_member_refresh"));
    }

    @Test
    void temporaryPasswordCannotBypassRequiredChangeThroughDirectRoutes() throws Exception {
        user.setRole("ROLE_SUPERADMIN"); staff.setPasswordChangeRequired(true);
        String authorization = staffAuthorization();
        mvc.perform(get("/v1/admin/auth/me").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passwordChangeRequired").value(true));
        for (String path : List.of("/v1/admin/me", "/v1/admin/verifications", "/v1/admin/staff")) {
            mvc.perform(get(path).header("Authorization", authorization))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
        }
        mvc.perform(post("/v1/admin/verifications/" + UUID.randomUUID() + "/approve")
                        .header("Authorization", authorization).contentType("application/json").content("{\"expectedVersion\":0}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/v1/admin/auth/logout").header("Authorization", authorization))
                .andExpect(status().isNoContent());
        verify(context.getBean(StaffAuthService.class)).logout(user.getId());
    }

    @Test
    void temporaryPasswordMayReachTheValidatedPasswordChangeEndpoint() throws Exception {
        user.setRole("ROLE_ADMIN"); staff.setPasswordChangeRequired(true);
        mvc.perform(post("/v1/admin/auth/change-password").header("Authorization", staffAuthorization())
                        .contentType("application/json").content("{\"currentPassword\":\"TEST_ONLY_current\",\"newPassword\":\"TEST_ONLY_next_password\"}"))
                .andExpect(status().isOk());
        verify(context.getBean(StaffAuthService.class)).changePassword(eq(user.getId()), any());
    }

    @Test
    void adminCannotCreateListOrResetStaff() throws Exception {
        user.setRole("ROLE_ADMIN");
        String authorization = staffAuthorization();
        mvc.perform(get("/v1/admin/staff").header("Authorization", authorization)).andExpect(status().isForbidden());
        mvc.perform(post("/v1/admin/staff").header("Authorization", authorization)
                        .contentType("application/json").content("{\"username\":\"other.admin\",\"temporaryPassword\":\"TEST_ONLY_temporary\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/v1/admin/staff/" + UUID.randomUUID() + "/reset-password")
                        .header("Authorization", authorization).contentType("application/json")
                        .content("{\"temporaryPassword\":\"TEST_ONLY_temporary\"}"))
                .andExpect(status().isForbidden());
        verify(context.getBean(StaffAuthService.class), never()).createAdmin(any(), any());
    }

    @Test
    void revokedDeletedAndRoleChangedStaffTokensStopWorking() throws Exception {
        user.setRole("ROLE_ADMIN"); String old = staffAuthorization();
        staff.revokeTokens();
        mvc.perform(get("/v1/admin/auth/me").header("Authorization", old)).andExpect(status().isUnauthorized());
        String fresh = staffAuthorization(); user.setRole("ROLE_SUPERADMIN");
        mvc.perform(get("/v1/admin/auth/me").header("Authorization", fresh)).andExpect(status().isUnauthorized());
        fresh = staffAuthorization(); user.setDeletedAt(Instant.now());
        mvc.perform(get("/v1/admin/auth/me").header("Authorization", fresh)).andExpect(status().isUnauthorized());
    }

    @Test
    void staffSessionIsPublicButCannotBeCreatedUsingTheWrongBody() throws Exception {
        mvc.perform(post("/v1/admin/auth/session").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/v1/admin/auth/session").contentType("application/json")
                        .content("{\"username\":\"test.admin\",\"password\":\"TEST_ONLY_password\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        verify(context.getBean(StaffAuthService.class)).login(any());
    }

    @Test
    void bootstrapIsOffAndMemberFilterIsNotRegisteredOutsideItsChain() {
        org.junit.jupiter.api.Assertions.assertTrue(context.getBeansOfType(StaffBootstrapRunner.class).isEmpty());
        var registration = (org.springframework.boot.web.servlet.FilterRegistrationBean<?>) context.getBean("memberJwtRegistration");
        org.junit.jupiter.api.Assertions.assertFalse(registration.isEnabled());
    }

    @Test
    void changingTemporaryPasswordThroughHttpUnlocksReviewAndRejectsTheOldTokenThenLogoutRevokesTheNewOne() throws Exception {
        user.setRole("ROLE_ADMIN"); staff.setPasswordChangeRequired(true);
        var encoder = context.getBean(org.springframework.security.crypto.password.PasswordEncoder.class);
        staff.setPasswordHash(encoder.encode("TEST_ONLY_current_password"));
        var repository = context.getBean(StaffAccountRepository.class);
        when(repository.lockByUserId(user.getId())).thenReturn(Optional.of(staff));
        var actual = new StaffAuthService(repository, context.getBean(UserRepository.class),
                context.getBean(StaffAccessService.class), mock(com.MM_CONNECT.api.admin.AuditEventRepository.class),
                mock(StaffBootstrapRepository.class), encoder, context.getBean(StaffTokenService.class), java.time.Clock.systemUTC());
        var auth = context.getBean(StaffAuthService.class);
        when(auth.changePassword(eq(user.getId()), any())).thenAnswer(invocation -> actual.changePassword(
                invocation.getArgument(0), invocation.getArgument(1)));
        doAnswer(invocation -> { actual.logout(invocation.getArgument(0)); return null; }).when(auth).logout(user.getId());
        String old = staffAuthorization();
        mvc.perform(post("/v1/admin/auth/change-password").header("Authorization", old).contentType("application/json")
                        .content("{\"currentPassword\":\"TEST_ONLY_current_password\",\"newPassword\":\"TEST_ONLY_new_password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passwordChangeRequired").value(false));
        mvc.perform(get("/v1/admin/me").header("Authorization", old)).andExpect(status().isUnauthorized());
        String fresh = staffAuthorization();
        mvc.perform(get("/v1/admin/me").header("Authorization", fresh)).andExpect(status().isOk());
        mvc.perform(post("/v1/admin/auth/logout").header("Authorization", fresh)).andExpect(status().isNoContent());
        mvc.perform(get("/v1/admin/me").header("Authorization", fresh)).andExpect(status().isUnauthorized());
    }

    private String staffAuthorization() {
        return "Bearer " + context.getBean(StaffTokenService.class).issue(user, staff);
    }
}

package com.MM_CONNECT.api.admin.auth;

import com.MM_CONNECT.api.admin.AuditEvent;
import com.MM_CONNECT.api.admin.AuditEventRepository;
import com.MM_CONNECT.api.admin.StaffAccessService;
import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class StaffAuthService {
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_TIME = Duration.ofMinutes(15);
    private final StaffAccountRepository accounts;
    private final UserRepository users;
    private final StaffAccessService access;
    private final AuditEventRepository audit;
    private final StaffBootstrapRepository bootstrap;
    private final PasswordEncoder passwords;
    private final StaffTokenService tokens;
    private final Clock clock;
    private final String dummyHash;

    public StaffAuthService(StaffAccountRepository accounts, UserRepository users, StaffAccessService access,
                            AuditEventRepository audit, StaffBootstrapRepository bootstrap,
                            PasswordEncoder passwords, StaffTokenService tokens, Clock staffClock) {
        this.accounts = accounts; this.users = users; this.access = access;
        this.audit = audit; this.bootstrap = bootstrap; this.passwords = passwords;
        this.tokens = tokens; this.clock = staffClock;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor = StaffAuthFailure.class)
    public StaffAuthDtos.Session login(StaffAuthDtos.Login request) {
        var staff = accounts.lockByUsername(normalize(request.username())).orElse(null);
        boolean matches = passwords.matches(request.password(), staff == null ? dummyHash : staff.getPasswordHash());
        if (staff == null) throw new StaffAuthFailure();
        Instant now = clock.instant();
        if (locked(staff, now)) throw new StaffAuthFailure();
        clearExpiredLock(staff, now);
        User user = users.findById(staff.getUserId()).orElse(null);
        if (!matches || user == null || !user.isActive() || user.getDeletedAt() != null
                || !StaffAccessService.isStaffRole(user.getRole())) {
            failedAttempt(staff, now);
            throw new StaffAuthFailure();
        }
        staff.setFailedAttempts(0); staff.setLockedUntil(null); staff.touch(now);
        audit.save(new AuditEvent(user.getId(), user.getId(), null, "STAFF_LOGIN"));
        return session(user, staff);
    }

    @Transactional(readOnly = true)
    public StaffAuthDtos.Me me(UUID actorId) {
        User user = access.requireReviewer(actorId);
        return info(user, account(actorId));
    }

    @Transactional(noRollbackFor = StaffAuthFailure.class)
    public StaffAuthDtos.Session changePassword(UUID actorId, StaffAuthDtos.ChangePassword request) {
        User user = access.requireReviewer(actorId);
        StaffAccount staff = lockedAccount(actorId);
        Instant now = clock.instant();
        if (locked(staff, now)) throw new StaffAuthFailure();
        clearExpiredLock(staff, now);
        if (!passwords.matches(request.currentPassword(), staff.getPasswordHash())) {
            failedAttempt(staff, now);
            throw new StaffAuthFailure();
        }
        validatePassword(request.newPassword());
        if (request.currentPassword().equals(request.newPassword())) {
            throw error(HttpStatus.BAD_REQUEST, "Choose a different password.", "PASSWORD_UNCHANGED");
        }
        staff.setPasswordHash(passwords.encode(request.newPassword()));
        staff.setPasswordChangeRequired(false);
        staff.revokeTokens(); staff.setFailedAttempts(0); staff.setLockedUntil(null); staff.touch(now);
        audit.save(new AuditEvent(actorId, actorId, null, "STAFF_PASSWORD_CHANGED"));
        return session(user, staff);
    }

    @Transactional
    public void logout(UUID actorId) {
        access.requireReviewer(actorId);
        StaffAccount staff = lockedAccount(actorId);
        staff.revokeTokens(); staff.touch(clock.instant());
        audit.save(new AuditEvent(actorId, actorId, null, "STAFF_LOGOUT"));
    }

    @Transactional
    public StaffAuthDtos.StaffMember createAdmin(UUID actorId, StaffAuthDtos.CreateAdmin request) {
        fullSuperadmin(actorId);
        return create(normalize(request.username()), request.temporaryPassword(), "ROLE_ADMIN", actorId);
    }

    @Transactional(readOnly = true)
    public StaffAuthDtos.StaffPage listStaff(UUID actorId, int page, int size) {
        fullSuperadmin(actorId);
        if (page < 0 || size < 1 || size > 100) throw error(HttpStatus.BAD_REQUEST,
                "Use page >= 0 and size between 1 and 100.", "INVALID_PAGE");
        var result = accounts.findAll(PageRequest.of(page, size, Sort.by("username")));
        var content = result.getContent().stream().map(staff -> member(
                users.findById(staff.getUserId()).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
                        "Staff account unavailable.", "STAFF_NOT_FOUND")), staff)).toList();
        return new StaffAuthDtos.StaffPage(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    @Transactional
    public StaffAuthDtos.StaffMember resetPassword(UUID actorId, UUID targetId, StaffAuthDtos.ResetPassword request) {
        fullSuperadmin(actorId);
        StaffAccount staff = lockedAccount(targetId);
        User target = users.findById(targetId).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
                "Staff account unavailable.", "STAFF_NOT_FOUND"));
        if (actorId.equals(targetId) || !"ROLE_ADMIN".equals(target.getRole())) {
            throw error(HttpStatus.FORBIDDEN, "Only an Admin account can be reset here.", "STAFF_PERMISSION_REQUIRED");
        }
        if (!target.isActive() || target.getDeletedAt() != null) throw error(HttpStatus.NOT_FOUND,
                "Staff account unavailable.", "STAFF_NOT_FOUND");
        validatePassword(request.temporaryPassword());
        if (passwords.matches(request.temporaryPassword(), staff.getPasswordHash())) {
            throw error(HttpStatus.BAD_REQUEST, "Choose a different temporary password.", "PASSWORD_UNCHANGED");
        }
        staff.setPasswordHash(passwords.encode(request.temporaryPassword()));
        staff.setPasswordChangeRequired(true); staff.revokeTokens(); staff.touch(clock.instant());
        staff.setFailedAttempts(0); staff.setLockedUntil(null);
        audit.save(new AuditEvent(actorId, targetId, null, "STAFF_PASSWORD_RESET"));
        return member(target, staff);
    }

    @Transactional
    public void bootstrapSuperadmin(String username, String password) {
        bootstrap.findById((short) 1).orElseThrow(() -> new IllegalStateException("Staff bootstrap metadata missing; apply V8."));
        // Check even inactive/deleted accounts: restart must never replace or recover an existing Superadmin.
        if (accounts.countSuperadmins() > 0) return;
        create(normalize(username), password, "ROLE_SUPERADMIN", null);
    }

    private StaffAuthDtos.StaffMember create(String username, String password, String role, UUID actorId) {
        if (username == null || !username.matches("[a-z0-9][a-z0-9._-]{2,63}")) {
            throw error(HttpStatus.BAD_REQUEST, "Username must be 3-64 letters, digits, dots, underscores or hyphens.", "INVALID_STAFF_USERNAME");
        }
        validatePassword(password);
        if (accounts.existsByUsername(username)) throw usernameTaken();
        User user = new User(); user.setRole(role);
        user = users.saveAndFlush(user);
        StaffAccount staff = new StaffAccount(user.getId(), username, passwords.encode(password), clock.instant());
        try { accounts.saveAndFlush(staff); }
        catch (DataIntegrityViolationException ex) { throw usernameTaken(); }
        audit.save(new AuditEvent(actorId == null ? user.getId() : actorId, user.getId(), null,
                actorId == null ? "STAFF_SUPERADMIN_BOOTSTRAPPED" : "STAFF_ADMIN_CREATED"));
        return member(user, staff);
    }

    private void fullSuperadmin(UUID actorId) {
        access.requireSuperadmin(actorId);
        if (account(actorId).isPasswordChangeRequired()) throw error(HttpStatus.FORBIDDEN,
                "Change your temporary password before continuing.", "PASSWORD_CHANGE_REQUIRED");
    }
    private StaffAccount account(UUID id) {
        return accounts.findById(id).orElseThrow(() -> error(HttpStatus.UNAUTHORIZED,
                "A staff session is required.", "STAFF_SESSION_REQUIRED"));
    }
    private StaffAccount lockedAccount(UUID id) {
        return accounts.lockByUserId(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
                "Staff account unavailable.", "STAFF_NOT_FOUND"));
    }
    private boolean locked(StaffAccount staff, Instant now) {
        return staff.getLockedUntil() != null && now.isBefore(staff.getLockedUntil());
    }
    private void clearExpiredLock(StaffAccount staff, Instant now) {
        if (staff.getLockedUntil() != null && !locked(staff, now)) {
            staff.setFailedAttempts(0); staff.setLockedUntil(null);
        }
    }
    private void failedAttempt(StaffAccount staff, Instant now) {
        staff.setFailedAttempts(staff.getFailedAttempts() + 1);
        if (staff.getFailedAttempts() >= MAX_ATTEMPTS) staff.setLockedUntil(now.plus(LOCK_TIME));
        staff.touch(now);
    }
    private StaffAuthDtos.Session session(User user, StaffAccount staff) {
        return new StaffAuthDtos.Session(tokens.issue(user, staff), "Bearer", tokens.expiresIn(),
                staff.isPasswordChangeRequired(), info(user, staff));
    }
    private StaffAuthDtos.Me info(User user, StaffAccount staff) {
        List<String> permissions = staff.isPasswordChangeRequired() ? List.of()
                : "ROLE_SUPERADMIN".equals(user.getRole())
                ? List.of("VERIFICATION_READ", "VERIFICATION_DECIDE", "REPORTS_READ", "STAFF_MANAGE")
                : List.of("VERIFICATION_READ", "VERIFICATION_DECIDE", "REPORTS_READ");
        return new StaffAuthDtos.Me(user.getId(), staff.getUsername(), user.getRole(),
                staff.isPasswordChangeRequired(), permissions);
    }
    private StaffAuthDtos.StaffMember member(User user, StaffAccount staff) {
        return new StaffAuthDtos.StaffMember(user.getId(), staff.getUsername(), user.getRole(),
                user.isActive() && user.getDeletedAt() == null, staff.isPasswordChangeRequired(), staff.getCreatedAt());
    }
    private String normalize(String value) { return value == null ? null : value.strip().toLowerCase(Locale.ROOT); }
    private void validatePassword(String password) {
        if (password == null || password.isBlank() || password.length() < 15 || password.length() > 128) {
            throw error(HttpStatus.BAD_REQUEST, "Use a password between 15 and 128 characters.", "INVALID_STAFF_PASSWORD");
        }
    }
    private ResponseStatusException usernameTaken() {
        return error(HttpStatus.CONFLICT, "Staff username is unavailable.", "STAFF_USERNAME_TAKEN");
    }
    private ResponseStatusException error(HttpStatus status, String detail, String code) {
        var error = new ResponseStatusException(status, detail);
        error.getBody().setProperty("code", code);
        return error;
    }
}

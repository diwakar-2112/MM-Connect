package com.MM_CONNECT.api.admin.auth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin")
public class StaffAuthController {
    private final StaffAuthService auth;
    public StaffAuthController(StaffAuthService auth) { this.auth = auth; }
    @PostMapping("/auth/session")
    public ResponseEntity<StaffAuthDtos.Session> login(@Valid @RequestBody StaffAuthDtos.Login request) {
        return privateResponse(200, auth.login(request));
    }
    @GetMapping("/auth/me")
    public ResponseEntity<StaffAuthDtos.Me> me(@AuthenticationPrincipal UUID actorId) {
        return privateResponse(200, auth.me(actorId));
    }
    @PostMapping("/auth/change-password")
    public ResponseEntity<StaffAuthDtos.Session> changePassword(@AuthenticationPrincipal UUID actorId,
            @Valid @RequestBody StaffAuthDtos.ChangePassword request) {
        return privateResponse(200, auth.changePassword(actorId, request));
    }
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UUID actorId) {
        auth.logout(actorId);
        return privateResponse(204, null);
    }
    @PostMapping("/staff")
    public ResponseEntity<StaffAuthDtos.StaffMember> create(@AuthenticationPrincipal UUID actorId,
            @Valid @RequestBody StaffAuthDtos.CreateAdmin request) {
        return privateResponse(201, auth.createAdmin(actorId, request));
    }
    @GetMapping("/staff")
    public ResponseEntity<StaffAuthDtos.StaffPage> list(@AuthenticationPrincipal UUID actorId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return privateResponse(200, auth.listStaff(actorId, page, size));
    }
    @PostMapping("/staff/{userId}/reset-password")
    public ResponseEntity<StaffAuthDtos.StaffMember> reset(@AuthenticationPrincipal UUID actorId,
            @PathVariable UUID userId, @Valid @RequestBody StaffAuthDtos.ResetPassword request) {
        return privateResponse(200, auth.resetPassword(actorId, userId, request));
    }
    private <T> ResponseEntity<T> privateResponse(int status, T body) {
        return ResponseEntity.status(status).header("Cache-Control", "no-store").body(body);
    }
}

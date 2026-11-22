package com.MM_CONNECT.api.verification;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
public class MemberVerificationController {
    private final MemberVerificationService verification;
    public MemberVerificationController(MemberVerificationService verification) { this.verification = verification; }

    @GetMapping("/v1/profiles/me/verification")
    public ResponseEntity<VerificationDtos.MemberStatus> status(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(verification.status(userId));
    }
}

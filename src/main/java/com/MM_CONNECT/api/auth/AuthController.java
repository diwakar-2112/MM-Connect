package com.MM_CONNECT.api.auth;

import com.MM_CONNECT.api.auth.dto.SessionRequest;
import com.MM_CONNECT.api.auth.dto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AppAuthService authService;

    public AuthController(AppAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/session")
    public ResponseEntity<TokenResponse> createSession(@Valid @RequestBody SessionRequest request) {
        TokenResponse tokens = authService.processFirebaseLogin(request.firebaseToken());
        return ResponseEntity.ok(tokens);
    }
}
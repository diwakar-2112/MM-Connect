package com.MM_CONNECT.api.profile;

import com.MM_CONNECT.api.profile.dto.OnboardingRequest;
import com.MM_CONNECT.api.profile.dto.OnboardingResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/onboarding")
public class OnboardingController {

    private final OnboardingManagerService onboardingManagerService;

    public OnboardingController(OnboardingManagerService onboardingManagerService) {
        this.onboardingManagerService = onboardingManagerService;
    }

    @PostMapping("/complete")
    public ResponseEntity<OnboardingResponse> completeSetup(
            @AuthenticationPrincipal UUID userId, // Automatically injected from the secure JWT!
            @Valid @RequestBody OnboardingRequest payload) {

        OnboardingResponse result = onboardingManagerService.finalizeSetup(userId, payload);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
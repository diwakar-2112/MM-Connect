package com.MM_CONNECT.api.profile;

import com.MM_CONNECT.api.profile.dto.OnboardingRequest;
import com.MM_CONNECT.api.profile.dto.OnboardingResponse;
import com.MM_CONNECT.api.verification.MemberVerificationService;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class OnboardingManagerService {
    private final MemberVerificationService verification;

    public OnboardingManagerService(MemberVerificationService verification) {
        this.verification = verification;
    }

    public OnboardingResponse finalizeSetup(UUID userId, OnboardingRequest request) {
        return verification.submit(userId, request, true);
    }

    public OnboardingResponse updateExistingProfile(UUID userId, OnboardingRequest request) {
        return verification.submit(userId, request, false);
    }
}

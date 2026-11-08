package com.MM_CONNECT.api.profile.dto;

import java.util.UUID;

public record OnboardingResponse(
        String message,
        UUID profileId,
        String status,
        boolean discoveryFeedUnlocked
) {}
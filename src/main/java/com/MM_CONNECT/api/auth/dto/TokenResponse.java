package com.MM_CONNECT.api.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        boolean isNewUser
) {}
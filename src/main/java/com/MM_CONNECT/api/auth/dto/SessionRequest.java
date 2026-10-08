package com.MM_CONNECT.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SessionRequest(
        @NotBlank(message = "Firebase token is required")
        String firebaseToken
) {}
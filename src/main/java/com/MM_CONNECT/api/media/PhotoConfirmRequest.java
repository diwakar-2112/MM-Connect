package com.MM_CONNECT.api.media;

import jakarta.validation.constraints.NotBlank;

public record PhotoConfirmRequest(
        @NotBlank String fileKey,
        boolean isPrimary
) {}
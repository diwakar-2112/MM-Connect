package com.MM_CONNECT.api.matching;

import java.util.UUID;

public record DiscoveryCardDto(
        UUID userId,
        String fullName,
        Integer age,
        String currentCity,
        String profession,
        String religion,
        String mainPhotoUrl // The generated 60-minute view link!
) {}
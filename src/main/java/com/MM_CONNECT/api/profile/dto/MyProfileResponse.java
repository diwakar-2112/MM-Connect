package com.MM_CONNECT.api.profile.dto;

import com.MM_CONNECT.api.profile.PartnerPreference;
import com.MM_CONNECT.api.profile.Profile;

import java.util.List;

public record MyProfileResponse(
        Profile profileDetails,
        PartnerPreference partnerPreferences,
        List<PhotoViewDto> photos
) {
    public record PhotoViewDto(
            String photoUrl, // This will hold the 60-minute S3/MinIO link!
            boolean isPrimary
    ) {}
}
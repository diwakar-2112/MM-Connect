package com.MM_CONNECT.api.profile.dto;

import java.util.List;
import java.util.UUID;

// Explicit public view: never serialize User/Profile entities for other members.
public record ProfileViewResponse(
        ProfileDetails profileDetails,
        List<PhotoViewDto> photos
) {
    public record ProfileDetails(
            UUID userId,
            String profileCreatedFor,
            String fullName,
            String gender,
            int age,
            Integer heightCm,
            String maritalStatus,
            String motherTongue,
            String religion,
            String community,
            String subCommunity,
            boolean showCommunity,
            String paternalGotra,
            String maternalGotra,
            String sampradaya,
            String highestQualification,
            String fieldOfStudy,
            String professionDomain,
            String employmentType,
            String currentCity,
            String currentState,
            String nativePlace,
            String nativeDistrict,
            String workCity,
            String relocationPreference,
            String familyType,
            String fatherOccupation,
            String motherOccupation,
            String siblings,
            String foodPreference,
            String smoking,
            String alcohol,
            List<String> interests,
            String weekendPreference,
            boolean verified
    ) {}

    public record PhotoViewDto(
            UUID photoId,
            String photoUrl,
            boolean isPrimary
    ) {}
}

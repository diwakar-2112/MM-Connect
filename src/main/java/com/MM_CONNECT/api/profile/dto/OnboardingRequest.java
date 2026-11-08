package com.MM_CONNECT.api.profile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record OnboardingRequest(
        @NotBlank String profileCreatedFor, // MYSELF, SON, DAUGHTER, BROTHER, SISTER
        @Valid @NotNull BasicDetails basicDetails,
        @Valid @NotNull Background background,
        @Valid @NotNull EducationAndCareer educationAndCareer,
        @Valid @NotNull LocationAndFamily locationAndFamily,
        @Valid @NotNull Lifestyle lifestyle,
        @Valid @NotNull PartnerPreferencesDto partnerPreferences
) {
    public record BasicDetails(
            @NotBlank String fullName,
            @NotBlank String gender,
            @NotNull LocalDate dateOfBirth,
            Integer heightCm,
            @NotBlank String maritalStatus,
            @Size(max = 100) String motherTongue
    ) {}

    public record Background(
            String religion,
            String community,
            boolean showCommunityOnProfile,
            @Size(max = 100) String subCommunity,
            @NotBlank @Size(max = 100) String paternalGotra,
            @Size(max = 100) String maternalGotra,
            @Size(max = 100) String sampradaya
    ) {}

    public record EducationAndCareer(
            String highestQualification,
            String fieldOfStudy,
            String professionDomain,
            String employmentType,
            Long annualIncomeInr,
            @Size(max = 100) String workCity
    ) {}

    public record LocationAndFamily(
            @NotBlank String currentCity,
            String nativePlace,
            String familyType,
            String fatherOccupation,
            @Size(max = 100) String motherOccupation,
            @Size(max = 30) String siblings,
            @Size(max = 100) String currentState,
            @Size(max = 100) String nativeDistrict,
            @Size(max = 30) String relocationPreference
    ) {}

    public record Lifestyle(
            String foodPreference,
            String smoking,
            String alcohol,
            @Size(max = 5) List<String> interests,
            @Size(max = 100) String weekendPreference
    ) {}

    public record PartnerPreferencesDto(
            @NotNull Integer minAge,
            @NotNull Integer maxAge,
            List<String> preferredLocations,
            List<String> preferredEducation,
            List<@NotBlank String> preferredMaritalStatuses,
            Boolean excludePaternalGotra,
            Boolean excludeMaternalGotra,
            List<@NotBlank String> preferredSubCommunities
    ) {}
}

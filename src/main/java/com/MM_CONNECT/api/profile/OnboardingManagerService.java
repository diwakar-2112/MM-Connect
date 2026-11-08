package com.MM_CONNECT.api.profile;

import com.MM_CONNECT.api.matching.MatchProfileRepository;
import com.MM_CONNECT.api.profile.dto.OnboardingRequest;
import com.MM_CONNECT.api.profile.dto.OnboardingResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

import com.MM_CONNECT.api.matching.MatchProfile;
import java.time.LocalDate;
import java.time.Period;

@Service
public class OnboardingManagerService {

    private final ProfileRepository profileRepository;
    private final PartnerPreferenceRepository preferenceRepository;
    private final MatchProfileRepository matchProfileRepository;
    public OnboardingManagerService(ProfileRepository profileRepository, PartnerPreferenceRepository preferenceRepository,MatchProfileRepository matchProfileRepository) {
        this.profileRepository = profileRepository;
        this.preferenceRepository = preferenceRepository;
        this.matchProfileRepository = matchProfileRepository;
    }

    @Transactional // Ensures if one save fails, everything rolls back (no partial profiles)
    public OnboardingResponse finalizeSetup(UUID authenticatedUserId, OnboardingRequest request) {
        if (profileRepository.existsById(authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile already exists for this user.");
        }
        // 1. Build & Save the User's Profile
        Profile profile = new Profile();
        profile.setUserId(authenticatedUserId);
        profile.setProfileCreatedFor(request.profileCreatedFor());

        profile.setFullName(request.basicDetails().fullName());
        profile.setGender(request.basicDetails().gender());
        profile.setDateOfBirth(request.basicDetails().dateOfBirth());
        profile.setHeightCm(request.basicDetails().heightCm());
        profile.setMaritalStatus(request.basicDetails().maritalStatus());

        profile.setReligion(request.background().religion());
        profile.setCommunity(request.background().community());
        profile.setShowCommunity(request.background().showCommunityOnProfile());

        profile.setHighestQualification(request.educationAndCareer().highestQualification());
        profile.setFieldOfStudy(request.educationAndCareer().fieldOfStudy());
        profile.setProfessionDomain(request.educationAndCareer().professionDomain());
        profile.setEmploymentType(request.educationAndCareer().employmentType());
        profile.setAnnualIncome(request.educationAndCareer().annualIncomeInr());

        profile.setCurrentCity(request.locationAndFamily().currentCity());
        profile.setNativePlace(request.locationAndFamily().nativePlace());
        profile.setFamilyType(request.locationAndFamily().familyType());
        profile.setFatherOccupation(request.locationAndFamily().fatherOccupation());

        profile.setFoodPreference(request.lifestyle().foodPreference());
        profile.setSmoking(request.lifestyle().smoking());
        profile.setAlcohol(request.lifestyle().alcohol());
        profile.setInterests(request.lifestyle().interests());

        applyAdditionalProfileFields(profile, request);
        profile.setUpdatedAt(Instant.now());

        profileRepository.save(profile);

        // 2. Build & Save the User's Preferences
        PartnerPreference prefs = new PartnerPreference();
        prefs.setUserId(authenticatedUserId);
        prefs.setPrefAgeMin(request.partnerPreferences().minAge());
        prefs.setPrefAgeMax(request.partnerPreferences().maxAge());
        prefs.setPrefLocations(request.partnerPreferences().preferredLocations());
        prefs.setPrefEducation(request.partnerPreferences().preferredEducation());
        applyAdditionalPreferenceFields(prefs, request.partnerPreferences());
        prefs.setUpdatedAt(Instant.now());

        preferenceRepository.save(prefs);

        //Profile matching
        // Calculate exact age in Java
        int userAge = Period.between(request.basicDetails().dateOfBirth(), LocalDate.now()).getYears();

        MatchProfile matchData = new MatchProfile();
        matchData.setUserId(authenticatedUserId);
        matchData.setGender(request.basicDetails().gender());
        matchData.setBirthDate(request.basicDetails().dateOfBirth());
        matchData.setAge(userAge);
        matchData.setMaritalStatus(request.basicDetails().maritalStatus());
        matchData.setReligion(request.background().religion());
        matchData.setCommunity(request.background().community());
        matchData.setCurrentCity(request.locationAndFamily().currentCity());

        matchData.setPrefAgeMin(request.partnerPreferences().minAge());
        matchData.setPrefAgeMax(request.partnerPreferences().maxAge());
        matchData.setPrefLocations(request.partnerPreferences().preferredLocations());
        matchData.setActive(true);
        matchData.setVerified(false);

        matchProfileRepository.save(matchData);

        // 3. Return success format for Flutter app
        return new OnboardingResponse(
                "Profile setup completed successfully.",
                authenticatedUserId,
                "UNDER_REVIEW", // Matches our platform plan for manual verification later
                true
        );
    }

    @Transactional
    public OnboardingResponse updateExistingProfile(UUID authenticatedUserId, OnboardingRequest request) {

        // 1. Fetch existing profile (must exist to be updated)
        Profile profile = profileRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Profile not found. Complete onboarding first."
                ));

        // 2. Update Profile fields
        profile.setProfileCreatedFor(request.profileCreatedFor());
        profile.setFullName(request.basicDetails().fullName());
        profile.setGender(request.basicDetails().gender());
        profile.setDateOfBirth(request.basicDetails().dateOfBirth());
        profile.setHeightCm(request.basicDetails().heightCm());
        profile.setMaritalStatus(request.basicDetails().maritalStatus());

        profile.setReligion(request.background().religion());
        profile.setCommunity(request.background().community());
        profile.setShowCommunity(request.background().showCommunityOnProfile());

        profile.setHighestQualification(request.educationAndCareer().highestQualification());
        profile.setFieldOfStudy(request.educationAndCareer().fieldOfStudy());
        profile.setProfessionDomain(request.educationAndCareer().professionDomain());
        profile.setEmploymentType(request.educationAndCareer().employmentType());
        profile.setAnnualIncome(request.educationAndCareer().annualIncomeInr());

        profile.setCurrentCity(request.locationAndFamily().currentCity());
        profile.setNativePlace(request.locationAndFamily().nativePlace());
        profile.setFamilyType(request.locationAndFamily().familyType());
        profile.setFatherOccupation(request.locationAndFamily().fatherOccupation());

        profile.setFoodPreference(request.lifestyle().foodPreference());
        profile.setSmoking(request.lifestyle().smoking());
        profile.setAlcohol(request.lifestyle().alcohol());
        profile.setInterests(request.lifestyle().interests());
        applyAdditionalProfileFields(profile, request);
        profile.setUpdatedAt(Instant.now());

        profileRepository.save(profile);

        // 3. Update Partner Preferences
        PartnerPreference prefs = preferenceRepository.findById(authenticatedUserId)
                .orElseGet(PartnerPreference::new);
        prefs.setUserId(authenticatedUserId);
        prefs.setPrefAgeMin(request.partnerPreferences().minAge());
        prefs.setPrefAgeMax(request.partnerPreferences().maxAge());
        prefs.setPrefLocations(request.partnerPreferences().preferredLocations());
        prefs.setPrefEducation(request.partnerPreferences().preferredEducation());
        applyAdditionalPreferenceFields(prefs, request.partnerPreferences());
        prefs.setUpdatedAt(Instant.now());

        preferenceRepository.save(prefs);

        // 4. Sync changes to MatchProfile table so Discovery stays accurate!
        int updatedAge = Period.between(request.basicDetails().dateOfBirth(), LocalDate.now()).getYears();

        MatchProfile matchData = matchProfileRepository.findById(authenticatedUserId)
                .orElseGet(MatchProfile::new);
        matchData.setUserId(authenticatedUserId);
        matchData.setGender(request.basicDetails().gender());
        matchData.setBirthDate(request.basicDetails().dateOfBirth());
        matchData.setAge(updatedAge);
        matchData.setMaritalStatus(request.basicDetails().maritalStatus());
        matchData.setReligion(request.background().religion());
        matchData.setCommunity(request.background().community());
        matchData.setCurrentCity(request.locationAndFamily().currentCity());
        matchData.setPrefAgeMin(request.partnerPreferences().minAge());
        matchData.setPrefAgeMax(request.partnerPreferences().maxAge());
        matchData.setPrefLocations(request.partnerPreferences().preferredLocations());
        matchData.setUpdatedAt(Instant.now());

        matchProfileRepository.save(matchData);

        return new OnboardingResponse(
                "Profile and preferences updated successfully.",
                authenticatedUserId,
                "UPDATED",
                true
        );
    }

    private void applyAdditionalProfileFields(Profile profile, OnboardingRequest request) {
        profile.setMotherTongue(request.basicDetails().motherTongue());
        profile.setSubCommunity(request.background().subCommunity());
        profile.setPaternalGotra(request.background().paternalGotra());
        profile.setMaternalGotra(request.background().maternalGotra());
        profile.setSampradaya(request.background().sampradaya());
        profile.setMotherOccupation(request.locationAndFamily().motherOccupation());
        profile.setSiblings(request.locationAndFamily().siblings());
        profile.setWeekendPreference(request.lifestyle().weekendPreference());
        profile.setCurrentState(request.locationAndFamily().currentState());
        profile.setNativeDistrict(request.locationAndFamily().nativeDistrict());
        profile.setWorkCity(request.educationAndCareer().workCity());
        profile.setRelocationPreference(request.locationAndFamily().relocationPreference());
    }

    private void applyAdditionalPreferenceFields(
            PartnerPreference prefs, OnboardingRequest.PartnerPreferencesDto request) {
        prefs.setPreferredMaritalStatuses(request.preferredMaritalStatuses());
        prefs.setExcludePaternalGotra(request.excludePaternalGotra());
        prefs.setExcludeMaternalGotra(request.excludeMaternalGotra());
        prefs.setPreferredSubCommunities(request.preferredSubCommunities());
    }
}

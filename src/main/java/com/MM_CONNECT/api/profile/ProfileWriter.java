package com.MM_CONNECT.api.profile;

import com.MM_CONNECT.api.matching.MatchProfile;
import com.MM_CONNECT.api.matching.MatchProfileRepository;
import com.MM_CONNECT.api.profile.dto.OnboardingRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

/** Writes the canonical profile, preferences and matching copy as one transaction. */
@Service
public class ProfileWriter {
    private final ProfileRepository profileRepository;
    private final PartnerPreferenceRepository preferenceRepository;
    private final MatchProfileRepository matchProfileRepository;

    public ProfileWriter(ProfileRepository profiles, PartnerPreferenceRepository preferences,
                         MatchProfileRepository matches) {
        profileRepository = profiles;
        preferenceRepository = preferences;
        matchProfileRepository = matches;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void write(UUID authenticatedUserId, OnboardingRequest request, boolean published) {
        // The caller holds the account lock and decides whether the canonical data is published.
        Profile profile = profileRepository.findById(authenticatedUserId).orElseGet(Profile::new);
        profile.setUserId(authenticatedUserId);

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

        matchData.setActive(published);
        matchData.setVerified(published);
        matchProfileRepository.save(matchData);

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

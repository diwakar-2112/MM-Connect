package com.MM_CONNECT.api.profile;

import com.MM_CONNECT.api.media.PhotoUploadService;
import com.MM_CONNECT.api.media.ProfilePhoto;
import com.MM_CONNECT.api.media.ProfilePhotoRepository;
import com.MM_CONNECT.api.profile.dto.MyProfileResponse;
import com.MM_CONNECT.api.profile.dto.MyProfileResponse.PhotoViewDto;
import com.MM_CONNECT.api.profile.dto.OnboardingRequest;
import com.MM_CONNECT.api.profile.dto.OnboardingResponse;
import com.MM_CONNECT.api.profile.dto.ProfileViewResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/profiles")
public class ProfileController {

    private final ProfileRepository profileRepository;
    private final PartnerPreferenceRepository preferenceRepository;
    private final ProfilePhotoRepository photoRepository;
    private final PhotoUploadService photoUploadService;
    private final OnboardingManagerService onboardingManagerService; // <-- Added
    private final ProfileViewService profileViewService;

    public ProfileController(ProfileRepository profileRepository,
                             PartnerPreferenceRepository preferenceRepository,
                             ProfilePhotoRepository photoRepository,
                             PhotoUploadService photoUploadService,
                             OnboardingManagerService onboardingManagerService,
                             ProfileViewService profileViewService) {
        this.profileRepository = profileRepository;
        this.preferenceRepository = preferenceRepository;
        this.photoRepository = photoRepository;
        this.photoUploadService = photoUploadService;
        this.onboardingManagerService = onboardingManagerService;
        this.profileViewService = profileViewService;
    }

    @GetMapping("/me")
    public ResponseEntity<MyProfileResponse> getMyProfile(@AuthenticationPrincipal UUID userId) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));

        PartnerPreference prefs = preferenceRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Preferences not found"));

        List<PhotoViewDto> photoDtos = new ArrayList<>();
        List<ProfilePhoto> myPhotos = photoRepository.findAll().stream()
                .filter(photo -> photo.getUserId().equals(userId))
                .toList();

        for (ProfilePhoto photo : myPhotos) {
            String tempUrl = photoUploadService.generatePresignedGetUrl(photo.getStorageKey());
            photoDtos.add(new PhotoViewDto(tempUrl, photo.isPrimary()));
        }

        return ResponseEntity.ok(new MyProfileResponse(profile, prefs, photoDtos));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ProfileViewResponse> getProfileById(
            @AuthenticationPrincipal UUID viewerId,
            @PathVariable("userId") UUID userId) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(profileViewService.getProfile(viewerId, userId));
    }

    // ---> NEW ENDPOINT: UPDATE PROFILE <---
    @PutMapping("/me")
    public ResponseEntity<OnboardingResponse> updateMyProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody OnboardingRequest payload) {

        OnboardingResponse updated = onboardingManagerService.updateExistingProfile(userId, payload);
        return ResponseEntity.ok(updated);
    }
}

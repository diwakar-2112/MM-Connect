package com.MM_CONNECT.api.profile;

import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import com.MM_CONNECT.api.connection.ConnectionRequestRepository;
import com.MM_CONNECT.api.matching.UserInteractionRepository;
import com.MM_CONNECT.api.media.PhotoUploadService;
import com.MM_CONNECT.api.media.ProfilePhoto;
import com.MM_CONNECT.api.media.ProfilePhotoRepository;
import com.MM_CONNECT.api.profile.dto.ProfileViewResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
public class ProfileViewService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserInteractionRepository interactionRepository;
    private final ConnectionRequestRepository connectionRepository;
    private final ProfilePhotoRepository photoRepository;
    private final PhotoUploadService photoUploadService;

    public ProfileViewService(UserRepository userRepository,
                              ProfileRepository profileRepository,
                              UserInteractionRepository interactionRepository,
                              ConnectionRequestRepository connectionRepository,
                              ProfilePhotoRepository photoRepository,
                              PhotoUploadService photoUploadService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.interactionRepository = interactionRepository;
        this.connectionRepository = connectionRepository;
        this.photoRepository = photoRepository;
        this.photoUploadService = photoUploadService;
    }

    @Transactional(readOnly = true)
    public ProfileViewResponse getProfile(UUID viewerId, UUID targetUserId) {
        User viewer = userRepository.findById(viewerId)
                .filter(user -> user.isActive() && user.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active."));

        if (!profileRepository.existsById(viewerId)) {
            var error = new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Please finish onboarding to view this profile.");
            error.getBody().setProperty("code", "ONBOARDING_REQUIRED");
            throw error;
        }

        boolean owner = viewerId.equals(targetUserId);
        if (!owner && (interactionRepository.existsByActorIdAndTargetIdAndAction(viewerId, targetUserId, "BLOCKED")
                || interactionRepository.existsByActorIdAndTargetIdAndAction(targetUserId, viewerId, "BLOCKED"))) {
            throw profileNotFound();
        }

        User target = owner ? viewer : userRepository.findById(targetUserId)
                .filter(user -> user.isActive() && user.getDeletedAt() == null)
                .orElseThrow(this::profileNotFound);
        Profile profile = profileRepository.findById(targetUserId).orElseThrow(this::profileNotFound);

        List<ProfilePhoto> photos = photoRepository.findProfilePhotos(targetUserId);
        boolean needsAcceptedConnection = !owner && photos.stream()
                .anyMatch(photo -> "APPROVED".equals(photo.getModerationStatus())
                        && "ACCEPTED_ONLY".equals(photo.getVisibility()));
        boolean accepted = needsAcceptedConnection
                && (connectionRepository.existsBySenderIdAndReceiverIdAndStatus(viewerId, targetUserId, "ACCEPTED")
                    || connectionRepository.existsBySenderIdAndReceiverIdAndStatus(targetUserId, viewerId, "ACCEPTED"));

        List<ProfileViewResponse.PhotoViewDto> photoViews = photos.stream()
                .filter(photo -> canViewPhoto(photo, owner, viewer.isVerified(), accepted))
                .map(photo -> new ProfileViewResponse.PhotoViewDto(
                        photo.getId(),
                        photoUploadService.generatePresignedGetUrl(photo.getStorageKey()),
                        photo.isPrimary()))
                .toList();

        return new ProfileViewResponse(toProfileDetails(profile, target.isVerified()), photoViews);
    }

    private boolean canViewPhoto(ProfilePhoto photo, boolean owner, boolean verifiedViewer, boolean accepted) {
        if (owner) {
            return true;
        }
        if (!"APPROVED".equals(photo.getModerationStatus())) {
            return false;
        }
        return switch (photo.getVisibility()) {
            case "ALL_VERIFIED" -> verifiedViewer;
            case "ACCEPTED_ONLY" -> accepted;
            default -> false;
        };
    }

    private ProfileViewResponse.ProfileDetails toProfileDetails(Profile profile, boolean verified) {
        boolean showCommunity = profile.isShowCommunity();
        return new ProfileViewResponse.ProfileDetails(
                profile.getUserId(),
                profile.getProfileCreatedFor(),
                profile.getFullName(),
                profile.getGender(),
                Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears(),
                profile.getHeightCm(),
                profile.getMaritalStatus(),
                profile.getMotherTongue(),
                profile.getReligion(),
                showCommunity ? profile.getCommunity() : null,
                showCommunity ? profile.getSubCommunity() : null,
                showCommunity,
                profile.getPaternalGotra(),
                profile.getMaternalGotra(),
                profile.getSampradaya(),
                profile.getHighestQualification(),
                profile.getFieldOfStudy(),
                profile.getProfessionDomain(),
                profile.getEmploymentType(),
                profile.getCurrentCity(),
                profile.getCurrentState(),
                profile.getNativePlace(),
                profile.getNativeDistrict(),
                profile.getWorkCity(),
                profile.getRelocationPreference(),
                profile.getFamilyType(),
                profile.getFatherOccupation(),
                profile.getMotherOccupation(),
                profile.getSiblings(),
                profile.getFoodPreference(),
                profile.getSmoking(),
                profile.getAlcohol(),
                profile.getInterests(),
                profile.getWeekendPreference(),
                verified);
    }

    private ResponseStatusException profileNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
    }
}

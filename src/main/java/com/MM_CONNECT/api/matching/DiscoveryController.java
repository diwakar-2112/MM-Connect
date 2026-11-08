package com.MM_CONNECT.api.matching;

import com.MM_CONNECT.api.auth.UserRepository;
import com.MM_CONNECT.api.media.PhotoUploadService;
import com.MM_CONNECT.api.media.ProfilePhoto;
import com.MM_CONNECT.api.media.ProfilePhotoRepository;
import com.MM_CONNECT.api.profile.Profile;
import com.MM_CONNECT.api.profile.ProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v1/discover")
public class DiscoveryController {

    private final MatchProfileRepository matchRepository;
    private final ProfileRepository profileRepository;
    private final ProfilePhotoRepository photoRepository;
    private final PhotoUploadService photoUploadService;
    private final UserInteractionRepository interactionRepository; // <-- Added
    private final UserRepository userRepository;

    public DiscoveryController(MatchProfileRepository matchRepository,
                               ProfileRepository profileRepository,
                               ProfilePhotoRepository photoRepository,
                               PhotoUploadService photoUploadService,
                               UserInteractionRepository interactionRepository,
                               UserRepository userRepository) {
        this.matchRepository = matchRepository;
        this.profileRepository = profileRepository;
        this.photoRepository = photoRepository;
        this.photoUploadService = photoUploadService;
        this.interactionRepository = interactionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<DiscoveryFeedResponse> getDiscoveryFeed(
            @AuthenticationPrincipal UUID myUserId,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String city,
            @RequestParam(name = "gender", required = false) String gender,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        userRepository.findById(myUserId)
                .filter(user -> user.isActive() && user.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active."));
        boolean profileLocked = !profileRepository.existsById(myUserId);

        String seekingGender = resolveSeekingGender(gender);
        Pageable pageable = PageRequest.of(page, size);

        Page<MatchProfile> matchPage = matchRepository.findDiscoveryFeed(
                myUserId, seekingGender, minAge, maxAge, city, pageable
        );

        List<DiscoveryCardDto> feed = new ArrayList<>();
        for (MatchProfile match : matchPage.getContent()) {
            DiscoveryCardDto card = buildCard(match);
            if (card != null) feed.add(card);
        }

        return ResponseEntity.ok(new DiscoveryFeedResponse(
                profileLocked,
                feed,
                matchPage.getNumber(),
                matchPage.getSize(),
                matchPage.getTotalElements(),
                matchPage.getTotalPages(),
                matchPage.isLast()
        ));
    }

    private String resolveSeekingGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return null;
        }

        return switch (gender.trim().toUpperCase(Locale.ROOT)) {
            case "MAN", "MALE" -> "MAN";
            case "WOMAN", "FEMALE" -> "WOMAN";
            case "ANY" -> null; // No gender constraint; all other discovery rules still apply.
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid gender filter. Use MAN, WOMAN, or ANY.");
        };
    }

    // 1. PASS A PROFILE
    @PostMapping("/{targetUserId}/pass")
    public ResponseEntity<Map<String, String>> passProfile(
            @AuthenticationPrincipal UUID myUserId,
            @PathVariable UUID targetUserId) {

        interactionRepository.save(new UserInteraction(myUserId, targetUserId, "PASSED"));
        return ResponseEntity.ok(Map.of("message", "Profile passed and hidden from discovery."));
    }

    // 2. SHORTLIST A PROFILE
    @PostMapping("/{targetUserId}/shortlist")
    public ResponseEntity<Map<String, String>> shortlistProfile(
            @AuthenticationPrincipal UUID myUserId,
            @PathVariable UUID targetUserId) {

        interactionRepository.save(new UserInteraction(myUserId, targetUserId, "SHORTLISTED"));
        return ResponseEntity.ok(Map.of("message", "Profile added to shortlist."));
    }

    // 3. GET MY SHORTLISTED PROFILES
    @GetMapping("/shortlist")
    public ResponseEntity<List<DiscoveryCardDto>> getShortlistedProfiles(@AuthenticationPrincipal UUID myUserId) {
        List<UserInteraction> shortlisted = interactionRepository.findByActorIdAndAction(myUserId, "SHORTLISTED");

        List<DiscoveryCardDto> result = new ArrayList<>();
        for (UserInteraction item : shortlisted) {
            matchRepository.findById(item.getTargetId()).ifPresent(match -> {
                DiscoveryCardDto card = buildCard(match);
                if (card != null) result.add(card);
            });
        }
        return ResponseEntity.ok(result);
    }

    // Helper method to build a UI Card cleanly
    private DiscoveryCardDto buildCard(MatchProfile match) {
        Profile profileDetails = profileRepository.findById(match.getUserId()).orElse(null);
        if (profileDetails == null) return null;

        String photoUrl = null;
        Optional<ProfilePhoto> mainPhoto = photoRepository.findAll().stream()
                .filter(p -> p.getUserId().equals(match.getUserId()) && p.isPrimary())
                .findFirst();

        if (mainPhoto.isPresent()) {
            photoUrl = photoUploadService.generatePresignedGetUrl(mainPhoto.get().getStorageKey());
        }

        return new DiscoveryCardDto(
                match.getUserId(),
                profileDetails.getFullName(),
                match.getAge(),
                match.getCurrentCity(),
                profileDetails.getProfessionDomain(),
                match.getReligion(),
                photoUrl
        );
    }
}

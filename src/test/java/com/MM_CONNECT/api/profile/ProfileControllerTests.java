package com.MM_CONNECT.api.profile;

import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import com.MM_CONNECT.api.common.GlobalExceptionHandler;
import com.MM_CONNECT.api.connection.ConnectionRequestRepository;
import com.MM_CONNECT.api.matching.UserInteractionRepository;
import com.MM_CONNECT.api.media.PhotoUploadService;
import com.MM_CONNECT.api.media.ProfilePhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ProfileControllerTests {

    private final UUID viewerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();
    private UserRepository users;
    private ProfileRepository profiles;
    private ProfilePhotoRepository photos;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        profiles = mock(ProfileRepository.class);
        var preferences = mock(PartnerPreferenceRepository.class);
        photos = mock(ProfilePhotoRepository.class);
        var photoUrls = mock(PhotoUploadService.class);
        var service = new ProfileViewService(users, profiles, mock(UserInteractionRepository.class),
                mock(ConnectionRequestRepository.class), photos, photoUrls);
        var controller = new ProfileController(profiles, preferences, photos, photoUrls,
                mock(OnboardingManagerService.class), service);

        User viewer = new User(null, "viewer@example.test");
        viewer.setId(viewerId);
        User target = new User("1234567890", "private@example.test");
        target.setId(targetId);
        when(users.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(users.findById(targetId)).thenReturn(Optional.of(target));
        when(profiles.existsById(viewerId)).thenReturn(true);
        Profile targetProfile = new Profile();
        targetProfile.setUserId(targetId);
        targetProfile.setFullName("Example Partner");
        targetProfile.setDateOfBirth(LocalDate.of(1995, 1, 1));
        targetProfile.setAnnualIncome(1200000L);
        targetProfile.setCurrentState("Maharashtra");
        targetProfile.setNativeDistrict("Sikar");
        targetProfile.setWorkCity("Pune");
        targetProfile.setRelocationPreference("DEPENDS");
        when(profiles.findById(targetId)).thenReturn(Optional.of(targetProfile));
        when(photos.findProfilePhotos(targetId)).thenReturn(List.of());

        Profile ownProfile = new Profile();
        ownProfile.setUserId(viewerId);
        ownProfile.setDateOfBirth(LocalDate.of(1990, 2, 1));
        when(profiles.findById(viewerId)).thenReturn(Optional.of(ownProfile));
        when(preferences.findById(viewerId)).thenReturn(Optional.of(new PartnerPreference()));
        when(photos.findAll()).thenReturn(List.of());

        mvc = standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                                  NativeWebRequest request, WebDataBinderFactory binderFactory) {
                        return viewerId;
                    }
                }).build();
    }

    @Test
    void detailResponseIncludesLocationsWithoutPrivateAccountOrIncomeFields() throws Exception {
        mvc.perform(get("/v1/profiles/" + targetId))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.profileDetails.userId").value(targetId.toString()))
                .andExpect(jsonPath("$.profileDetails.fullName").value("Example Partner"))
                .andExpect(jsonPath("$.profileDetails.age").isNumber())
                .andExpect(jsonPath("$.profileDetails.currentState").value("Maharashtra"))
                .andExpect(jsonPath("$.profileDetails.nativeDistrict").value("Sikar"))
                .andExpect(jsonPath("$.profileDetails.workCity").value("Pune"))
                .andExpect(jsonPath("$.profileDetails.relocationPreference").value("DEPENDS"))
                .andExpect(jsonPath("$.photos").isArray())
                .andExpect(jsonPath("$.profileDetails.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.profileDetails.email").doesNotExist())
                .andExpect(jsonPath("$.profileDetails.dateOfBirth").doesNotExist())
                .andExpect(jsonPath("$.profileDetails.annualIncome").doesNotExist())
                .andExpect(jsonPath("$.profileDetails.role").doesNotExist())
                .andExpect(jsonPath("$.partnerPreferences").doesNotExist());
    }

    @Test
    void meRouteStillReturnsTheOwnersExistingResponse() throws Exception {
        mvc.perform(get("/v1/profiles/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileDetails.userId").value(viewerId.toString()))
                .andExpect(jsonPath("$.profileDetails.dateOfBirth").exists())
                .andExpect(jsonPath("$.partnerPreferences").exists());

        verifyNoInteractions(users);
    }

    @Test
    void malformedUserIdReturnsBadRequest() throws Exception {
        mvc.perform(get("/v1/profiles/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Invalid value for parameter 'userId'."));

        verifyNoInteractions(users);
    }

    @Test
    void directProfileRequestRequiresOnboardingBeforeReadingTargetData() throws Exception {
        when(profiles.existsById(viewerId)).thenReturn(false);

        mvc.perform(get("/v1/profiles/" + targetId).param("profileLocked", "false"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ONBOARDING_REQUIRED"))
                .andExpect(jsonPath("$.detail").value("Please finish onboarding to view this profile."))
                .andExpect(jsonPath("$.profileDetails").doesNotExist())
                .andExpect(jsonPath("$.photos").doesNotExist());

        verify(users, never()).findById(targetId);
        verify(profiles, never()).findById(targetId);
        verifyNoInteractions(photos);
    }

    @Test
    void missingProfileReturnsNotFound() throws Exception {
        when(profiles.findById(targetId)).thenReturn(Optional.empty());

        mvc.perform(get("/v1/profiles/" + targetId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Profile not found"));
    }
}

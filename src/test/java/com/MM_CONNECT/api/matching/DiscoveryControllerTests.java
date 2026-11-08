package com.MM_CONNECT.api.matching;

import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import com.MM_CONNECT.api.common.GlobalExceptionHandler;
import com.MM_CONNECT.api.media.PhotoUploadService;
import com.MM_CONNECT.api.media.ProfilePhotoRepository;
import com.MM_CONNECT.api.profile.Profile;
import com.MM_CONNECT.api.profile.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class DiscoveryControllerTests {

    private final UUID userId = UUID.randomUUID();
    private MatchProfileRepository matchRepository;
    private ProfileRepository profiles;
    private UserRepository users;
    private User viewer;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        matchRepository = mock(MatchProfileRepository.class);
        profiles = mock(ProfileRepository.class);
        users = mock(UserRepository.class);
        viewer = new User(null, "viewer@example.test");
        viewer.setId(userId);
        when(users.findById(userId)).thenReturn(Optional.of(viewer));
        when(profiles.existsById(userId)).thenReturn(true);
        when(matchRepository.findDiscoveryFeed(eq(userId), nullable(String.class),
                nullable(Integer.class), nullable(Integer.class), nullable(String.class), any(Pageable.class)))
                .thenAnswer(invocation -> Page.empty(invocation.getArgument(5, Pageable.class)));

        DiscoveryController controller = new DiscoveryController(matchRepository,
                profiles, mock(ProfilePhotoRepository.class),
                mock(PhotoUploadService.class), mock(UserInteractionRepository.class), users);

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
                        return userId;
                    }
                })
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MAN", "WOMAN", "OTHER"})
    void omittedGenderShowsAllGendersRegardlessOfOwnGender(String ownGender) throws Exception {
        Profile ownProfile = new Profile();
        ownProfile.setGender(ownGender);
        when(profiles.findById(userId)).thenReturn(Optional.of(ownProfile));

        mvc.perform(get("/v1/discover"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileLocked").value(false))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(20));

        verify(matchRepository).findDiscoveryFeed(userId, null, null, null, null,
                PageRequest.of(0, 20));
    }

    @ParameterizedTest
    @CsvSource({"MAN,MAN", "WOMAN,WOMAN", "male,MAN", "female,WOMAN", "man,MAN", "woman,WOMAN"})
    void explicitGenderOverridesDefault(String selectedGender, String expectedGender) throws Exception {
        mvc.perform(get("/v1/discover").param("gender", selectedGender))
                .andExpect(status().isOk());

        verify(matchRepository).findDiscoveryFeed(userId, expectedGender, null, null, null,
                PageRequest.of(0, 20));
    }

    @Test
    void anyRemovesGenderConstraintAndPreservesOtherFiltersAndPagination() throws Exception {
        mvc.perform(get("/v1/discover").param("gender", " any ")
                        .param("minAge", "24").param("maxAge", "32").param("city", "Pune")
                        .param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber").value(2))
                .andExpect(jsonPath("$.pageSize").value(10));

        verify(matchRepository).findDiscoveryFeed(userId, null, 24, 32, "Pune", PageRequest.of(2, 10));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankGenderShowsAllGenders(String gender) throws Exception {
        mvc.perform(get("/v1/discover").param("gender", gender))
                .andExpect(status().isOk());

        verify(matchRepository).findDiscoveryFeed(userId, null, null, null, null, PageRequest.of(0, 20));
    }

    @Test
    void omittedGenderPreservesOtherFiltersAndPagination() throws Exception {
        mvc.perform(get("/v1/discover")
                        .param("minAge", "24").param("maxAge", "32").param("city", "Pune")
                        .param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber").value(2))
                .andExpect(jsonPath("$.pageSize").value(10));

        verify(matchRepository).findDiscoveryFeed(userId, null, 24, 32, "Pune", PageRequest.of(2, 10));
    }

    @Test
    void invalidGenderReturnsBadRequestWithoutQueryingFeed() throws Exception {
        mvc.perform(get("/v1/discover").param("gender", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Invalid gender filter. Use MAN, WOMAN, or ANY."));

        verify(matchRepository, never()).findDiscoveryFeed(any(UUID.class), nullable(String.class),
                nullable(Integer.class), nullable(Integer.class), nullable(String.class), any(Pageable.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ANY", "MAN", "WOMAN"})
    void incompleteViewerCanBrowseCardsWithOneCommonLock(String gender) throws Exception {
        when(profiles.existsById(userId)).thenReturn(false);
        UUID targetId = UUID.randomUUID();
        MatchProfile target = new MatchProfile();
        target.setUserId(targetId);
        target.setAge(28);
        Profile targetProfile = new Profile();
        targetProfile.setFullName("Example Partner");
        when(profiles.findById(targetId)).thenReturn(Optional.of(targetProfile));
        when(matchRepository.findDiscoveryFeed(eq(userId), nullable(String.class),
                nullable(Integer.class), nullable(Integer.class), nullable(String.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(target),
                        invocation.getArgument(5, Pageable.class), 1));

        var request = get("/v1/discover");
        if (gender != null) {
            request.param("gender", gender);
        }

        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileLocked").value(true))
                .andExpect(jsonPath("$.content[0].userId").value(targetId.toString()))
                .andExpect(jsonPath("$.content[0].fullName").value("Example Partner"))
                .andExpect(jsonPath("$.content[0].profileLocked").doesNotExist());

        String expectedGender = "MAN".equals(gender) || "WOMAN".equals(gender) ? gender : null;
        verify(matchRepository).findDiscoveryFeed(userId, expectedGender, null, null, null, PageRequest.of(0, 20));
    }

    @Test
    void lockedFeedPreservesFiltersAndPaginationEvenWhenEmpty() throws Exception {
        when(profiles.existsById(userId)).thenReturn(false);

        mvc.perform(get("/v1/discover").param("profileLocked", "false").param("gender", "WOMAN")
                        .param("minAge", "24").param("maxAge", "32").param("city", "Pune")
                        .param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileLocked").value(true))
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.pageNumber").value(2))
                .andExpect(jsonPath("$.pageSize").value(10));

        verify(matchRepository).findDiscoveryFeed(userId, "WOMAN", 24, 32, "Pune", PageRequest.of(2, 10));
    }

    @Test
    void lockReflectsCompletedOnboardingOnTheNextRequest() throws Exception {
        when(profiles.existsById(userId)).thenReturn(false, true);

        mvc.perform(get("/v1/discover"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileLocked").value(true));
        mvc.perform(get("/v1/discover"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileLocked").value(false));
    }

    @Test
    void lockedViewerStillReceivesInvalidGenderError() throws Exception {
        when(profiles.existsById(userId)).thenReturn(false);

        mvc.perform(get("/v1/discover").param("gender", "INVALID"))
                .andExpect(status().isBadRequest());

        verify(matchRepository, never()).findDiscoveryFeed(any(UUID.class), nullable(String.class),
                nullable(Integer.class), nullable(Integer.class), nullable(String.class), any(Pageable.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING", "INACTIVE", "DELETED"})
    void unavailableAccountCannotBrowsePreviewFeed(String condition) throws Exception {
        switch (condition) {
            case "MISSING" -> when(users.findById(userId)).thenReturn(Optional.empty());
            case "INACTIVE" -> viewer.setActive(false);
            case "DELETED" -> viewer.setDeletedAt(Instant.now());
        }

        mvc.perform(get("/v1/discover"))
                .andExpect(status().isForbidden());

        verify(matchRepository, never()).findDiscoveryFeed(any(UUID.class), nullable(String.class),
                nullable(Integer.class), nullable(Integer.class), nullable(String.class), any(Pageable.class));
    }
}

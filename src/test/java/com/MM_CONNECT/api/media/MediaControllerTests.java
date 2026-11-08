package com.MM_CONNECT.api.media;

import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import com.MM_CONNECT.api.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class MediaControllerTests {

    private final UUID userId = UUID.randomUUID();
    private ProfilePhotoRepository photos;
    private S3Presigner presigner;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        photos = mock(ProfilePhotoRepository.class);
        presigner = mock(S3Presigner.class);
        var users = mock(UserRepository.class);
        when(users.findByIdForUpdate(userId)).thenReturn(Optional.of(new User()));
        when(photos.countByUserId(userId)).thenReturn(5L);
        var controller = new MediaController(new PhotoUploadService(presigner, photos, users));
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
                }).build();
    }

    @Test
    void uploadUrlAtLimitReturnsConflictWithErrorCode() throws Exception {
        mvc.perform(post("/v1/media/upload-url").accept("application/json"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("PHOTO_LIMIT_REACHED"))
                .andExpect(jsonPath("$.maxPhotos").value(5))
                .andExpect(jsonPath("$.uploadUrl").doesNotExist());

        verifyNoInteractions(presigner);
    }

    @Test
    void directConfirmAtLimitReturnsConflictWithoutSaving() throws Exception {
        mvc.perform(post("/v1/media/confirm").contentType("application/json")
                        .accept("text/plain", "application/problem+json")
                        .content("{\"fileKey\":\"sixth.jpg\",\"isPrimary\":false}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("PHOTO_LIMIT_REACHED"))
                .andExpect(jsonPath("$.detail").value("You can upload a maximum of 5 profile photos."))
                .andExpect(jsonPath("$.maxPhotos").value(5));

        verify(photos, never()).save(any(ProfilePhoto.class));
    }
}

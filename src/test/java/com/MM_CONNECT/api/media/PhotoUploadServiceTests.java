package com.MM_CONNECT.api.media;

import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PhotoUploadServiceTests {

    private final UUID userId = UUID.randomUUID();
    private S3Presigner presigner;
    private ProfilePhotoRepository photos;
    private UserRepository users;
    private PhotoUploadService service;

    @BeforeEach
    void setUp() {
        presigner = mock(S3Presigner.class);
        photos = mock(ProfilePhotoRepository.class);
        users = mock(UserRepository.class);
        when(users.findByIdForUpdate(userId)).thenReturn(Optional.of(new User()));
        service = new PhotoUploadService(presigner, photos, users);
        ReflectionTestUtils.setField(service, "bucketName", "test-profile-photos");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 4})
    void uploadUrlIsAvailableBelowFivePhotos(long savedCount) throws Exception {
        when(photos.countByUserId(userId)).thenReturn(savedCount);
        var signed = mock(PresignedPutObjectRequest.class);
        when(signed.url()).thenReturn(URI.create("https://storage.example.test/upload").toURL());
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(signed);

        UploadLinkResponse response = service.generatePresignedPutUrl(userId, ".jpg");

        assertEquals("https://storage.example.test/upload", response.uploadUrl());
        assertTrue(response.fileKey().startsWith("profiles/" + userId + "/"));
        verify(photos, never()).save(any(ProfilePhoto.class));
    }

    @ParameterizedTest
    @ValueSource(longs = {5, 6})
    void fullOrPreviouslyOverLimitAccountsCannotRequestAnUploadUrl(long savedCount) {
        when(photos.countByUserId(userId)).thenReturn(savedCount);

        assertPhotoLimit(assertThrows(ResponseStatusException.class,
                () -> service.generatePresignedPutUrl(userId, ".jpg")));

        verifyNoInteractions(presigner);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void fifthPhotoCanBeSavedWhetherPrimaryOrAdditional(boolean primary) {
        when(photos.countByUserId(userId)).thenReturn(4L);

        service.confirmAndSavePhoto(userId, new PhotoConfirmRequest("photo-five.jpg", primary));

        var order = inOrder(users, photos);
        order.verify(users).findByIdForUpdate(userId);
        order.verify(photos).countByUserId(userId);
        var saved = org.mockito.ArgumentCaptor.forClass(ProfilePhoto.class);
        order.verify(photos).save(saved.capture());
        assertEquals(userId, saved.getValue().getUserId());
        assertEquals(primary, saved.getValue().isPrimary());
    }

    @ParameterizedTest
    @ValueSource(longs = {5, 6})
    void directConfirmationCannotAddAPhotoWhenFull(long savedCount) {
        when(photos.countByUserId(userId)).thenReturn(savedCount);

        assertPhotoLimit(assertThrows(ResponseStatusException.class,
                () -> service.confirmAndSavePhoto(userId, new PhotoConfirmRequest("extra.jpg", false))));

        var order = inOrder(users, photos);
        order.verify(users).findByIdForUpdate(userId);
        order.verify(photos).countByUserId(userId);
        verify(photos, never()).save(any(ProfilePhoto.class));
    }

    @Test
    void anotherUploadCannotUseAnOldUrlToBypassTheConfirmationLimit() throws Exception {
        when(photos.countByUserId(userId)).thenReturn(4L, 4L, 5L);
        var signed = mock(PresignedPutObjectRequest.class);
        when(signed.url()).thenReturn(URI.create("https://storage.example.test/upload").toURL());
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(signed);
        var oldLink = service.generatePresignedPutUrl(userId, ".jpg");

        service.confirmAndSavePhoto(userId, new PhotoConfirmRequest("fifth.jpg", false));
        assertPhotoLimit(assertThrows(ResponseStatusException.class,
                () -> service.confirmAndSavePhoto(userId, new PhotoConfirmRequest(oldLink.fileKey(), false))));

        verify(photos, times(1)).save(any(ProfilePhoto.class));
        verify(users, times(2)).findByIdForUpdate(userId);
    }

    @Test
    void missingAccountCannotConfirmPhotos() {
        when(users.findByIdForUpdate(userId)).thenReturn(Optional.empty());

        var error = assertThrows(ResponseStatusException.class,
                () -> service.confirmAndSavePhoto(userId, new PhotoConfirmRequest("photo.jpg", false)));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(photos);
    }

    private void assertPhotoLimit(ResponseStatusException error) {
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("You can upload a maximum of 5 profile photos.", error.getReason());
        assertEquals("PHOTO_LIMIT_REACHED", error.getBody().getProperties().get("code"));
        assertEquals(5, error.getBody().getProperties().get("maxPhotos"));
    }
}

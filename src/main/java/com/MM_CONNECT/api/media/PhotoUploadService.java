package com.MM_CONNECT.api.media;

import com.MM_CONNECT.api.auth.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import java.time.Duration;
import java.util.UUID;

@Service
public class PhotoUploadService {

    public static final int MAX_PROFILE_PHOTOS = 5;

    private final S3Presigner presigner;
    private final ProfilePhotoRepository photoRepository;
    private final UserRepository userRepository;

    @Value("${cloud.storage.bucket}")
    private String bucketName;

    public PhotoUploadService(S3Presigner presigner, ProfilePhotoRepository photoRepository,
                              UserRepository userRepository) {
        this.presigner = presigner;
        this.photoRepository = photoRepository;
        this.userRepository = userRepository;
    }

    public UploadLinkResponse generatePresignedPutUrl(UUID userId, String fileExtension) {
        requireAvailablePhotoSlot(userId);

        // 1. Create a unique path for the photo in the bucket
        String uniqueFileKey = "profiles/" + userId.toString() + "/" + UUID.randomUUID() + fileExtension;

        // 2. Tell S3 we expect an upload to this specific bucket and path
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(uniqueFileKey)
                .contentType("image/jpeg") // In production, dynamic based on fileExtension
                .build();

        // 3. Generate a signature valid for exactly 5 minutes
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(objectRequest)
                .build();

        String generatedUrl = presigner.presignPutObject(presignRequest).url().toString();

        return new UploadLinkResponse(generatedUrl, uniqueFileKey);
    }
    @Transactional
    public void confirmAndSavePhoto(UUID userId, PhotoConfirmRequest request) {
        // Lock the account, which exists even before onboarding, until this save commits.
        // Every confirmation for the same account must count photos after acquiring it.
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Account not found."));
        requireAvailablePhotoSlot(userId);

        // Saves the link into PostgreSQL!
        ProfilePhoto photo = new ProfilePhoto(userId, request.fileKey(), request.isPrimary());
        photoRepository.save(photo);

        // (Later in Phase 4: we can trigger EXIF stripping or image blurring here)
    }

    private void requireAvailablePhotoSlot(UUID userId) {
        if (photoRepository.countByUserId(userId) >= MAX_PROFILE_PHOTOS) {
            var error = new ResponseStatusException(HttpStatus.CONFLICT,
                    "You can upload a maximum of 5 profile photos.");
            error.getBody().setProperty("code", "PHOTO_LIMIT_REACHED");
            error.getBody().setProperty("maxPhotos", MAX_PROFILE_PHOTOS);
            throw error;
        }
    }

    public String generatePresignedGetUrl(String fileKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60)) // Link expires in 60 minutes for security
                .getObjectRequest(getObjectRequest)
                .build();

        return presigner.presignGetObject(presignRequest).url().toString();
    }
}

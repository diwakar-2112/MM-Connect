package com.MM_CONNECT.api.media;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.UUID;

@RestController
@RequestMapping("/v1/media")
public class MediaController {

    private final PhotoUploadService photoUploadService;

    public MediaController(PhotoUploadService photoUploadService) {
        this.photoUploadService = photoUploadService;
    }

    @PostMapping("/upload-url")
    public ResponseEntity<UploadLinkResponse> getUploadUrl(@AuthenticationPrincipal UUID userId) {
        // We will default to .jpg for now
        UploadLinkResponse response = photoUploadService.generatePresignedPutUrl(userId, ".jpg");
        return ResponseEntity.ok(response);
    }
    @PostMapping("/confirm")
    public ResponseEntity<String> confirmPhotoUpload(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody PhotoConfirmRequest request) {

        photoUploadService.confirmAndSavePhoto(userId, request);
        return ResponseEntity.ok("Photo linked to profile successfully.");
    }

    @GetMapping("/view")
    public ResponseEntity<String> viewPhoto(@AuthenticationPrincipal UUID userId, @RequestParam String fileKey) {

        // Note: In Phase 4, we will actually verify if this 'userId' has permission
        // to view this 'fileKey', but for now, we just generate the link.

        String viewUrl = photoUploadService.generatePresignedGetUrl(fileKey);
        return ResponseEntity.ok(viewUrl);
    }
}
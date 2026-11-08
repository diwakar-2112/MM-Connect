package com.MM_CONNECT.api.media;

public record UploadLinkResponse(
        String uploadUrl,
        String fileKey // Example: "profiles/user-id/xyz.jpg"
) {}
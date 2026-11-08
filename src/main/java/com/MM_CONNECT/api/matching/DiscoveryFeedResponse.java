package com.MM_CONNECT.api.matching;

import java.util.List;

public record DiscoveryFeedResponse(
        boolean profileLocked,
        List<DiscoveryCardDto> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean isLast
) {}

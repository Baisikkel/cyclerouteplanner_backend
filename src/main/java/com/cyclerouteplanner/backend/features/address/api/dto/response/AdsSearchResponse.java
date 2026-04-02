package com.cyclerouteplanner.backend.features.address.api.dto.response;

import java.time.Instant;

public record AdsSearchResponse(
        String provider,
        boolean reachable,
        String query,
        int limit,
        String details,
        String payloadSnippet,
        Instant checkedAt
) {
}

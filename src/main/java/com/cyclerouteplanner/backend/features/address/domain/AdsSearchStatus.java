package com.cyclerouteplanner.backend.features.address.domain;

import java.time.Instant;

public record AdsSearchStatus(
        boolean reachable,
        String provider,
        String query,
        int limit,
        String details,
        String payloadSnippet,
        Instant checkedAt
) {
}

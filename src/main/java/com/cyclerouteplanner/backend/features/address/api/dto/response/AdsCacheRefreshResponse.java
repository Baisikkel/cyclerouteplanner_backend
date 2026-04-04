package com.cyclerouteplanner.backend.features.address.api.dto.response;

import java.time.Instant;

public record AdsCacheRefreshResponse(
        String provider,
        boolean reachable,
        String query,
        int limit,
        int upsertedCount,
        String details,
        Instant checkedAt
) {
}

package com.cyclerouteplanner.backend.features.address.domain;

import java.time.Instant;

public record AdsCacheRefreshStatus(
        boolean reachable,
        String provider,
        String query,
        int limit,
        int upsertedCount,
        String details,
        Instant checkedAt
) {
}

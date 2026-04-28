package com.cyclerouteplanner.backend.features.routing.domain;

import java.time.Instant;

public record RouteOptionRefreshStatus(
        boolean successful,
        String source,
        int upsertedCount,
        String details,
        Instant checkedAt
) {
}

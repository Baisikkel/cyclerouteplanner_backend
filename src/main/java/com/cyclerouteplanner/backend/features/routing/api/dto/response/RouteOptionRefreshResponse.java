package com.cyclerouteplanner.backend.features.routing.api.dto.response;

import java.time.Instant;

public record RouteOptionRefreshResponse(
        String source,
        boolean successful,
        int upsertedCount,
        String details,
        Instant checkedAt
) {
}

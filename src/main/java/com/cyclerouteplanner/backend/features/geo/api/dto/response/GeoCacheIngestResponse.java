package com.cyclerouteplanner.backend.features.geo.api.dto.response;

import java.time.Instant;

public record GeoCacheIngestResponse(
        String source,
        boolean successful,
        int requestedCount,
        int upsertedCount,
        String details,
        Instant checkedAt
) {
}

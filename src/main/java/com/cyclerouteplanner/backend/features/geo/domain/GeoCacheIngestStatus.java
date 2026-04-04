package com.cyclerouteplanner.backend.features.geo.domain;

import java.time.Instant;

public record GeoCacheIngestStatus(
        boolean successful,
        String source,
        int requestedCount,
        int upsertedCount,
        String details,
        Instant checkedAt
) {
}

package com.cyclerouteplanner.backend.features.geo.domain;

import java.time.Instant;

public record GeoRoutingEdgeBuildStatus(
        boolean successful,
        String source,
        int osmUpsertedCount,
        int osmPlusTallinnUpsertedCount,
        int tallinnOnlyUpsertedCount,
        int totalUpsertedCount,
        String details,
        Instant checkedAt
) {
}

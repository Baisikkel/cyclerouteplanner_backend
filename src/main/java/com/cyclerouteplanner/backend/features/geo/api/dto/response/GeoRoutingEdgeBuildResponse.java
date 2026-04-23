package com.cyclerouteplanner.backend.features.geo.api.dto.response;

import java.time.Instant;

public record GeoRoutingEdgeBuildResponse(
        String source,
        boolean successful,
        int osmUpsertedCount,
        int osmPlusTallinnUpsertedCount,
        int tallinnOnlyUpsertedCount,
        int totalUpsertedCount,
        String details,
        Instant checkedAt
) {
}

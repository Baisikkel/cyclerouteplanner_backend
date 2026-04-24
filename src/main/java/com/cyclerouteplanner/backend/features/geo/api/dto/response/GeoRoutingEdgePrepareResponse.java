package com.cyclerouteplanner.backend.features.geo.api.dto.response;

import java.time.Instant;

public record GeoRoutingEdgePrepareResponse(
        String source,
        boolean successful,
        int osmUpsertedCount,
        int osmPlusTallinnUpsertedCount,
        int tallinnOnlyUpsertedCount,
        int totalUpsertedCount,
        int pseudoTagsExportedCount,
        String pseudoTagsOutputPath,
        String details,
        Instant checkedAt
) {
}

package com.cyclerouteplanner.backend.features.geo.api.dto.response;

import java.time.Instant;

public record GeoRoutingEdgeExportResponse(
        String source,
        boolean successful,
        int exportedCount,
        String outputPath,
        String details,
        Instant checkedAt
) {
}

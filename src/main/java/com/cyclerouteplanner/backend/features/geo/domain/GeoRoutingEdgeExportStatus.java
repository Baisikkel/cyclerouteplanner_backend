package com.cyclerouteplanner.backend.features.geo.domain;

import java.time.Instant;

public record GeoRoutingEdgeExportStatus(
        boolean successful,
        String source,
        int exportedCount,
        String outputPath,
        String details,
        Instant checkedAt
) {
}

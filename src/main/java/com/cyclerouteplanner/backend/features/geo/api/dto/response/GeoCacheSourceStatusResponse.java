package com.cyclerouteplanner.backend.features.geo.api.dto.response;

import java.time.Instant;

public record GeoCacheSourceStatusResponse(
        String source,
        long cachedRowCount,
        Instant lastSnapshotAt,
        Instant lastSourceTimestamp
) {
}

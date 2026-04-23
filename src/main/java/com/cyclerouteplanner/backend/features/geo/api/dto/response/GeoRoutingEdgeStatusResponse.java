package com.cyclerouteplanner.backend.features.geo.api.dto.response;

import java.time.Instant;

public record GeoRoutingEdgeStatusResponse(
        Instant checkedAt,
        long activeTotalCount,
        long activeOsmCount,
        long activeOsmPlusTallinnCount,
        long activeTallinnOnlyCount
) {
}

package com.cyclerouteplanner.backend.features.geo.domain;

import java.time.Instant;

public record GeoRoutingEdgeStatus(
        Instant checkedAt,
        long activeTotalCount,
        long activeOsmCount,
        long activeOsmPlusTallinnCount,
        long activeTallinnOnlyCount
) {
}

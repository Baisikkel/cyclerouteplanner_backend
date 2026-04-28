package com.cyclerouteplanner.backend.features.geo.api.dto.response;

import java.time.Instant;

public record GeoRoutingAuditResponse(
        Instant checkedAt,
        GeoRoutingAuditOsmResponse osm,
        GeoRoutingAuditTallinnResponse tallinn,
        GeoRoutingAuditReadinessResponse readiness
) {
}

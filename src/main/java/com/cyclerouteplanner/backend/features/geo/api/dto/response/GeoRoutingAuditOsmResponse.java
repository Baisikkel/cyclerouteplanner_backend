package com.cyclerouteplanner.backend.features.geo.api.dto.response;

public record GeoRoutingAuditOsmResponse(
        long totalRows,
        long lineRows,
        long bikeCandidateLineRows,
        long lineRowsWithBicycleTag,
        long lineRowsWithCyclewayTag,
        long lineRowsWithRouteRelationTag,
        long lineRowsWithSurfaceTag,
        long lineRowsWithTracktypeTag
) {
}

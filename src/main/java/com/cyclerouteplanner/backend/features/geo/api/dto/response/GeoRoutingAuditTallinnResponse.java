package com.cyclerouteplanner.backend.features.geo.api.dto.response;

public record GeoRoutingAuditTallinnResponse(
        long totalRows,
        long lineRows,
        long rowsWithProperties,
        long rowsWithName,
        long distinctSourceLayers,
        long lineRowsOverlappingOsm
) {
}

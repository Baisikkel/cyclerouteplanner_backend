package com.cyclerouteplanner.backend.features.geo.api.dto.response;

import java.util.List;

public record GeoRoutingAuditReadinessResponse(
        boolean readyForOsmOnlyGraphBuild,
        boolean readyForMergedOsmTallinnGraphBuild,
        double osmBikeCoverageRatio,
        double tallinnOverlapRatio,
        List<String> gaps
) {
}

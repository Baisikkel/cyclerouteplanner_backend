package com.cyclerouteplanner.backend.features.routing.api.dto.response;

public record RouteOptionResponse(
        long id,
        String sourceId,
        String name,
        String profileHint,
        double qualityScore,
        String enrichmentType,
        String wktGeometry
) {
}

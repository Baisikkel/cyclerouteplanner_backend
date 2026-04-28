package com.cyclerouteplanner.backend.features.routing.domain;

public record RouteOptionRecord(
        long id,
        String sourceId,
        String name,
        String profileHint,
        double qualityScore,
        String enrichmentType,
        String wktGeometry
) {
}

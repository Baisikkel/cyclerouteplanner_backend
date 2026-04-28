package com.cyclerouteplanner.backend.features.routing.application;

public record RouteCalculationResult(
        String geoJson,
        String resolvedProfile
) {
}

package com.cyclerouteplanner.backend.features.routing.api.dto.request;

import java.util.List;

public record RouteCalculationRequest(
        List<RouteWaypointRequest> waypoints,
        String profile
) {
}

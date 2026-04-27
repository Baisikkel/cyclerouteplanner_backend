package com.cyclerouteplanner.backend.features.routing.application;

import com.cyclerouteplanner.backend.features.routing.domain.RouteWaypoint;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"local", "docker"})
public class RouteCalculationService {

    private final BRouterService brouterService;
    private final RouteOptionService routeOptionService;

    public RouteCalculationService(BRouterService brouterService, RouteOptionService routeOptionService) {
        this.brouterService = brouterService;
        this.routeOptionService = routeOptionService;
    }

    public RouteCalculationResult calculate(List<RouteWaypoint> waypoints, String requestedProfile) {
        validateWaypoints(waypoints);
        RouteWaypoint start = waypoints.getFirst();
        RouteWaypoint end = waypoints.getLast();
        String resolvedProfile = routeOptionService.resolveRouteProfile(
                requestedProfile,
                start.latitude(),
                start.longitude(),
                end.latitude(),
                end.longitude()
        );
        String geoJson = brouterService.getRoute(waypoints, resolvedProfile);
        return new RouteCalculationResult(geoJson, resolvedProfile);
    }

    public RouteCalculationResult calculate(
            double startLat,
            double startLon,
            double endLat,
            double endLon,
            String requestedProfile
    ) {
        return calculate(
                List.of(
                        new RouteWaypoint(startLat, startLon),
                        new RouteWaypoint(endLat, endLon)
                ),
                requestedProfile
        );
    }

    private void validateWaypoints(List<RouteWaypoint> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            throw new IllegalArgumentException("at least 2 waypoints are required");
        }
        waypoints.forEach(this::validateWaypoint);
    }

    private void validateWaypoint(RouteWaypoint waypoint) {
        if (waypoint == null) {
            throw new IllegalArgumentException("waypoints must not contain null values");
        }
        if (!Double.isFinite(waypoint.latitude()) || waypoint.latitude() < -90 || waypoint.latitude() > 90) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (!Double.isFinite(waypoint.longitude()) || waypoint.longitude() < -180 || waypoint.longitude() > 180) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
    }
}

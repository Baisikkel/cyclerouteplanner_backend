package com.cyclerouteplanner.backend.features.routing.application;

import com.cyclerouteplanner.backend.features.routing.domain.RouteWaypoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RouteCalculationServiceTest {

    @Test
    void calculateValidatesResolvesProfileAndCallsBrouter() {
        BRouterService bRouterService = mock(BRouterService.class);
        RouteOptionService routeOptionService = mock(RouteOptionService.class);
        RouteCalculationService service = new RouteCalculationService(bRouterService, routeOptionService);
        List<RouteWaypoint> waypoints = List.of(
                new RouteWaypoint(59.43, 24.72),
                new RouteWaypoint(59.44, 24.73),
                new RouteWaypoint(59.45, 24.74)
        );
        when(routeOptionService.resolveRouteProfile("fastbike", 59.43, 24.72, 59.45, 24.74))
                .thenReturn("fastbike");
        when(bRouterService.getRoute(waypoints, "fastbike"))
                .thenReturn("{\"type\":\"FeatureCollection\",\"features\":[]}");

        RouteCalculationResult result = service.calculate(waypoints, "fastbike");

        assertEquals("fastbike", result.resolvedProfile());
        assertEquals("{\"type\":\"FeatureCollection\",\"features\":[]}", result.geoJson());
        verify(routeOptionService).resolveRouteProfile("fastbike", 59.43, 24.72, 59.45, 24.74);
        verify(bRouterService).getRoute(waypoints, "fastbike");
    }

    @Test
    void calculateRejectsTooFewWaypoints() {
        RouteCalculationService service = new RouteCalculationService(mock(BRouterService.class), mock(RouteOptionService.class));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.calculate(List.of(new RouteWaypoint(59.43, 24.72)), null));

        assertEquals("at least 2 waypoints are required", ex.getMessage());
    }

    @Test
    void calculateRejectsInvalidLatitude() {
        RouteCalculationService service = new RouteCalculationService(mock(BRouterService.class), mock(RouteOptionService.class));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.calculate(List.of(
                        new RouteWaypoint(91, 24.72),
                        new RouteWaypoint(59.44, 24.73)
                ), null));

        assertEquals("latitude must be between -90 and 90", ex.getMessage());
    }

    @Test
    void calculateRejectsInvalidLongitude() {
        RouteCalculationService service = new RouteCalculationService(mock(BRouterService.class), mock(RouteOptionService.class));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.calculate(List.of(
                        new RouteWaypoint(59.43, 181),
                        new RouteWaypoint(59.44, 24.73)
                ), null));

        assertEquals("longitude must be between -180 and 180", ex.getMessage());
    }
}

package com.cyclerouteplanner.backend.features.routing.api;

import com.cyclerouteplanner.backend.features.routing.application.RouteCalculationResult;
import com.cyclerouteplanner.backend.features.routing.application.RouteCalculationService;
import com.cyclerouteplanner.backend.features.routing.application.RouteOptionService;
import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionRecord;
import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionRefreshStatus;
import com.cyclerouteplanner.backend.features.routing.domain.RouteWaypoint;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RouteControllerTest {

    @Test
    void calculateRouteReturnsBrouterPayload() throws Exception {
        RouteCalculationService routeCalculationService = mock(RouteCalculationService.class);
        RouteOptionService routeOptionService = mock(RouteOptionService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new RouteController(routeCalculationService, routeOptionService)
        ).build();
        when(routeCalculationService.calculate(59.43, 24.72, 59.44, 24.73, null))
                .thenReturn(new RouteCalculationResult("{\"type\":\"FeatureCollection\",\"features\":[]}", "fastbike"));

        mockMvc.perform(get("/api/routes/calculate")
                        .param("startLat", "59.43")
                        .param("startLon", "24.72")
                        .param("endLat", "59.44")
                        .param("endLon", "24.73"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Route-Profile", "fastbike"))
                .andExpect(jsonPath("$.type").value("FeatureCollection"));
    }

    @Test
    void calculateRouteUsesExplicitProfileWhenProvided() throws Exception {
        RouteCalculationService routeCalculationService = mock(RouteCalculationService.class);
        RouteOptionService routeOptionService = mock(RouteOptionService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new RouteController(routeCalculationService, routeOptionService)
        ).build();
        when(routeCalculationService.calculate(59.43, 24.72, 59.44, 24.73, "gravel"))
                .thenReturn(new RouteCalculationResult("{\"type\":\"FeatureCollection\",\"features\":[]}", "gravel"));

        mockMvc.perform(get("/api/routes/calculate")
                        .param("startLat", "59.43")
                        .param("startLon", "24.72")
                        .param("endLat", "59.44")
                        .param("endLon", "24.73")
                        .param("profile", "gravel"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Route-Profile", "gravel"));

        verify(routeCalculationService).calculate(59.43, 24.72, 59.44, 24.73, "gravel");
    }

    @Test
    void calculateRoutePostSupportsMultipleWaypoints() throws Exception {
        RouteCalculationService routeCalculationService = mock(RouteCalculationService.class);
        RouteOptionService routeOptionService = mock(RouteOptionService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new RouteController(routeCalculationService, routeOptionService)
        ).build();
        List<RouteWaypoint> waypoints = List.of(
                new RouteWaypoint(59.43, 24.72),
                new RouteWaypoint(59.44, 24.73),
                new RouteWaypoint(59.45, 24.74)
        );
        when(routeCalculationService.calculate(waypoints, "fastbike"))
                .thenReturn(new RouteCalculationResult("{\"type\":\"FeatureCollection\",\"features\":[]}", "fastbike"));

        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "waypoints": [
                                    { "lat": 59.43, "lon": 24.72 },
                                    { "lat": 59.44, "lon": 24.73 },
                                    { "lat": 59.45, "lon": 24.74 }
                                  ],
                                  "profile": "fastbike"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Route-Profile", "fastbike"))
                .andExpect(jsonPath("$.type").value("FeatureCollection"));

        verify(routeCalculationService).calculate(waypoints, "fastbike");
    }

    @Test
    void calculateRoutePostRejectsTooFewWaypoints() throws Exception {
        RouteCalculationService routeCalculationService = mock(RouteCalculationService.class);
        RouteOptionService routeOptionService = mock(RouteOptionService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new RouteController(routeCalculationService, routeOptionService)
        ).build();
        when(routeCalculationService.calculate(List.of(new RouteWaypoint(59.43, 24.72)), null))
                .thenThrow(new IllegalArgumentException("at least 2 waypoints are required"));

        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "waypoints": [
                                    { "lat": 59.43, "lon": 24.72 }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculateRoutePostRejectsInvalidCoordinates() throws Exception {
        RouteCalculationService routeCalculationService = mock(RouteCalculationService.class);
        RouteOptionService routeOptionService = mock(RouteOptionService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new RouteController(routeCalculationService, routeOptionService)
        ).build();
        when(routeCalculationService.calculate(List.of(
                new RouteWaypoint(91, 24.72),
                new RouteWaypoint(59.44, 24.73)
        ), null)).thenThrow(new IllegalArgumentException("latitude must be between -90 and 90"));

        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "waypoints": [
                                    { "lat": 91, "lon": 24.72 },
                                    { "lat": 59.44, "lon": 24.73 }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshOptionsReturnsStatusPayload() throws Exception {
        RouteCalculationService routeCalculationService = mock(RouteCalculationService.class);
        RouteOptionService routeOptionService = mock(RouteOptionService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new RouteController(routeCalculationService, routeOptionService)
        ).build();
        when(routeOptionService.refreshFromGeoCaches()).thenReturn(new RouteOptionRefreshStatus(
                true,
                "osm_with_optional_tallinn_enrichment",
                15,
                "Route options refresh completed",
                Instant.parse("2026-04-22T12:00:00Z")
        ));

        mockMvc.perform(post("/api/routes/options/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("osm_with_optional_tallinn_enrichment"))
                .andExpect(jsonPath("$.upsertedCount").value(15));
    }

    @Test
    void listOptionsReturnsCurrentRouteOptions() throws Exception {
        RouteCalculationService routeCalculationService = mock(RouteCalculationService.class);
        RouteOptionService routeOptionService = mock(RouteOptionService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new RouteController(routeCalculationService, routeOptionService)
        ).build();
        when(routeOptionService.activeOptions(2)).thenReturn(List.of(
                new RouteOptionRecord(
                        7L,
                        "way/1001",
                        "Bike Street",
                        "fastbike",
                        0.85,
                        "osm_plus_tallinn",
                        "LINESTRING(24.72 59.43,24.73 59.44)"
                )
        ));

        mockMvc.perform(get("/api/routes/options").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceId").value("way/1001"))
                .andExpect(jsonPath("$[0].profileHint").value("fastbike"))
                .andExpect(jsonPath("$[0].enrichmentType").value("osm_plus_tallinn"));
    }
}
